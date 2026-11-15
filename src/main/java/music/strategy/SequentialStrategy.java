package music.strategy;

public class SequentialStrategy implements PlaybackStrategy {

    @Override
    public int nextIndex(int currentIndex, int playlistSize) {
        if (playlistSize <= 0) {
            throw new IllegalArgumentException("Empty playlist");
        }

        return (currentIndex + 1) % playlistSize;
    }
}
