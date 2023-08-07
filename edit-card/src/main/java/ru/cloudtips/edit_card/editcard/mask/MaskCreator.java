package ru.cloudtips.edit_card.editcard.mask;

import androidx.annotation.Nullable;

import ru.tinkoff.decoro.MaskDescriptor;

/**
 * @author Stanislav Mukhametshin
 */
public interface MaskCreator {

    MaskDescriptor createMaskDescriptor(@Nullable String shownNumberPart, @Nullable String maskedNumber);
}
