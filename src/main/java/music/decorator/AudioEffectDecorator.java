package music.decorator;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

// Abstract Decorator
public abstract class AudioEffectDecorator implements AudioProcessor {
    protected final AudioProcessor wrapped;
    private volatile boolean enabled = false;

    protected AudioEffectDecorator(AudioProcessor wrapped) {
        this.wrapped = wrapped;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public final boolean process(AudioEvent event) {
        if (!wrapped.process(event)) {
            return false;
        }

        if (enabled) {
            applyEffect(event);
        }
        return true;
    }

    protected abstract void applyEffect(AudioEvent event);

    @Override
    public void processingFinished() {
        wrapped.processingFinished();
    }
}
