package ru.cloudtips.edit_card.editcard;

import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;

import ru.tinkoff.decoro.MaskDescriptor;
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser;
import ru.cloudtips.edit_card.editcard.util.CardValidator;

import static ru.tinkoff.decoro.MaskDescriptor.ofSlots;

/**
 * @author Stanislav Mukhametshin
 */
public class DateEditText extends CardBaseField {

    public static final String DEFAULT_MASK = "__/__";

    private boolean allowExpired = false;

    public DateEditText(Context context) {
        super(context);
        initDate();
    }

    public DateEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDate();
    }

    public DateEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDate();
    }

    protected DateEditText(Context context, AttributeSet attrs, boolean withParentAttrs) {
        super(context, attrs, withParentAttrs);
        initDate();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        DateBaseState ss = new DateBaseState(superState);
        ss.visibility = getVisibility();
        ss.isEnabled = isEnabled();
        ss.isFocusable = isFocusable();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        DateBaseState ss = (DateBaseState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setVisibility(ss.visibility);
        setFocusableInTouchMode(ss.isFocusable);
        setFocusable(ss.isFocusable);
        setEnabled(ss.isEnabled);
    }

    private void initDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE);
        }
    }

    @Override
    public boolean isValid() {
        return CardValidator.validateExpirationDate(getText().toString(), allowExpired);
    }

    @Override
    public boolean isErrorTextColor() {
        return !(length() < DEFAULT_MASK.length() || isValid());
    }

    @Override
    public int getMaxLength() {
        return DEFAULT_MASK.length();
    }

    @Override
    protected MaskDescriptor getMask() {
        return ofSlots(new UnderscoreDigitSlotsParser().parseSlots(DEFAULT_MASK));
    }

    public void setSavedCardState(boolean savedCardState) {
        setFocusableInTouchMode(!savedCardState);
        setFocusable(!savedCardState);
        setEnabled(!savedCardState);
    }

    public boolean isAllowExpired() {
        return allowExpired;
    }

    public void setAllowExpired(boolean checkExpiration) {
        this.allowExpired = checkExpiration;
    }

    protected static class DateBaseState extends BaseSavedState {

        int visibility;
        boolean isFocusable;
        boolean isEnabled;

        DateBaseState(Parcelable superState) {
            super(superState);
        }

        DateBaseState(Parcel in) {
            super(in);
            visibility = in.readInt();
            isFocusable = in.readInt() == 1;
            isEnabled = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(visibility);
            out.writeInt(isFocusable ? 1 : 0);
            out.writeInt(isEnabled ? 1 : 0);
        }

        public static final Parcelable.Creator<DateBaseState> CREATOR
                = new Parcelable.Creator<DateBaseState>() {

            public DateBaseState createFromParcel(Parcel in) {
                return new DateBaseState(in);
            }

            public DateBaseState[] newArray(int size) {
                return new DateBaseState[size];
            }
        };
    }
}
