package music.decorator;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

// Basic component
public class BaseAudioProcessor implements AudioProcessor {
    @Override
    public boolean process(AudioEvent event) {
        return true;
    }

    @Override
    public void processingFinished() {
    }
}
