package music.model;

import java.nio.file.Path;

public class Track {
    private final String title;
    private final String artist;
    private final Path filePath;
    private final String lyrics;

    public Track(String title, String artist, Path filePath, String lyrics) {
        this.title = title;
        this.artist = artist;
        this.filePath = filePath;
        this.lyrics = lyrics;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public Path getFilePath() { return filePath; }
    public String getLyrics() { return lyrics; }
}