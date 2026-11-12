package music.adapter;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public final class FfmpegDecoder {
    public AudioDispatcher decode(String filePath, int sampleRatem, int bufferSize, int overlap) {
        return AudioDispatcherFactory.fromPipe(filePath, sampleRatem, bufferSize, overlap);
    }
}
