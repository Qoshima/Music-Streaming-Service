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


    public void setFade(boolean enabled) {
        fade.setEnabled(enabled);
        System.out.println("Fade In: " +
                (enabled ? "ON" : "OFF"));
    }

    private final AudioSource source =
            new FfmpegAudioAdapter();

    private final EchoDecorator echo =
            new EchoDecorator(new BaseAudioProcessor());

    private final FadeInDecorator fade =
            new FadeInDecorator(echo);

    private final Object pauseLock = new Object();

    private volatile AudioDispatcher dispatcher;
    private volatile Thread worker;
    private volatile boolean paused;
    private volatile boolean stopping;

    public void play(Path file) throws Exception {
        stop();

        AudioDispatcher newDispatcher = source.open(file);
        echo.reset();
        fade.reset();

        stopping = false;
        paused = false;

        // Pause processing before sending samples to speakers.
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
            public void processingFinished() {
            }
        });

        newDispatcher.addAudioProcessor(fade);
        newDispatcher.addAudioProcessor(
                new AudioPlayer(newDispatcher.getFormat())
        );

        dispatcher = newDispatcher;

        Thread newWorker = new Thread(() -> {
            try {
                newDispatcher.run();
            } catch (Throwable e) {
                System.err.println(
                        "Playback error: " + e.getMessage());
            } finally {
                if (dispatcher == newDispatcher) {
                    dispatcher = null;
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
        echo.setEnabled(enabled);
        System.out.println("Echo: " +
                (enabled ? "ON" : "OFF"));
    }

    public void stop() {
        AudioDispatcher oldDispatcher = dispatcher;
        Thread oldWorker = worker;

        stopping = true;
        paused = false;

        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        if (oldDispatcher != null) {
            oldDispatcher.stop();
        }

        if (oldWorker != null &&
                oldWorker != Thread.currentThread()) {
            try {
                oldWorker.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        dispatcher = null;
        worker = null;
    }
}
