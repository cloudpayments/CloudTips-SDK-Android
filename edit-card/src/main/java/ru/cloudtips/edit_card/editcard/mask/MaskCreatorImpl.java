package ru.cloudtips.edit_card.editcard.mask;

import ru.tinkoff.decoro.MaskDescriptor;
import ru.cloudtips.edit_card.editcard.util.CardValidator;

import static ru.tinkoff.decoro.MaskDescriptor.ofSlots;

/**
 * @author Stanislav Mukhametshin
 */
public class MaskCreatorImpl implements MaskCreator {

    /**
     * Create mask descriptor by shown number part.
     * <a href="https://wiki.tcsbank.ru/pages/viewpage.action?pageId=1556747782">Input masks reference</a>
     *
     * @param shownNumberPart
     * @param maskedNumber
     */
    @Override
    public MaskDescriptor createMaskDescriptor(String shownNumberPart, String maskedNumber) {
        MaskDescriptor newDescriptor;
        MaskDescriptor standardMaskable = ofSlots(CardMaskUtil.CARD_NUMBER_STANDARD_MASKABLE);
        MaskDescriptor notMaskable = ofSlots(CardMaskUtil.CARD_NUMBER_NOT_MASKABLE);

        final int numberLength = shownNumberPart != null ? shownNumberPart.length() : 0;
        if (CardValidator.isVisa(shownNumberPart) || CardValidator.isVisa(maskedNumber)) {
            newDescriptor = standardMaskable;
        } else if (CardValidator.isMastercard(shownNumberPart) || CardValidator.isMastercard(maskedNumber)) {
            newDescriptor = standardMaskable;
        } else if (CardValidator.isMir(shownNumberPart) || CardValidator.isMir(maskedNumber)) {
            if (numberLength >= 19) {
                newDescriptor = ofSlots(CardMaskUtil.CARD_NUMBER_19_MASKABLE);
            } else if (numberLength > 16) {
                newDescriptor = notMaskable;
            } else {
                newDescriptor = standardMaskable;
            }
        } else if (CardValidator.isMaestro(shownNumberPart) || CardValidator.isMaestro(maskedNumber)) {
            if (numberLength == 12 || numberLength == 14 || (numberLength > 16 && numberLength < 19)) {
                newDescriptor = notMaskable;
            } else if (numberLength == 13) {
                newDescriptor = ofSlots(CardMaskUtil.CARD_NUMBER_MAESTRO13_MASKABLE);
            } else if (numberLength == 15) {
                newDescriptor = ofSlots(CardMaskUtil.CARD_NUMBER_MAESTRO15_MASKABLE);
            } else if (numberLength >= 19) {
                newDescriptor = ofSlots(CardMaskUtil.CARD_NUMBER_19_MASKABLE);
            } else {
                newDescriptor = standardMaskable;
            }
        } else if (CardValidator.isUnionPay(shownNumberPart) || CardValidator.isUnionPay(maskedNumber)) {
            if (numberLength >= 19) {
                newDescriptor = ofSlots(CardMaskUtil.CARD_NUMBER_19_MASKABLE);
            } else if (numberLength > 16) {
                newDescriptor = notMaskable;
            } else {
                newDescriptor = standardMaskable;
            }
        } else {
            newDescriptor = standardMaskable;
        }
        return newDescriptor;
    }
}
