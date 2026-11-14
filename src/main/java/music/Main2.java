package music;

import music.facade.MusicLibraryFacade;
import music.model.MusicCatalog;
import music.model.Track;
import music.observer.Observer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main2 {
    public static void main(String[] args) {
        MusicCatalog catalog = new MusicCatalog();
        MusicLibraryFacade facade = new MusicLibraryFacade(catalog);

        // Setup mock observer 
        facade.registerObserver(track -> System.out.println("[LYRICS]: " + track.getLyrics()));
        // later will be changed to:
        // facade.registerObserver(new LyricsDisplay());

        catalog.addTrack(new Track("Test1", "Artist 1", Path.of("music/test.mp3"), "La la la..."));
        catalog.addTrack(new Track("Test2", "Artist 2", Path.of("music/test.flac"), "Na na na..."));
        catalog.addTrack(new Track("Test3", "Artist 3", Path.of("music/test.m4a"), "Do do do..."));
        catalog.addTrack(new Track("Test4", "Artist 4", Path.of("music/test.webm"), "Beep boop..."));

        System.out.println("=== Music Streaming Service ===");
        System.out.println("Commands:");
        System.out.println("play <title>");
        System.out.println("playlist <name> <title1, title2>");
        System.out.println("playlist-play <name>");
        System.out.println("next");
        System.out.println("pause | resume | stop | echo on/off | fade on/off");
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
                            if (parts.length > 1) facade.playTrack(parts[1]);
                        }
                        case "playlist" -> {
                            if (parts.length > 1) {
                                String[] playlistArgs = parts[1].split("\\s+", 2);
                                String name = playlistArgs[0];
                                
                                List<String> titles = new ArrayList<>();
                                String[] rawTitles = playlistArgs[1].split(",");
                                for (String rawTitle : rawTitles) {
                                    titles.add(rawTitle.trim()); 
                                }
                                
                                facade.buildAndSavePlaylist(name, titles);
                            }
                        }
                        case "playlist-play" -> {
                            if (parts.length > 1) facade.playPlaylist(parts[1]);
                        }
                        case "next" -> facade.playNextInPlaylist();
                        case "pause" -> facade.pause();
                        case "resume" -> facade.resume();
                        case "stop" -> facade.stop();
                        case "echo" -> facade.setEcho(parts.length > 1 && parts[1].equalsIgnoreCase("on"));
                        case "fade" -> facade.setFade(parts.length > 1 && parts[1].equalsIgnoreCase("on"));
                        case "exit" -> {
                            facade.stop();
                            return;
                        }
                        default -> System.out.println("Unknown command");
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        } finally {
            facade.stop();
        }
    }
}