
package music.builder;

import music.model.Track;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PlaylistBuilderTest {

    private Track createTrack() {
        return new Track(
                "Test Song",
                "Artist",
                Path.of("music/test.mp3"),
                ""
        );
    }

    @Test
    void buildsPlaylistSuccessfully() {
        Playlist playlist = new PlaylistBuilder()
                .setName("Favorites")
                .addTrack(createTrack())
                .build();

        assertEquals("Favorites", playlist.getName());
        assertEquals(1, playlist.getTracks().size());
        assertEquals(
                "Test Song",
                playlist.getTracks().get(0).getTitle()
        );
    }

    @Test
    void playlistWithoutNameThrowsException() {
        PlaylistBuilder builder = new PlaylistBuilder()
                .addTrack(createTrack());

        assertThrows(
                IllegalStateException.class,
                builder::build
        );
    }

    @Test
    void emptyPlaylistThrowsException() {
        PlaylistBuilder builder = new PlaylistBuilder()
                .setName("Empty");

        assertThrows(
                IllegalStateException.class,
                builder::build
        );
    }

    @Test
    void playlistCannotBeModifiedDirectly() {
        Playlist playlist = new PlaylistBuilder()
                .setName("Favorites")
                .addTrack(createTrack())
                .build();

        assertThrows(
                UnsupportedOperationException.class,
                () -> playlist.getTracks().add(createTrack())
        );
    }
}
