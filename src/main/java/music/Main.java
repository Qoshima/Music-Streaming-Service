
package music;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import music.player.AudioEngine;

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
                        case "fade" -> {
                            if (parts.length > 1 &&
                                    (parts[1].equalsIgnoreCase("on") ||
                                            parts[1].equalsIgnoreCase("off"))) {
                                engine.setFade(parts[1].equalsIgnoreCase("on"));
                            } else {
                                System.out.println("Use: fade on/off");
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
