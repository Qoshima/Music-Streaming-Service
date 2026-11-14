package music.data;

import music.model.MusicCatalog;
import music.model.Track;
import java.nio.file.Path;

public class MusicDataLoader {
    public static void loadInitialData(MusicCatalog catalog) {
        catalog.addTrack(new Track("Test1", "Artist 1", Path.of("music/test.mp3"), "La la la..."));
        catalog.addTrack(new Track("Test2", "Artist 2", Path.of("music/test.flac"), "Na na na..."));
        catalog.addTrack(new Track("Test3", "Artist 3", Path.of("music/test.m4a"), "Do do do..."));
        catalog.addTrack(new Track("Test4", "Artist 4", Path.of("music/test.webm"), "Beep boop..."));
        
    }
}