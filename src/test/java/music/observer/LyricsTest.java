
package music.observer;

import music.model.Track;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LyricsTest {

    @TempDir
    Path directory;

    @Test
    void parsesLyricsInTimeOrder() throws Exception {
        Path audio = directory.resolve("song.mp3");
        Path lrc = directory.resolve("song.lrc");

        Files.writeString(lrc,
                "[00:03.00]Second line\n" +
                "[00:01.50]First line\n"
        );

        List<LyricLine> lines = LrcParser.load(audio);

        assertEquals(2, lines.size());
        assertEquals("First line", lines.get(0).getText());
        assertEquals(1500, lines.get(0).getTimeMillis());
        assertEquals("Second line", lines.get(1).getText());
    }

    @Test
    void returnsEmptyLyricsWhenFileMissing() {
        Path audio = directory.resolve("missing.mp3");

        assertTrue(LrcParser.load(audio).isEmpty());
    }

    @Test
    void observerDisplaysLyricsAtCorrectTime() throws Exception {
        Path audio = directory.resolve("song.mp3");
        Path lrc = directory.resolve("song.lrc");

        Files.writeString(lrc,
                "[00:01.00]Hello\n" +
                "[00:03.00]World\n"
        );

        Track track = new Track("Song", "Artist", audio, "");
        LyricsDisplay observer = new LyricsDisplay();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;

        try (PrintStream capture = new PrintStream(
                output, true, StandardCharsets.UTF_8)) {

            System.setOut(capture);

            observer.update(track, 0);
            observer.update(track, 1500);
            observer.update(track, 3500);

        } finally {
            System.setOut(original);
        }

        String result = output.toString(StandardCharsets.UTF_8);

        assertTrue(result.contains("[LYRICS] Hello"));
        assertTrue(result.contains("[LYRICS] World"));
    }
}
