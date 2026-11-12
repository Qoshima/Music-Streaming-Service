package music.adapter;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

import java.nio.file.Files;
import java.nio.file.Path;

// Converts our interface to TarsosDSP/FFmpeg API.
public class FfmpegAudioAdapter implements AudioSource {

    @Override
    public AudioDispatcher open(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(
                    "File not found: " + file);
        }

        return AudioDispatcherFactory.fromPipe(
                file.toAbsolutePath().toString(),
                44100,
                2048,
                0
        );
    }
}
