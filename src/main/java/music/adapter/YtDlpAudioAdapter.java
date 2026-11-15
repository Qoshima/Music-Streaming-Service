
package music.adapter;

import music.model.Track;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public class YtDlpAudioAdapter implements TrackImporter {

    private final YtDlpClient client = new YtDlpClient();

    @Override
    public Track importTrack(String url)
            throws IOException, InterruptedException {

        validateUrl(url);

        Path audio = client.download(
                url, Path.of("music", "downloads")
        );

        String filename = audio.getFileName().toString();
        int dot = filename.lastIndexOf('.');

        String title = dot >= 0
                ? filename.substring(0, dot)
                : filename;

        // LyricsDisplay expects: <audio-base-name>.lrc
        Path lyrics = audio.resolveSibling(title + ".lrc");

        if (!Files.isRegularFile(lyrics)) {

            try (Stream<Path> files = Files.list(audio.getParent())) {

                Optional<Path> candidate = files
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = path.getFileName().toString();

                            return name.startsWith(title + ".") &&
                                    name.endsWith(".lrc");
                        })
                        .sorted(Comparator.comparing(
                                path -> !path.getFileName().toString()
                                        .endsWith("-orig.lrc")
                        ))
                        .findFirst();

                if (candidate.isPresent()) {
                    Files.copy(
                            candidate.get(),
                            lyrics,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }
        }

        System.out.println("Audio saved: " + audio);

        if (Files.isRegularFile(lyrics)) {
            System.out.println("Lyrics saved: " + lyrics);
        } else {
            System.out.println(
                    "Lyrics unavailable. Audio saved successfully."
            );
        }

        return new Track(title, "YouTube", audio, "");
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "YouTube URL is required."
            );
        }

        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid YouTube URL."
            );
        }

        String host = uri.getHost();

        if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                host == null) {
            throw new IllegalArgumentException(
                    "Use an HTTPS YouTube link."
            );
        }

        host = host.toLowerCase(Locale.ROOT);

        if (!(host.equals("youtube.com") ||
                host.equals("www.youtube.com") ||
                host.equals("m.youtube.com") ||
                host.equals("music.youtube.com") ||
                host.equals("youtu.be"))) {

            throw new IllegalArgumentException(
                    "Only YouTube links are supported."
            );
        }
    }
}
