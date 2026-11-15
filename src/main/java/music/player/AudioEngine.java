
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
import music.decorator.VolumeDecorator;

import java.nio.file.Path;
import java.util.function.LongConsumer;

public class AudioEngine {

    private final AudioSource source = new FfmpegAudioAdapter();
    private final Object pauseLock = new Object();

    private volatile AudioDispatcher dispatcher;
    private volatile Thread worker;

    private volatile EchoDecorator echo;
    private volatile FadeInDecorator fade;
    private volatile VolumeDecorator volume;

    private volatile LongConsumer onTrackFinished;
    private volatile long playbackId;

    private volatile boolean echoEnabled;
    private volatile boolean fadeEnabled;
    private volatile boolean paused;
    private volatile boolean stopping;

    private volatile int volumePercent = 100;

    public synchronized void play(Path file) throws Exception {
        stop();

        AudioDispatcher newDispatcher = source.open(file);

        // Create a fresh Decorator chain.
        EchoDecorator newEcho =
                new EchoDecorator(new BaseAudioProcessor());

        FadeInDecorator newFade =
                new FadeInDecorator(newEcho);

        VolumeDecorator newVolume =
                new VolumeDecorator(newFade);

        newEcho.setEnabled(echoEnabled);
        newFade.setEnabled(fadeEnabled);
        newVolume.setVolume(volumePercent);

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
                public void processingFinished() {
                }
            });

            // Process Echo, Fade In and Volume.
            newDispatcher.addAudioProcessor(newVolume);

            // Send processed audio to speakers.
            newDispatcher.addAudioProcessor(
                    new AudioPlayer(newDispatcher.getFormat())
            );

        } catch (Exception e) {
            newDispatcher.stop();
            throw e;
        }

        echo = newEcho;
        fade = newFade;
        volume = newVolume;
        dispatcher = newDispatcher;

        final long startedPlaybackId = playbackId;


        Thread newWorker = new Thread(() -> {

            boolean finishedNormally = false;

            try {
                newDispatcher.run();
                finishedNormally = true;

            } catch (Exception e) {
                System.err.println(
                        "[AudioEngine] Playback error: " + e
                );
                e.printStackTrace(System.err);
            } finally {
                // Only notify if the track finished naturally.
                boolean shouldNotify =
                        finishedNormally &&
                                !stopping &&
                                dispatcher == newDispatcher;

                if (dispatcher == newDispatcher) {
                    dispatcher = null;
                    worker = null;
                    echo = null;
                    fade = null;
                    volume = null;
                }

                if (shouldNotify) {
                    LongConsumer listener = onTrackFinished;

                    if (listener != null) {
                        // Separate thread avoids blocking the audio worker.
                        Thread notificationThread = new Thread(
                                () -> listener.accept(startedPlaybackId),
                                "Track-Finished"
                        );

                        notificationThread.setDaemon(true);
                        notificationThread.start();
                    }
                }
            }

        }, "Audio-Playback");

        worker = newWorker;
        newWorker.start();

        System.out.println("Playing: " + file.getFileName());
    }

    // ===========================
    // Playback controls
    // ===========================

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

    public synchronized void stop() {
        playbackId++;
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
        volume = null;
    }

    // ===========================
    // Audio effects
    // ===========================

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

    // ===========================
    // Volume control
    // ===========================

    public void setVolume(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException(
                    "Volume must be between 0 and 100"
            );
        }

        volumePercent = percent;

        VolumeDecorator current = volume;

        if (current != null) {
            current.setVolume(percent);
        }

        System.out.println("Volume: " + percent + "%");
    }

    public int getVolume() {
        return volumePercent;
    }

    // ===========================
    // Playback information
    // ===========================

    public long getPositionMillis() {
        AudioDispatcher current = dispatcher;

        if (current == null) {
            return 0;
        }

        return Math.max(
                0,
                Math.round(current.secondsProcessed() * 1000.0)
        );
    }

    public boolean isActive() {
        return dispatcher != null && !stopping;
    }

    public void setOnTrackFinished(LongConsumer listener) {
        this.onTrackFinished = listener;
    }

    public boolean isCurrentPlayback(long id) {
        return playbackId == id && !stopping;
    }

}
