
package music.decorator;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

// Concrete Decorator: controls audio volume.
public class VolumeDecorator extends AudioEffectDecorator {

    private volatile int volume = 100;

    public VolumeDecorator(AudioProcessor wrapped) {
        super(wrapped);
        setEnabled(true);
    }

    public void setVolume(int volume) {
        if (volume < 0 || volume > 100) {
            throw new IllegalArgumentException(
                    "Volume must be between 0 and 100"
            );
        }

        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }

    @Override
    protected void applyEffect(AudioEvent event) {
        float[] samples = event.getFloatBuffer();
        float gain = volume / 100.0f;

        for (int i = 0; i < samples.length; i++) {
            samples[i] *= gain;
        }
    }
}
