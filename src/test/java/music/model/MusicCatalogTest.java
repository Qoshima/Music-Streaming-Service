
package music.model;

import music.builder.Playlist;
import music.builder.PlaylistBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MusicCatalogTest {

    private Track createTrack() {
        return new Track(
                "Hello",
                "Artist",
                Path.of("hello.mp3"),
                ""
        );
    }

    @Test
    void findsTrackIgnoringCase() {
        MusicCatalog catalog = new MusicCatalog();
        catalog.addTrack(createTrack());

        assertNotNull(catalog.findTrackByTitle("HELLO"));
        assertNotNull(catalog.findTrackByTitle("hello"));
    }

    @Test
    void returnsNullForUnknownTrack() {
        MusicCatalog catalog = new MusicCatalog();

        assertNull(catalog.findTrackByTitle("Unknown"));
    }

    @Test
    void savesAndFindsPlaylist() {
        MusicCatalog catalog = new MusicCatalog();

        Playlist playlist = new PlaylistBuilder()
                .setName("Favorites")
                .addTrack(createTrack())
                .build();

        catalog.savePlaylist(playlist);

        assertSame(
                playlist,
                catalog.findPlaylistByName("favorites")
        );
    }
}
