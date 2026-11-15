
package music.adapter;

import music.model.Track;
import java.io.IOException;

public interface TrackImporter {
    Track importTrack(String url)
            throws IOException, InterruptedException;
}
