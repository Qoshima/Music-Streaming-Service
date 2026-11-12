package music.decorator;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

// Concrete Decorator: adds an echo effect.
public class EchoDecorator extends AudioEffectDecorator {

    private static final int SAMPLE_RATE = 44100;
    private static final int DELAY_SAMPLES = SAMPLE_RATE / 2;

    private final float[] delay = new float[DELAY_SAMPLES];
    private final AtomicBoolean resetPending =
            new AtomicBoolean(true);

    private int position = 0;

    public EchoDecorator(AudioProcessor wrapped) {
        super(wrapped);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled && !isEnabled()) {
            reset();
        }

        super.setEnabled(enabled);
    }

    public void reset() {
        // Audio thread will clear the buffer safely.
        resetPending.set(true);
    }

    @Override
    protected void applyEffect(AudioEvent event) {
        if (resetPending.getAndSet(false)) {
            Arrays.fill(delay, 0f);
            position = 0;
        }

        float[] samples = event.getFloatBuffer();

        for (int i = 0; i < samples.length; i++) {
            float original = samples[i];
            float delayed = delay[position];

            samples[i] = clamp(
                    original + delayed * 0.35f
            );

            delay[position] = clamp(
                    original + delayed * 0.4f
            );

            position = (position + 1) % delay.length;
        }
    }

    private float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }
}
