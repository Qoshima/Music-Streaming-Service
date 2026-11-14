package music.observer;

import music.model.Track;

public interface Observer {
    void update(Track track);
}