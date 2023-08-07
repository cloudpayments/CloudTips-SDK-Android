package ru.cloudtips.edit_card.editcard;

import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;

import ru.cloudtips.edit_card.editcard.util.CardValidator;

/**
 * @author Stanislav Mukhametshin
 */
public class CvcEditText extends CardBaseField {

    @SuppressWarnings("AvoidEscapedUnicodeCharacters")
    private static final String EMPTY_DOTS = "\u2022\u2022\u2022";
    private boolean isSavedState = false;

    public CvcEditText(Context context) {
        super(context);
        initCvc();
    }

    public CvcEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCvc();
    }

    public CvcEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCvc();
    }

    protected CvcEditText(Context context, AttributeSet attrs, boolean withParentAttrs) {
        super(context, attrs, withParentAttrs);
        initCvc();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        CvcBaseState ss = new CvcBaseState(superState);
        ss.visibility = getVisibility();
        ss.isSavedState = isSavedState;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        CvcBaseState ss = (CvcBaseState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setVisibility(ss.visibility);
        isSavedState = ss.isSavedState;
        setFocusableInTouchMode(!isSavedState);
        setFocusable(!isSavedState);
        setEnabled(!isSavedState);
    }

    public void setSavedCardState(boolean savedCardState) {
        isSavedState = savedCardState;
        setEnabled(true);
        setText(savedCardState ? EMPTY_DOTS : null);
        setFocusableInTouchMode(!savedCardState);
        setFocusable(!savedCardState);
        setEnabled(!savedCardState);
    }

    @Override
    public int getMaxLength() {
        return 3;
    }

    @Override
    public boolean isValid() {
        return isSavedState || CardValidator.validateSecurityCode(getText().toString());
    }

    @Override
    public boolean isErrorTextColor() {
        return !(length() < getMaxLength() || isValid());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setTransformationMethod(PasswordTransformationMethod.getInstance());
    }

    private void initCvc() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE);
        }
    }

    protected static class CvcBaseState extends BaseSavedState {

        public static final Parcelable.Creator<CvcBaseState> CREATOR
                = new Parcelable.Creator<CvcBaseState>() {

            public CvcBaseState createFromParcel(Parcel in) {
                return new CvcBaseState(in);
            }

            public CvcBaseState[] newArray(int size) {
                return new CvcBaseState[size];
            }
        };

        int visibility;
        boolean isSavedState;

        CvcBaseState(Parcelable superState) {
            super(superState);
        }

        CvcBaseState(Parcel in) {
            super(in);
            visibility = in.readInt();
            isSavedState = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(visibility);
            out.writeInt(isSavedState ? 1 : 0);
        }
    }
}
