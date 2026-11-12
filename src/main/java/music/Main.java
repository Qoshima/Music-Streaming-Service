
package music;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static void main(String[] args) {
        AudioEngine engine = new AudioEngine();

        System.out.println("=== Music Streaming Service ===");
        System.out.println("Commands:");
        System.out.println("play music/test.mp3");
        System.out.println("pause | resume | stop");
        System.out.println("echo on | echo off");
        System.out.println("exit");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) break;

                String input = scanner.nextLine().trim();
                String[] parts = input.split("\\s+", 2);

                try {
                    switch (parts[0].toLowerCase()) {
                        case "play" -> {
                            String file = parts.length > 1
                                    ? parts[1] : "music/test.mp3";
                            engine.play(Path.of(file));
                        }
                        case "pause" -> engine.pause();
                        case "resume" -> engine.resume();
                        case "stop" -> engine.stop();
                        case "echo" -> {
                            if (parts.length > 1 &&
                                    (parts[1].equalsIgnoreCase("on") ||
                                            parts[1].equalsIgnoreCase("off"))) {
                                engine.setEcho(
                                        parts[1].equalsIgnoreCase("on"));
                            } else {
                                System.out.println("Use: echo on/off");
                            }
                        }
                        case "exit" -> {
                            engine.stop();
                            return;
                        }
                        default -> System.out.println("Unknown command");
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        } finally {
            engine.stop();
        }
    }
}

// Unified interface expected by our application.
interface AudioSource {
    AudioDispatcher open(Path file);
}

// Converts our interface to TarsosDSP/FFmpeg API.
class FfmpegAudioAdapter implements AudioSource {

    @Override
    public AudioDispatcher open(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(
                    "File not found: " + file);
        }

        return AudioDispatcherFactory.fromPipe(
                file.toAbsolutePath().toString(),
                44100,
                2048,
                0
        );
    }
}

// Effect that can be enabled while audio is playing.
class EchoProcessor implements AudioProcessor {

    private final AtomicBoolean enabled =
            new AtomicBoolean(false);

    private final float[] delayBuffer = new float[22050];
    private int position = 0;

    public void setEnabled(boolean value) {
        enabled.set(value);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void reset() {
        Arrays.fill(delayBuffer, 0);
        position = 0;
    }

    @Override
    public boolean process(AudioEvent event) {
        float[] samples = event.getFloatBuffer();

        for (int i = 0; i < samples.length; i++) {
            float input = samples[i];
            float delayed = delayBuffer[position];

            if (enabled.get()) {
                samples[i] = Math.max(-1, Math.min(
                        1, input + delayed * 0.35f));

                delayBuffer[position] = Math.max(-1,
                        Math.min(1, input + delayed * 0.4f));
            } else {
                delayBuffer[position] = 0;
            }

            position = (position + 1) % delayBuffer.length;
        }

        return true;
    }

    @Override
    public void processingFinished() {}
}

class AudioEngine {

    private final AudioSource source =
            new FfmpegAudioAdapter();

    private final EchoProcessor echo =
            new EchoProcessor();

    private final Object pauseLock = new Object();

    private volatile AudioDispatcher dispatcher;
    private volatile Thread worker;
    private volatile boolean paused;
    private volatile boolean stopping;

    public void play(Path file) throws Exception {
        stop();

        AudioDispatcher newDispatcher = source.open(file);
        echo.reset();

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
            public void processingFinished() {}
        });

        newDispatcher.addAudioProcessor(echo);
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