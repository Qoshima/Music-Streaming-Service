package music.adapter;

import be.tarsos.dsp.AudioDispatcher;

import java.nio.file.Path;

// Unified interface expected by our application.
public interface AudioSource {
    AudioDispatcher open(Path file);
}
