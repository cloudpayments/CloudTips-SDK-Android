package ru.cloudtips.edit_card.editcard.animation;

import android.util.Property;

import ru.cloudtips.edit_card.editcard.CardBaseField;

/**
 * @author Stanislav Mukhametshin
 */
public final class CardFieldAlphaProperty extends Property<CardBaseField, Float> {

    CardFieldAlphaProperty(String name) {
        super(Float.class, name);
    }

    @Override
    public Float get(CardBaseField object) {
        return object.getAlpha();
    }

    @Override
    public void set(CardBaseField object, Float value) {
        object.setAlpha(value);
    }
}
