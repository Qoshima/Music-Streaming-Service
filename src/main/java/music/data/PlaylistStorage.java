package music.data;

import music.builder.Playlist;
import music.builder.PlaylistBuilder;
import music.model.MusicCatalog;
import music.model.Track;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PlaylistStorage {

    private static final Path FILE =
            Path.of("playlists.properties");

    public static void save(Playlist playlist) throws IOException {
        Properties properties = new Properties();

        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(
                    FILE, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }

        // Replace an existing playlist with the same name.
        for (String name : new ArrayList<>(
                properties.stringPropertyNames())) {
            if (name.equalsIgnoreCase(playlist.getName())) {
                properties.remove(name);
            }
        }

        List<String> titles = new ArrayList<>();

        for (Track track : playlist.getTracks()) {
            titles.add(track.getTitle());
        }

        properties.setProperty(
                playlist.getName(),
                String.join("\n", titles)
        );

        try (Writer writer = Files.newBufferedWriter(
                FILE, StandardCharsets.UTF_8)) {
            properties.store(writer, "Saved playlists");
        }
    }

    public static void load(MusicCatalog catalog) {
        if (!Files.exists(FILE)) {
            return;
        }

        Properties properties = new Properties();

        try (Reader reader = Files.newBufferedReader(
                FILE, StandardCharsets.UTF_8)) {
            properties.load(reader);

        } catch (IOException e) {
            System.out.println(
                    "Cannot load playlists: " + e.getMessage()
            );
            return;
        }

        for (String name : properties.stringPropertyNames()) {
            PlaylistBuilder builder =
                    new PlaylistBuilder().setName(name);

            String savedTitles = properties.getProperty(name);

            for (String title : savedTitles.split("\n")) {
                Track track = catalog.findTrackByTitle(title);

                if (track != null) {
                    builder.addTrack(track);
                }
            }

            try {
                Playlist playlist = builder.build();
                catalog.savePlaylist(playlist);

                System.out.println("Loaded playlist: " + name);

            } catch (IllegalStateException e) {
                System.out.println(
                        "Cannot restore playlist: " + name
                );
            }
        }
    }
}
