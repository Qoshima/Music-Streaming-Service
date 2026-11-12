package music.decorator;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

import java.util.Arrays;

// Concrete Decorator 1
public class EchoDecorator extends AudioEffectDecorator {
    private final float[] delay = new float[22050];
    private int position = 0;

    public EchoDecorator(AudioProcessor wrapped) {
        super(wrapped);
    }

    public void reset() {
        Arrays.fill(delay, 0);
        position = 0;
    }

    @Override
    protected void applyEffect(AudioEvent event) {
        float[] samples = event.getFloatBuffer();

        for (int i = 0; i < samples.length; i++) {
            float original = samples[i];
            float delayed = delay[position];

            samples[i] = Math.max(-1f, Math.min(
                    1f, original + delayed * 0.35f));

            delay[position] = Math.max(-1f, Math.min(
                    1f, original + delayed * 0.4f));

            position = (position + 1) % delay.length;
        }
    }
}
