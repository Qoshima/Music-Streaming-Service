
package music.adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class YtDlpClient {

    public Path download(String url, Path directory)
            throws IOException, InterruptedException {

        Path folder = directory.toAbsolutePath().normalize();
        Files.createDirectories(folder);

        String template = folder.resolve(
                "%(title).80s [%(id)s].%(ext)s"
        ).toString();

        // Step 1: Download audio first.
        List<String> audioCommand = List.of(
                executable(),
                "--no-playlist",
                "--no-progress",
                "-f", "bestaudio/best",
                "-x",
                "--audio-format", "mp3",
                "-o", template,
                "--print", "after_move:MSS_AUDIO=%(filepath)s",
                url
        );

        String result = run(audioCommand);

        if (result == null || result.isBlank()) {
            throw new IOException(
                    "yt-dlp did not return the MP3 path."
            );
        }

        Path audio = Path.of(result)
                .toAbsolutePath()
                .normalize();

        if (!audio.startsWith(folder) ||
                !Files.isRegularFile(audio)) {
            throw new IOException(
                    "Downloaded MP3 was not found: " + audio
            );
        }

        // Step 2: Download subtitles independently.
        downloadSubtitles(url, template, audio);

        return audio;
    }

    private void downloadSubtitles(
            String url, String template, Path audio)
            throws InterruptedException {

        // Try original-language automatic captions.
        try {
            run(List.of(
                    executable(),
                    "--no-playlist",
                    "--skip-download",
                    "--no-progress",
                    "--write-auto-subs",
                    "--sub-langs", ".*-orig",
                    "--sub-format", "vtt/best",
                    "--convert-subs", "lrc",
                    "-o", template,
                    url
            ));
        } catch (IOException e) {
            System.out.println(
                    "Original captions unavailable: "
                            + e.getMessage()
            );
        }

        if (hasLrc(audio)) {
            return;
        }

        // Fallback: subtitles uploaded by the video creator.
        try {
            run(List.of(
                    executable(),
                    "--no-playlist",
                    "--skip-download",
                    "--no-progress",
                    "--write-subs",
                    "--sub-langs", "all,-live_chat",
                    "--sub-format", "vtt/best",
                    "--convert-subs", "lrc",
                    "-o", template,
                    url
            ));
        } catch (IOException e) {
            System.out.println(
                    "Manual subtitles unavailable: "
                            + e.getMessage()
            );
        }
    }

    private boolean hasLrc(Path audio) {
        String filename = audio.getFileName().toString();
        String base = filename.substring(
                0, filename.lastIndexOf('.')
        );

        try (Stream<Path> files = Files.list(audio.getParent())) {
            return files.anyMatch(file -> {
                String name = file.getFileName().toString();

                return Files.isRegularFile(file) &&
                        (name.equals(base + ".lrc") ||
                                (name.startsWith(base + ".") &&
                                        name.endsWith(".lrc")));
            });
        } catch (IOException e) {
            return false;
        }
    }

    private String executable() {
        String configured = System.getenv("YT_DLP_BIN");

        return configured == null || configured.isBlank()
                ? "yt-dlp"
                : configured;
    }

    private String run(List<String> command)
            throws IOException, InterruptedException {

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String audioPath = null;
        String lastLine = "Unknown error";

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        process.getInputStream(),
                        StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MSS_AUDIO=")) {
                    audioPath = line.substring(
                            "MSS_AUDIO=".length()
                    );
                } else if (!line.isBlank()) {
                    System.out.println("[yt-dlp] " + line);
                    lastLine = line;
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
                    lastLine + " (exit code " + code + ")"
            );
        }

        return audioPath;
    }
}
