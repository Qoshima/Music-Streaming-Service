package music.strategy;

import java.util.Random;

public class ShuffleStrategy implements PlaybackStrategy {

    private final Random random = new Random();

    @Override
    public int nextIndex(int currentIndex, int playlistSize) {
        if (playlistSize <= 0) {
            throw new IllegalArgumentException("Empty playlist");
        }

        if (playlistSize == 1) {
            return 0;
        }

        int next;

        do {
            next = random.nextInt(playlistSize);
        } while (next == currentIndex);

        return next;
    }
}
