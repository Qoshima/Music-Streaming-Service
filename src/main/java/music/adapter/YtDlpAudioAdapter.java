
package music.adapter;

import music.model.Track;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class YtDlpAudioAdapter implements TrackImporter {

    private final YtDlpClient client = new YtDlpClient();

    @Override
    public Track importTrack(String url)
            throws IOException, InterruptedException {

        validateUrl(url);

        Path folder = Path.of("music", "downloads");
        Path audioFile = client.download(url, folder);

        String filename = audioFile.getFileName().toString();
        int dot = filename.lastIndexOf('.');

        String title = dot >= 0
                ? filename.substring(0, dot)
                : filename;

        // Existing LyricsDisplay expects the same base filename.
        Path lyricsFile = audioFile.resolveSibling(
                title + ".lrc"
        );

        boolean foundLyrics = false;

        for (String language : new String[]{"ru", "en", "kk"}) {
            Path subtitles = audioFile.resolveSibling(
                    title + "." + language + ".lrc"
            );

            if (Files.isRegularFile(subtitles)) {
                Files.copy(
                        subtitles,
                        lyricsFile,
                        StandardCopyOption.REPLACE_EXISTING
                );

                foundLyrics = true;
                break;
            }
        }

        System.out.println("Audio: " + audioFile);

        if (foundLyrics || Files.isRegularFile(lyricsFile)) {
            System.out.println("Lyrics: " + lyricsFile);
        } else {
            System.out.println(
                    "No subtitles available for this video."
            );
        }

        return new Track(title, "YouTube", audioFile, "");
    }

    private void validateUrl(String url) {
        URI uri;

        try {
            uri = URI.create(url);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid YouTube URL"
            );
        }

        String host = uri.getHost();

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || host == null) {
            throw new IllegalArgumentException(
                    "Use a valid HTTPS YouTube URL"
            );
        }

        host = host.toLowerCase(Locale.ROOT);

        boolean validHost =
                host.equals("youtube.com") ||
                host.equals("www.youtube.com") ||
                host.equals("m.youtube.com") ||
                host.equals("music.youtube.com") ||
                host.equals("youtu.be");

        if (!validHost) {
            throw new IllegalArgumentException(
                    "Only YouTube links are supported"
            );
        }
    }
}
