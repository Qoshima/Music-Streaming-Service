package music.observer;

import music.model.Track;
import java.util.List;

public class LyricsDisplay implements Observer {

    private Track currentTrack;
    private List<LyricLine> lyrics;
    private int lastLine = -2;
    private long lastPosition = -1;

    @Override
    public synchronized void update(
            Track track, long positionMillis) {

        if (track == null) {
            return;
        }

        // Load lyrics when the song changes or restarts.
        if (track != currentTrack ||
                positionMillis < lastPosition) {

            currentTrack = track;
            lyrics = LrcParser.load(track.getFilePath());
            lastLine = -2;

            System.out.println(
                    "[LYRICS] Track: " + track.getTitle()
            );
        }

        lastPosition = positionMillis;

        // If no LRC exists, show simple lyrics once.
        if (lyrics.isEmpty()) {
            if (lastLine != -1) {
                String text = track.getLyrics();

                System.out.println(
                        "[LYRICS] " +
                                (text == null || text.isBlank()
                                        ? "No lyrics available."
                                        : text)
                );

                lastLine = -1;
            }

            return;
        }

        int currentLine = -1;

        for (int i = 0; i < lyrics.size(); i++) {
            if (lyrics.get(i).getTimeMillis()
                    <= positionMillis) {
                currentLine = i;
            } else {
                break;
            }
        }

        if (currentLine != lastLine) {
            lastLine = currentLine;

            if (currentLine >= 0) {
                System.out.println(
                        "[LYRICS] " +
                                lyrics.get(currentLine).getText()
                );
            }
        }
    }
}
