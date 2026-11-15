package music.data;

import music.model.MusicCatalog;
import music.model.Track;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.io.IOException;
import java.util.Locale;

public class MusicDataLoader {
    public static void loadInitialData(MusicCatalog catalog) {
        catalog.addTrack(new Track("Test1", "Artist 1", Path.of("music/test.mp3"), "La la la..."));
        catalog.addTrack(new Track("Test2", "Artist 2", Path.of("music/test.flac"), "Na na na..."));
        catalog.addTrack(new Track("Test3", "Artist 3", Path.of("music/test.m4a"), "Do do do..."));
        catalog.addTrack(new Track("Test4", "Artist 4", Path.of("music/test.webm"), "Beep boop..."));

    }

    public static void loadDownloadedTracks(MusicCatalog catalog) {
        Path directory = Path.of("music", "downloads");

        if (!Files.isDirectory(directory)) {
            return;
        }

        try (DirectoryStream<Path> files =
                     Files.newDirectoryStream(directory)) {

            for (Path file : files) {
                String filename = file.getFileName().toString();

                if (!Files.isRegularFile(file) ||
                        !filename.toLowerCase(Locale.ROOT)
                                .endsWith(".mp3")) {
                    continue;
                }

                String title = filename.substring(
                        0, filename.length() - 4
                );

                if (catalog.findTrackByTitle(title) == null) {
                    catalog.addTrack(
                            new Track(title, "YouTube", file, "")
                    );
                }
            }

        } catch (IOException e) {
            System.err.println(
                    "Cannot load downloaded tracks: " + e.getMessage()
            );
        }
    }

}