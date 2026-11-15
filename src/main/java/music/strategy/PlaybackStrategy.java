package music.strategy;

public interface PlaybackStrategy {

    int nextIndex(int currentIndex, int playlistSize);

}
