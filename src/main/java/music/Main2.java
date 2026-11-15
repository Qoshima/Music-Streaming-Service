package music;

import music.data.MusicDataLoader;
import music.facade.MusicLibraryFacade;
import music.model.MusicCatalog;
import music.observer.LyricsDisplay;
import music.strategy.SequentialStrategy;
import music.strategy.ShuffleStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main2 {

    public static void main(String[] args) {

        MusicCatalog catalog = new MusicCatalog();
        MusicDataLoader.loadInitialData(catalog);

        MusicLibraryFacade facade =
                new MusicLibraryFacade(catalog);

        // Register Concrete Observer.
        facade.registerObserver(new LyricsDisplay());

        System.out.println("=== Music Streaming Service ===");
        System.out.println("Available: Test1, Test2, Test3, Test4");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("play <title>");
        System.out.println("playlist <name> <title1, title2>");
        System.out.println("playlist-play <name>");
        System.out.println("next");
        System.out.println("strategy sequential");
        System.out.println("strategy shuffle");
        System.out.println("pause | resume | stop");
        System.out.println("echo on/off | fade on/off");
        System.out.println("exit");

        try (Scanner scanner = new Scanner(System.in)) {

            while (true) {
                System.out.print("> ");

                if (!scanner.hasNextLine()) {
                    break;
                }

                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                String[] parts = input.split("\\s+", 2);
                String command = parts[0].toLowerCase();

                String argument = parts.length > 1
                        ? parts[1].trim()
                        : "";

                try {
                    switch (command) {

                        case "play" -> {
                            if (argument.isEmpty()) {
                                System.out.println(
                                        "Usage: play <title>"
                                );
                            } else {
                                facade.playTrack(argument);
                            }
                        }

                        case "playlist" -> {
                            String[] playlistArgs =
                                    argument.split("\\s+", 2);

                            if (playlistArgs.length < 2) {
                                System.out.println(
                                        "Usage: playlist <name> " +
                                                "<title1, title2>"
                                );
                            } else {
                                String name = playlistArgs[0];
                                List<String> titles = new ArrayList<>();

                                for (String title :
                                        playlistArgs[1].split(",")) {

                                    if (!title.trim().isEmpty()) {
                                        titles.add(title.trim());
                                    }
                                }

                                facade.buildAndSavePlaylist(
                                        name, titles
                                );
                            }
                        }

                        case "playlist-play" -> {
                            if (argument.isEmpty()) {
                                System.out.println(
                                        "Usage: playlist-play <name>"
                                );
                            } else {
                                facade.playPlaylist(argument);
                            }
                        }

                        case "next" ->
                                facade.playNextInPlaylist();

                        case "strategy" -> {
                            switch (argument.toLowerCase()) {

                                case "sequential" ->
                                        facade.setPlaybackStrategy(
                                                new SequentialStrategy()
                                        );

                                case "shuffle" ->
                                        facade.setPlaybackStrategy(
                                                new ShuffleStrategy()
                                        );

                                default -> System.out.println(
                                        "Use: strategy sequential/shuffle"
                                );
                            }
                        }

                        case "pause" -> facade.pause();

                        case "resume" -> facade.resume();

                        case "stop" -> facade.stop();

                        case "echo" -> {
                            if (argument.equalsIgnoreCase("on") ||
                                    argument.equalsIgnoreCase("off")) {

                                facade.setEcho(
                                        argument.equalsIgnoreCase("on")
                                );
                            } else {
                                System.out.println("Use: echo on/off");
                            }
                        }

                        case "fade" -> {
                            if (argument.equalsIgnoreCase("on") ||
                                    argument.equalsIgnoreCase("off")) {

                                facade.setFade(
                                        argument.equalsIgnoreCase("on")
                                );
                            } else {
                                System.out.println("Use: fade on/off");
                            }
                        }

                        case "exit" -> {
                            return;
                        }

                        default -> System.out.println(
                                "Unknown command: " + command
                        );
                    }

                } catch (Exception e) {
                    System.err.println(
                            "Error: " + e.getMessage()
                    );
                }
            }

        } finally {
            facade.close();
        }
    }
}
