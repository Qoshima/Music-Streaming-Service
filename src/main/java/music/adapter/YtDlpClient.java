
package music.adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class YtDlpClient {

    public Path download(String url, Path directory)
            throws IOException, InterruptedException {

        Path folder = directory.toAbsolutePath().normalize();
        Files.createDirectories(folder);

        String binary = System.getenv("YT_DLP_BIN");
        if (binary == null || binary.isBlank()) {
            binary = "yt-dlp";
        }

        String template = folder.resolve(
                "%(title).80s [%(id)s].%(ext)s"
        ).toString();

        List<String> command = List.of(
                binary,
                "--ignore-errors",
                "--no-playlist",
                "--no-progress",
                "-f", "bestaudio/best",
                "-x",
                "--audio-format", "mp3",
                "--write-subs",
                "--write-auto-subs",
                "--sub-langs", "ru,en,kk",
                "--sub-format", "vtt/best",
                "--convert-subs", "lrc",
                "-o", template,
                "--print", "after_move:MSS_AUDIO=%(filepath)s",
                url
        );

        Process process;

        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new IOException(
                    "yt-dlp not found. Check installation and PATH.",
                    e
            );
        }

        Path downloadedFile = null;
        String lastMessage = "";

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        process.getInputStream(),
                        StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MSS_AUDIO=")) {
                    downloadedFile = Path.of(
                            line.substring("MSS_AUDIO=".length())
                    ).toAbsolutePath().normalize();
                } else {
                    System.out.println("[yt-dlp] " + line);
                    lastMessage = line;
                }
            }
        }

        int code;

        try {
            code = process.waitFor();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw e;
        }

        if (code != 0) {
            throw new IOException(
                    "yt-dlp failed: " + lastMessage
            );
        }

        if (downloadedFile == null ||
                !downloadedFile.startsWith(folder) ||
                !Files.isRegularFile(downloadedFile)) {
            throw new IOException("Downloaded audio not found.");
        }

        return downloadedFile;
    }
}
