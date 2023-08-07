package ru.cloudtips.edit_card.editcard.mask;

import ru.tinkoff.decoro.slots.Slot;

import static ru.tinkoff.decoro.slots.PredefinedSlots.hardcodedSlot;
import static ru.tinkoff.decoro.slots.PredefinedSlots.maskableDigit;

/**
 * @author Stanislav Mukhametshin
 */
public final class CardMaskUtil {

    /**
     * Standard mask for 12 digits card number (4-4-4-4)
     */
    public static final Slot[] CARD_NUMBER_STANDARD_MASKABLE = {
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit()
    };

    /**
     * @deprecated mask for maestro card (8-11), use mask (6-13)
     */
    @Deprecated
    public static final Slot[] CARD_NUMBER_MAESTRO_MASKABLE = {
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit()
    };

    /**
     * Maestro mask for 13 digits (4-4-5)
     */
    static final Slot[] CARD_NUMBER_MAESTRO13_MASKABLE = {
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit()
    };

    /**
     * Maestro mask for 15 digits (4-6-5)
     */
    static final Slot[] CARD_NUMBER_MAESTRO15_MASKABLE = {
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit()
    };

    /**
     * Base mask for 19 digits card number (6-13)
     */
    static final Slot[] CARD_NUMBER_19_MASKABLE = {
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit()
    };

    /**
     * not masked 17..18 symbols
     */
    static final Slot[] CARD_NUMBER_NOT_MASKABLE = {
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit(),
            maskableDigit()
    };

    public static final char MASK_CHAR = '*';

    public static boolean compareMaskNumbers(String maskFirst, String maskSecond) {
        if (maskFirst == null || maskSecond == null) {
            return false;
        }
        String maskFirstRefactored = maskFirst.replace(" ", "");
        String maskSecondRefactored = maskSecond.replace(" ", "");
        if (maskFirstRefactored.length() != maskSecondRefactored.length()) {
            return false;
        }
        char charMaskFirst;
        char charMaskSecond;
        for (int pos = maskFirstRefactored.length() - 1; pos >= 0; pos--) {
            charMaskFirst = maskFirstRefactored.charAt(pos);
            charMaskSecond = maskSecondRefactored.charAt(pos);
            if (charMaskFirst != MASK_CHAR && charMaskSecond != MASK_CHAR && charMaskFirst != charMaskSecond) {
                return false;
            }
        }
        return true;
    }
}
