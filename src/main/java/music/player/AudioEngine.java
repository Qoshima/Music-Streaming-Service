package music.player;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioPlayer;

import music.adapter.AudioSource;
import music.adapter.FfmpegAudioAdapter;
import music.decorator.BaseAudioProcessor;
import music.decorator.EchoDecorator;
import music.decorator.FadeInDecorator;

import java.nio.file.Path;

public class AudioEngine {

    private final AudioSource source = new FfmpegAudioAdapter();
    private final Object pauseLock = new Object();

    private volatile AudioDispatcher dispatcher;
    private volatile Thread worker;

    private volatile EchoDecorator echo;
    private volatile FadeInDecorator fade;

    private volatile boolean echoEnabled;
    private volatile boolean fadeEnabled;
    private volatile boolean paused;
    private volatile boolean stopping;

    public synchronized void play(Path file) throws Exception {
        stop();

        AudioDispatcher newDispatcher = source.open(file);

        // Create a fresh Decorator chain for every track.
        EchoDecorator newEcho =
                new EchoDecorator(new BaseAudioProcessor());

        FadeInDecorator newFade =
                new FadeInDecorator(newEcho);

        newEcho.setEnabled(echoEnabled);
        newFade.setEnabled(fadeEnabled);

        stopping = false;
        paused = false;

        try {
            // Pause without terminating the audio stream.
            newDispatcher.addAudioProcessor(new AudioProcessor() {
                @Override
                public boolean process(AudioEvent event) {
                    synchronized (pauseLock) {
                        while (paused && !stopping) {
                            try {
                                pauseLock.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return false;
                            }
                        }
                    }

                    return !stopping;
                }

                @Override
                public void processingFinished() {}
            });

            // Apply both effects through one Decorator chain.
            newDispatcher.addAudioProcessor(newFade);

            // Send processed audio to the speakers.
            newDispatcher.addAudioProcessor(
                    new AudioPlayer(newDispatcher.getFormat())
            );
        } catch (Exception e) {
            newDispatcher.stop();
            throw e;
        }

        echo = newEcho;
        fade = newFade;
        dispatcher = newDispatcher;

        Thread newWorker = new Thread(() -> {
            try {
                newDispatcher.run();
            } catch (Exception e) {
                System.err.println(
                        "Playback error: " + e.getMessage()
                );
            } finally {
                if (dispatcher == newDispatcher) {
                    dispatcher = null;
                    worker = null;
                    echo = null;
                    fade = null;
                }
            }
        }, "Audio-Playback");

        worker = newWorker;
        newWorker.start();

        System.out.println("Playing: " + file.getFileName());
    }

    public void pause() {
        if (dispatcher != null) {
            paused = true;
            System.out.println("Paused");
        }
    }

    public void resume() {
        paused = false;

        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        System.out.println("Resumed");
    }

    public void setEcho(boolean enabled) {
        echoEnabled = enabled;

        EchoDecorator current = echo;
        if (current != null) {
            current.setEnabled(enabled);
        }

        System.out.println(
                "Echo: " + (enabled ? "ON" : "OFF")
        );
    }

    public void setFade(boolean enabled) {
        fadeEnabled = enabled;

        FadeInDecorator current = fade;
        if (current != null) {
            current.setEnabled(enabled);
        }

        System.out.println(
                "Fade In: " + (enabled ? "ON" : "OFF")
        );
    }

    public synchronized void stop() {
        stopping = true;
        paused = false;

        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        AudioDispatcher oldDispatcher = dispatcher;
        Thread oldWorker = worker;

        if (oldDispatcher != null) {
            oldDispatcher.stop();
        }

        if (oldWorker != null &&
                oldWorker != Thread.currentThread()) {
            try {
                oldWorker.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        dispatcher = null;
        worker = null;
        echo = null;
        fade = null;
    }
}