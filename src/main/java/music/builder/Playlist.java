package music.builder;

import music.model.Track;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Playlist {
    private final String name;
    private final List<Track> tracks;

    public Playlist(String name, List<Track> tracks) {
        this.name = name;
        this.tracks = Collections.unmodifiableList(tracks); 
    }

    public String getName() { return name; }
    public List<Track> getTracks() { return tracks; }
}