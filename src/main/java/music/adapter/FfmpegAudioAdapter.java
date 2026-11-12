package music.adapter;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class FfmpegAudioAdapter implements AudioSource {
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 2048;

    private final FfmpegDecoder decoder;

    public FfmpegAudioAdapter() {
        this(new FfmpegDecoder());
    }
    public FfmpegAudioAdapter(FfmpegDecoder decoder) {
        this.decoder = Objects.requireNonNull(decoder);
    }

    @Override
    public AudioDispatcher open(Path file) {
        Objects.requireNonNull(file, "Audio file cannot be null");

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Audio file not found: " + file);
        }

        if (!Files.isReadable(file)) {
            throw new IllegalArgumentException("Audio file is not readable: " + file);
        }

        return decoder.decode(
                file.toAbsolutePath().toString(), SAMPLE_RATE, BUFFER_SIZE, 0
        );
    }
}