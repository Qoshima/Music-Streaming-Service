package music.observer;

public class LyricLine {

    private final long timeMillis;
    private final String text;

    public LyricLine(long timeMillis, String text) {
        this.timeMillis = timeMillis;
        this.text = text;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public String getText() {
        return text;
    }
}
