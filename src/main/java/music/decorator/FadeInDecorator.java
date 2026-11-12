
package music.decorator;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

import java.util.concurrent.atomic.AtomicLong;

// Concrete Decorator 2
public class FadeInDecorator extends AudioEffectDecorator {
    private static final long DURATION = 44100L * 3;
    private final AtomicLong processed = new AtomicLong();

    public FadeInDecorator(AudioProcessor wrapped) {
        super(wrapped);
    }

    public void reset() {
        processed.set(0);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled && !isEnabled()) {
            reset();
        }
        super.setEnabled(enabled);
    }

    @Override
    protected void applyEffect(AudioEvent event) {
        float[] samples = event.getFloatBuffer();
        long start = processed.getAndAdd(samples.length);

        for (int i = 0; i < samples.length; i++) {
            float gain = Math.min(
                    1f, (float) (start + i) / DURATION);

            samples[i] *= gain;
        }
    }
}
