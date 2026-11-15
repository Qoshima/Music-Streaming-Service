
package music;

import music.builder.Playlist;
import music.data.MusicDataLoader;
import music.data.PlaylistStorage;
import music.facade.MusicLibraryFacade;
import music.model.MusicCatalog;
import music.model.Track;
import music.observer.LyricsDisplay;
import music.strategy.SequentialStrategy;
import music.strategy.ShuffleStrategy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // Initialize music library.
        MusicCatalog catalog = new MusicCatalog();
        MusicDataLoader.loadInitialData(catalog);
        PlaylistStorage.load(catalog);

        // Initialize Facade and Observer.
        MusicLibraryFacade player =
                new MusicLibraryFacade(catalog);

        player.registerObserver(new LyricsDisplay());

        printWelcome();
        printHelp();

        try (Scanner scanner = new Scanner(System.in)) {

            while (true) {
                System.out.print("\nMusic > ");

                if (!scanner.hasNextLine()) {
                    break;
                }

                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                String[] parts = input.split("\\s+", 2);

                String command =
                        parts[0].toLowerCase(Locale.ROOT);

                String argument = parts.length > 1
                        ? parts[1].trim()
                        : "";

                try {
                    switch (command) {

                        // General
                        case "help", "menu" -> printHelp();

                        case "tracks", "library" ->
                                showTracks(catalog);

                        case "playlists" ->
                                showPlaylists(catalog);

                        // Playback
                        case "play" ->
                                playMusic(player, argument);

                        case "pause" -> player.pause();

                        case "resume" -> player.resume();

                        case "stop" -> player.stop();

                        case "next" ->
                                player.playNextInPlaylist();

                        // Playlists
                        case "playlist" ->
                                createPlaylist(player, argument);

                        case "playlist-play" -> {
                            if (argument.isEmpty()) {
                                System.out.println(
                                        "Usage: playlist-play <name>"
                                );
                            } else {
                                player.playPlaylist(argument);
                            }
                        }

                        // Strategy Pattern
                        case "strategy" -> {
                            switch (argument.toLowerCase(Locale.ROOT)) {

                                case "sequential" ->
                                        player.setPlaybackStrategy(
                                                new SequentialStrategy()
                                        );

                                case "shuffle" ->
                                        player.setPlaybackStrategy(
                                                new ShuffleStrategy()
                                        );

                                default -> System.out.println(
                                        "Usage: strategy sequential/shuffle"
                                );
                            }
                        }

                        // Audio effects
                        case "echo" ->
                                changeEffect(player, "echo", argument);

                        case "fade" ->
                                changeEffect(player, "fade", argument);

                        // Exit
                        case "exit", "quit" -> {
                            System.out.println("Goodbye!");
                            return;
                        }

                        default -> System.out.println(
                                "Unknown command. Type 'help'."
                        );
                    }

                } catch (Exception e) {
                    System.err.println(
                            "Error: " + e.getMessage()
                    );
                }
            }

        } finally {
            player.close();
        }
    }

    // =========================================
    // Console appearance
    // =========================================

    private static void printWelcome() {
        System.out.println();
        System.out.println("================================================");
        System.out.println("             MUSIC STREAMING SERVICE            ");
        System.out.println("                 Java 17 Player                 ");
        System.out.println("================================================");
        System.out.println("      Music Library | Playlists | Effects       ");
        System.out.println("================================================");
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("--------------- MUSIC LIBRARY ----------------");
        System.out.println(" tracks                   Show available songs");
        System.out.println(" play <title>             Play a library song");
        System.out.println(" play <file path>         Play a local audio file");

        System.out.println();
        System.out.println("---------------- PLAYBACK --------------------");
        System.out.println(" pause                    Pause playback");
        System.out.println(" resume                   Resume playback");
        System.out.println(" stop                     Stop playback");
        System.out.println(" next                     Next playlist track");

        System.out.println();
        System.out.println("---------------- PLAYLISTS -------------------");
        System.out.println(" playlists                Show saved playlists");
        System.out.println(" playlist <name> <songs>  Create a playlist");
        System.out.println(" playlist-play <name>     Play a playlist");

        System.out.println();
        System.out.println("--------------- PLAYBACK MODE ----------------");
        System.out.println(" strategy sequential      Play in order");
        System.out.println(" strategy shuffle         Play randomly");

        System.out.println();
        System.out.println("----------------- EFFECTS --------------------");
        System.out.println(" echo on/off              Toggle Echo");
        System.out.println(" fade on/off              Toggle Fade In");

        System.out.println();
        System.out.println("------------------ SYSTEM --------------------");
        System.out.println(" help                     Show commands");
        System.out.println(" exit                     Close the player");
        System.out.println("----------------------------------------------");
    }

    // =========================================
    // Music Library
    // =========================================

    private static void showTracks(MusicCatalog catalog) {
        System.out.println();
        System.out.println("AVAILABLE TRACKS");
        System.out.println("----------------------------------------------");

        List<Track> tracks = catalog.getTracks();

        if (tracks.isEmpty()) {
            System.out.println("No tracks available.");
            return;
        }

        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);

            System.out.println(
                    (i + 1) + ". " +
                            track.getTitle() + " - " +
                            track.getArtist()
            );
        }
    }

    private static void playMusic(
            MusicLibraryFacade player, String argument) {

        if (argument.isEmpty()) {
            System.out.println("Usage: play <title or file path>");
            return;
        }

        // Support paths with spaces enclosed in quotes.
        String value = argument;

        if (value.length() >= 2 &&
                value.startsWith("\"") &&
                value.endsWith("\"")) {

            value = value.substring(1, value.length() - 1);
        }

        String lower = value.toLowerCase(Locale.ROOT);

        boolean isFile =
                value.contains("/") ||
                        value.contains("\\") ||
                        lower.endsWith(".mp3") ||
                        lower.endsWith(".flac") ||
                        lower.endsWith(".m4a") ||
                        lower.endsWith(".webm") ||
                        lower.endsWith(".wav") ||
                        lower.endsWith(".ogg");

        if (isFile) {
            player.playLocalFile(Path.of(value));
        } else {
            player.playTrack(value);
        }
    }

    // =========================================
    // Playlists
    // =========================================

    private static void showPlaylists(MusicCatalog catalog) {
        System.out.println();
        System.out.println("SAVED PLAYLISTS");
        System.out.println("----------------------------------------------");

        List<Playlist> playlists = catalog.getPlaylists();

        if (playlists.isEmpty()) {
            System.out.println("No playlists available.");
            return;
        }

        for (Playlist playlist : playlists) {
            System.out.println(
                    playlist.getName() + " (" +
                            playlist.getTracks().size() + " tracks)"
            );

            for (Track track : playlist.getTracks()) {
                System.out.println("  - " + track.getTitle());
            }
        }
    }

    private static void createPlaylist(
            MusicLibraryFacade player, String argument) {

        String[] parts = argument.split("\\s+", 2);

        if (parts.length < 2) {
            System.out.println(
                    "Usage: playlist <name> <title1, title2>"
            );
            return;
        }

        String name = parts[0];
        List<String> titles = new ArrayList<>();

        for (String title : parts[1].split(",")) {
            if (!title.trim().isEmpty()) {
                titles.add(title.trim());
            }
        }

        player.buildAndSavePlaylist(name, titles);
    }

    // =========================================
    // Audio effects
    // =========================================

    private static void changeEffect(
            MusicLibraryFacade player,
            String effect,
            String argument) {

        if (!argument.equalsIgnoreCase("on") &&
                !argument.equalsIgnoreCase("off")) {

            System.out.println(
                    "Usage: " + effect + " on/off"
            );
            return;
        }

        boolean enabled = argument.equalsIgnoreCase("on");

        if (effect.equals("echo")) {
            player.setEcho(enabled);
        } else {
            player.setFade(enabled);
        }
    }
}