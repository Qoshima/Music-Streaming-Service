
package music.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StrategyTest {

    @Test
    void sequentialMovesToNextTrack() {
        SequentialStrategy strategy = new SequentialStrategy();

        assertEquals(1, strategy.nextIndex(0, 3));
        assertEquals(2, strategy.nextIndex(1, 3));
    }

    @Test
    void sequentialReturnsToFirstTrack() {
        SequentialStrategy strategy = new SequentialStrategy();

        assertEquals(0, strategy.nextIndex(2, 3));
    }

    @Test
    void shuffleDoesNotRepeatCurrentTrack() {
        ShuffleStrategy strategy = new ShuffleStrategy();

        for (int i = 0; i < 100; i++) {
            int next = strategy.nextIndex(1, 3);

            assertNotEquals(1, next);
            assertTrue(next >= 0 && next < 3);
        }
    }

    @Test
    void emptyPlaylistThrowsException() {
        SequentialStrategy sequential = new SequentialStrategy();
        ShuffleStrategy shuffle = new ShuffleStrategy();

        assertThrows(
                IllegalArgumentException.class,
                () -> sequential.nextIndex(0, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> shuffle.nextIndex(0, 0)
        );
    }
}
