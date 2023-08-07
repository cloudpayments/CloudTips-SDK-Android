package ru.cloudtips.edit_card.editcard.util;

/**
 * @author Stanislav Mukhametshin
 */
public interface CardActionButtonShowRule {

    boolean isNeedToShow(String knownNumberPart, String maskedNumber);
}
