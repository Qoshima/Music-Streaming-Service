package music.observer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcParser {

    public static List<LyricLine> load(Path audioPath) {

        String filename = audioPath.getFileName().toString();
        int dot = filename.lastIndexOf('.');

        String name = dot >= 0
                ? filename.substring(0, dot)
                : filename;

        Path lyricsPath = audioPath.resolveSibling(name + ".lrc");

        List<LyricLine> result = new ArrayList<>();

        if (!Files.exists(lyricsPath)) {
            return result;
        }

        Pattern pattern = Pattern.compile(
                "\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]"
        );

        try {
            List<String> lines = Files.readAllLines(
                    lyricsPath, StandardCharsets.UTF_8
            );

            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                String text = matcher.replaceAll("").trim();

                if (text.isEmpty()) {
                    continue;
                }

                matcher.reset();

                while (matcher.find()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));

                    if (seconds >= 60) {
                        continue;
                    }

                    String fraction = matcher.group(3);
                    int milliseconds = 0;

                    if (fraction != null) {
                        milliseconds = Integer.parseInt(
                                (fraction + "000").substring(0, 3)
                        );
                    }

                    long time = (minutes * 60L + seconds)
                            * 1000 + milliseconds;

                    result.add(new LyricLine(time, text));
                }
            }

        } catch (IOException e) {
            System.out.println(
                    "Cannot read lyrics: " + e.getMessage()
            );
        }

        result.sort(Comparator.comparingLong(
                LyricLine::getTimeMillis
        ));

        return result;
    }
}
