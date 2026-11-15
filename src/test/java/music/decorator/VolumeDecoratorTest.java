
package music.decorator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VolumeDecoratorTest {

    @Test
    void setsVolumeSuccessfully() {
        VolumeDecorator volume = new VolumeDecorator(
                new BaseAudioProcessor()
        );

        volume.setVolume(50);

        assertEquals(50, volume.getVolume());
    }

    @Test
    void rejectsInvalidVolume() {
        VolumeDecorator volume = new VolumeDecorator(
                new BaseAudioProcessor()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> volume.setVolume(150)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> volume.setVolume(-10)
        );
    }
}
