package music.model;

import music.builder.Playlist;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class MusicCatalog {
    private final List<Track> tracks = new ArrayList<>();
    private final Map<String, Playlist> playlists = new HashMap<>();

    public void addTrack(Track track) {
        tracks.add(track);
    }

    public void savePlaylist(Playlist playlist) {
        playlists.put(playlist.getName().toLowerCase(), playlist);
    }

    public Track findTrackByTitle(String title) {
    for (Track t : tracks) {
        if (t.getTitle().equalsIgnoreCase(title)) {
            return t;
        }
    }
    return null;
}

    public Playlist findPlaylistByName(String name) {
        return playlists.get(name.toLowerCase());
    }
}