package ru.cloudtips.edit_card.editcard.util;

import android.text.TextUtils;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a utility class to check validity of the card
 */
public class CardValidator {

    public static final int DEFAULT_MAX_SYMBOL_LENGTH = 19;

    private static final int[] VISA_CARD_LENGTHS = {16};
    private static final int[] MASTERCARD_CARD_LENGTHS = {16};
    private static final int[] MIR_CARD_LENGTHS = {16, 17, 18, 19};
    private static final int[] MAESTRO_CARD_LENGTHS = {12, 13, 14, 15, 16, 17, 18, 19};
    private static final int[] UNION_PAY_CARD_LENGTHS = {13, 14, 15, 16, 17, 18, 19};
    private static final int[] DEFAULT_CARD_LENGTHS = {16};

    private static final String VISA_REGEXP = "^(4[0-9]*)$";
    private static final String MASTERCARD_REGEXP = "^(5(?!05827|61468)[0-9]*)$";
    private static final String MIR_REGEXP_UPDATED = "^((220[0-4]|356|505827|561468|623446|629129|629157|629244|676347|676454|676531|671182|676884|676907|677319|677384|8600|9051|9112(?!00|50|39|99)|9417(?!00|99)|9762|9777|9990(?!01))[0-9]*)$";
    private static final String MAESTRO_REGEXP = "^(6(?!2|76347|76454|76531|71182|76884|76907|77319|77384)[0-9]*)$";
    private static final String UNION_PAY_REGEXP = "^((81[0-6]|817[01]|62(?!3446|9129|9157|9244))[0-9]*)$";

    private static final String ZERO_NUMBERS_CARD_NUMBER_REGEXP = "[0]{1,}";
    private static final String CVC_REGEXP = "^[0-9]{3}$";

    @Deprecated
    public static final int DEFAULT_CARD_LENGTH = 16;

    @Deprecated
    public static final Pattern MIR_REGEXP = Pattern.compile(MIR_REGEXP_UPDATED);

    public static boolean validateNumber(String cardNumber) {
        if (TextUtils.isEmpty(cardNumber)) {
            return false;
        }

        if (RegexpValidator.matchesFully(cardNumber, ZERO_NUMBERS_CARD_NUMBER_REGEXP)) {
            return false;
        }

        if (!isKnownPaymentSystem(cardNumber)) {
            return false;
        }

        if (!validateCardNumberLength(cardNumber)) {
            return false;
        }

        return validateWithLuhnAlgorithm(cardNumber);
    }

    /**
     * @deprecated use validateCardNumberLength(String) instead
     */
    @Deprecated
    public static boolean isNumberNeedToCheck(String cardNumber) {
        return validateCardNumberLength(cardNumber);
    }

    public static boolean validateCardNumberLength(String cardNumber) {
        if (TextUtils.isEmpty(cardNumber)) {
            return false;
        }

        int[] allowedCardLengths;
        if (isVisa(cardNumber)) {
            allowedCardLengths = VISA_CARD_LENGTHS;
        } else if (isMastercard(cardNumber)) {
            allowedCardLengths = MASTERCARD_CARD_LENGTHS;
        } else if (isMir(cardNumber)) {
            allowedCardLengths = MIR_CARD_LENGTHS;
        } else if (isMaestro(cardNumber)) {
            allowedCardLengths = MAESTRO_CARD_LENGTHS;
        } else if (isUnionPay(cardNumber)) {
            allowedCardLengths = UNION_PAY_CARD_LENGTHS;
        } else {
            allowedCardLengths = DEFAULT_CARD_LENGTHS;
        }

        final int cardNumberLength = cardNumber.length();

        for (int length : allowedCardLengths) {
            if (length == cardNumberLength) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNotStandardValidNumber(boolean isValid, String number) {
        return isValid && validateCardNumberLength(number) &&
                (isMir(number) || isMaestro(number) || isUnionPay(number));
    }

    public static boolean isDefaultNumberFormat(String cardNumber) {
        return !isMir(cardNumber) && !isMaestro(cardNumber) && !isUnionPay(cardNumber);
    }

    public static boolean isVisa(String cardNumber) {
        return !TextUtils.isEmpty(cardNumber) && RegexpValidator.matchesPartially(cardNumber, VISA_REGEXP);
    }

    public static boolean isMastercard(String cardNumber) {
        return !TextUtils.isEmpty(cardNumber) && RegexpValidator.matchesPartially(cardNumber, MASTERCARD_REGEXP);
    }

    public static boolean isMir(String cardNumber) {
        return !TextUtils.isEmpty(cardNumber) && RegexpValidator.matchesPartially(cardNumber, MIR_REGEXP_UPDATED);
    }

    public static boolean isMaestro(String cardNumber) {
        return !TextUtils.isEmpty(cardNumber) && RegexpValidator.matchesPartially(cardNumber, MAESTRO_REGEXP);
    }

    public static boolean isUnionPay(String cardNumber) {
        return !TextUtils.isEmpty(cardNumber) && RegexpValidator.matchesPartially(cardNumber, UNION_PAY_REGEXP);
    }

    public static boolean isMaxSymbols(String cardNumber) {
        return cardNumber != null && cardNumber.length() == DEFAULT_MAX_SYMBOL_LENGTH;
    }

    public static boolean validateSecurityCode(String cvc) {
        return !TextUtils.isEmpty(cvc) && RegexpValidator.matchesFully(cvc, CVC_REGEXP);
    }

    public static boolean validateExpirationDate(String expiryDate) {
        return validateExpirationDate(expiryDate, false);
    }

    /**
     * Validate specified date
     * - on empty
     * - on right length
     * - month between 1 and 12
     * - on expire date
     *
     * @param expiryDate   specified date on string format
     * @param allowExpired allow expired cards during validation or not
     * @return true if valid else false
     */
    public static boolean validateExpirationDate(String expiryDate, boolean allowExpired) {
        if (TextUtils.isEmpty(expiryDate) || expiryDate.length() != 5) {
            return false;
        }

        int month;
        int year;

        try {
            month = Integer.parseInt(expiryDate.substring(0, 2));
            year = Integer.parseInt(expiryDate.substring(3, 5));
        } catch (NumberFormatException e) {
            return false;
        }

        if (month >= 1 && month <= 12) {
            if (allowExpired) {
                return true;
            } else {
                Calendar c = Calendar.getInstance();
                String currentYearStr = Integer.toString(c.get(Calendar.YEAR)).substring(2);
                int currentMonth = c.get(Calendar.MONTH) + 1;
                int currentYear = Integer.parseInt(currentYearStr);
                return (year == currentYear && month >= currentMonth)
                        || (year > currentYear && year <= currentYear + 20);
            }
        }

        return false;
    }

    private static boolean isKnownPaymentSystem(String knownDigits) {
        return isVisa(knownDigits) || isMastercard(knownDigits) || isMir(knownDigits) ||
                isMaestro(knownDigits) || isUnionPay(knownDigits);
    }

    // http://en.wikipedia.org/wiki/Luhn_algorithm
    private static boolean validateWithLuhnAlgorithm(String cardNumber) {
        int sum = 0;
        int value;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            value = Character.getNumericValue(cardNumber.charAt(i));
            if (value == -1 || value == -2) {
                return false;
            }
            boolean shouldBeDoubled = (cardNumber.length() - i) % 2 == 0;

            if (shouldBeDoubled) {
                value *= 2;
                sum += value > 9 ? 1 + value % 10 : value;
            } else {
                sum += value;
            }
        }

        return sum % 10 == 0;
    }

    private static class RegexpValidator {

        private RegexpValidator() {
        }

        private static boolean matchesFully(CharSequence string, String regexp) {
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(string);
            return matcher.matches();
        }

        private static boolean matchesPartially(CharSequence string, String regexp) {
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(string);
            return matcher.find();
        }
    }
}
