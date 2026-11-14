package music.builder;

import music.model.Track;
import java.util.ArrayList;
import java.util.List;

public class PlaylistBuilder {
    private String name;
    private final List<Track> tracks = new ArrayList<>();

    public PlaylistBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public PlaylistBuilder addTrack(Track track) {
        this.tracks.add(track);
        return this;
    }

    public Playlist build() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Playlist must have a name.");
        }
        if (tracks.isEmpty()) {
            throw new IllegalStateException("Playlist cannot be empty.");
        }
        return new Playlist(name, tracks);
    }
}