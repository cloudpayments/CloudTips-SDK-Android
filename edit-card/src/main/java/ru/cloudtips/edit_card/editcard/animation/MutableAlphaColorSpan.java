package ru.cloudtips.edit_card.editcard.animation;

import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.util.Property;

/**
 * @author Stanislav Mukhametshin
 */
public class MutableAlphaColorSpan extends CharacterStyle implements UpdateAppearance {

    private int color;
    private int alpha;

    public MutableAlphaColorSpan(int color) {
        super();
        this.color = color;
    }

    public void setColor(int color) {
        this.color = color;
        setAlpha(alpha);
    }

    public int getAlpha() {
        return color >>> 24;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
        color = (color & 0x00FFFFFF) | (alpha << 24);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.setColor(color);
    }

    public static final Property<MutableAlphaColorSpan, Integer> ALPHA = new Property<MutableAlphaColorSpan, Integer>(Integer.class, "span_alpha") {

        @Override
        public Integer get(MutableAlphaColorSpan object) {
            return object.getAlpha();
        }

        @Override
        public void set(MutableAlphaColorSpan object, Integer value) {
            object.setAlpha(value);
        }
    };
}
