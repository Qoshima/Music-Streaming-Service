package music.strategy;

import music.model.Track;
import java.util.List;

public interface PlaybackStrategy {
    // Returns the next track to play based on the strategy
    Track nextTrack(List<Track> playlist);
}