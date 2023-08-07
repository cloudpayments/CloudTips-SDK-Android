package ru.cloudtips.edit_card.editcard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStructure;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import java.util.HashSet;
import java.util.Set;

import ru.cloudtips.edit_card.R;
import ru.tinkoff.decoro.FormattedTextChangeListener;
import ru.tinkoff.decoro.MaskDescriptor;
import ru.tinkoff.decoro.watchers.DescriptorFormatWatcher;
import ru.tinkoff.decoro.watchers.FormatWatcher;

/**
 * @author Stanislav Mukhametshin
 * <p>
 * A base element of the whole library. If you want to create new custom edittext
 * you should extend it from CardBaseField. CardBaseField can change focus of the element
 * and move the focus to other elements. Also it provides some methods for checking the field.
 * This methods in the most of cases should be overriden.
 */
public abstract class CardBaseField extends AppCompatEditText implements TextWatcher, FormattedTextChangeListener {

    protected int previousViewId;
    protected int nextViewId;
    protected int errorColor;
    protected int textColor;
    protected DescriptorFormatWatcher formatWatcher;
    protected MaskDescriptor maskDescriptor;
    private boolean isMoveToPrevAllowed;
    private boolean isNeededToIgnorePrev = true;
    private boolean isGoneMode;
    @Nullable
    private Set<CardTextChangedListener> textChangedListenerSet;
    private FieldFilledCorrectListener fieldFilledCorrectListener;
    private String lastKnownText = null;
    @Nullable
    private OnFocusChangeListener focusChangeListenerInternal;

    public CardBaseField(Context context) {
        super(context);
        init(null);
    }

    public CardBaseField(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CardBaseField(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        if (getText() != null) {
            Selection.setSelection(getText(), getText().length());
        }
    }

    /**
     * This is protected constructor, that allows to send parent attributes to this view,
     * this attributes transformed to R.styleable.CoreBaseEditText attrs.
     * Used in {@link EditCard}. Should be used in custom ViewGroups, where is several
     * CardBaseFields are used, to align views correctly and use attributes of CardBaseField
     *
     * @param context         - the Context the view is running in
     * @param attrs           - attributes of parent view
     * @param withParentAttrs - true, if it is a parent attrs
     */
    protected CardBaseField(Context context, AttributeSet attrs, boolean withParentAttrs) {
        super(context, withParentAttrs ? null : attrs);
        setPadding(getPaddingLeft(), 0, getPaddingRight(), 0);
        init(attrs);
    }

    @Override
    public void onProvideAutofillStructure(ViewStructure structure, int flags) {
        super.onProvideAutofillStructure(structure, flags);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // by default autofill is not working for invisible views.
            // in our case it should be possible
            structure.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focusChangeListenerInternal != null) {
            focusChangeListenerInternal.onFocusChange(this, focused);
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    protected final void setFocusChangeListenerInternal(@Nullable OnFocusChangeListener focusChangeListenerInternal) {
        this.focusChangeListenerInternal = focusChangeListenerInternal;
    }

    @Override
    public boolean beforeFormatting(String oldValue, String newValue) {
        return false;
    }

    @Override
    public void onTextFormatted(FormatWatcher formatter, String newFormattedText) {
        notifyTextChanged();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //empty
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (formatWatcher == null || !formatWatcher.isInstalled()) {
            //in other cases it will be called from onTextFormatted
            notifyTextChanged();
        }
        changeColorIfNeeded();
        if (isValid() && length() == getMaxLength()) {
            activateNext();
            sendFilledCorrectEvent(true);
        } else {
            sendFilledCorrectEvent(false);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        //empty
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (formatWatcher == null) {
            initFilters();
            setFormat();
        }
    }

    /**
     * onCreateInputConnection was overridden to
     * to catch delete click and move to previous field
     *
     * @param outAttrs EditorInfo
     * @return wrapped edit text InputConnection
     * @see CardBaseField#activatePrevious()
     */
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new DeleteInputConnection(super.onCreateInputConnection(outAttrs), true);
    }

    public void clear() {
        setText("");
    }

    /**
     * change focus to this EditText
     */
    public void activate() {
        requestFocus();
        post(() -> setSelection(length()));
    }

    /**
     * @return true if the data correct and filled
     */
    public abstract boolean isValid();

    /**
     * Should be overridden in the most of cases.
     *
     * @return true if the field should be marked as not correct
     * @see CardBaseField#errorColor
     */
    public abstract boolean isErrorTextColor();

    /**
     * Moves the focus to the next field with
     *
     * @see CardBaseField#nextViewId
     */
    public final void activateNext() {
        if (nextViewId != NO_ID) {
            ViewGroup parent = (ViewGroup) getParent();
            View view = parent.findViewById(nextViewId);
            if (view instanceof CardBaseField) {
                if (((CardBaseField) view).isValid()) {
                    ((CardBaseField) view).activateNext();
                } else {
                    ((CardBaseField) view).activate();
                }
            } else {
                throw new IllegalArgumentException("Attr core_next_field_id is not correct");
            }
        } else {
            activate();
        }
    }

    /**
     * Moves the focus to the prevField field with previousViewId
     *
     * @see CardBaseField#previousViewId
     * @see CardBaseField.DeleteInputConnection
     */
    public final void activatePrevious() {
        if (previousViewId != NO_ID && isMoveToPrevAllowed) {
            ViewGroup parent = (ViewGroup) getParent();
            View view = parent.findViewById(previousViewId);
            if (view instanceof CardBaseField) {
                ((CardBaseField) view).activate();
            } else {
                throw new IllegalArgumentException("Attr core_prev_field_id is not correct");
            }
        }
    }

    public void enableMoveToPreviousFields(boolean moveToPrevAllowed) {
        isMoveToPrevAllowed = moveToPrevAllowed;
    }

    public void setPreviousViewId(int previousViewId) {
        this.previousViewId = previousViewId;
    }

    public void setNextViewId(int nextViewId) {
        this.nextViewId = nextViewId;
    }

    public void addTextChangedListener(@NonNull CardTextChangedListener cardTextChangedListener) {
        if (textChangedListenerSet == null) {
            textChangedListenerSet = new HashSet<>();
        }
        textChangedListenerSet.add(cardTextChangedListener);
    }

    public void removeTextChangedListener(@NonNull CardTextChangedListener cardTextChangedListener) {
        if (textChangedListenerSet != null) {
            textChangedListenerSet.remove(cardTextChangedListener);
        }
    }

    public void setFieldFilledCorrectListener(@Nullable FieldFilledCorrectListener fieldFilledCorrectListener) {
        this.fieldFilledCorrectListener = fieldFilledCorrectListener;
    }

    public boolean isGoneMode() {
        return isGoneMode;
    }

    public void setGoneMode(boolean goneMode) {
        isGoneMode = goneMode;
    }

    public void setStandardTextColor(int textColor) {
        this.textColor = textColor;
        changeColorIfNeeded();
    }

    public int getErrorColor() {
        return errorColor;
    }

    public void setErrorColor(int errorColor) {
        this.errorColor = errorColor;
        changeColorIfNeeded();
    }

    public int getTextColor() {
        return textColor;
    }

    protected void sendFilledCorrectEvent(boolean isFilledCorrect) {
        if (fieldFilledCorrectListener != null) {
            fieldFilledCorrectListener.onFieldValid(this, getId(), isFilledCorrect);
        }
    }

    protected final void init(AttributeSet attrs) {
        enableMoveToPreviousFields(true);
        setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        setSingleLine(true);
        setBackgroundDrawable(null);
        addTextChangedListener(this);
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CoreBaseEditText,
                0, 0);
        previousViewId = a.getResourceId(R.styleable.CoreBaseEditText_core_prev_field_id, NO_ID);
        nextViewId = a.getResourceId(R.styleable.CoreBaseEditText_core_next_field_id, NO_ID);
        errorColor = a.getColor(R.styleable.CoreBaseEditText_core_error_color, Color.RED);
        textColor = a.getColor(R.styleable.CoreBaseEditText_core_text_color, Color.BLACK);
        a.recycle();
        setTextColor(textColor);
        setInputType(InputType.TYPE_CLASS_PHONE);
    }

    protected void initFilters() {
        setFilters(new InputFilter[]{new InputFilter.LengthFilter(getMaxLength())});
    }

    /**
     * @return maximum length of the field
     */
    protected int getMaxLength() {
        return Integer.MAX_VALUE;
    }

    /**
     * @return the mask for an edit text
     * default value is null, it means that there is no mask
     */
    @Nullable
    protected MaskDescriptor getMask() {
        return null;
    }

    protected void setFormat() {
        MaskDescriptor maskDescriptor = getMask();
        if (maskDescriptor != null) {
            this.maskDescriptor = maskDescriptor;
            formatWatcher = new DescriptorFormatWatcher(maskDescriptor);
            formatWatcher.installOn(this);
            formatWatcher.refreshMask(getText());
            formatWatcher.setCallback(this);
        }
    }

    protected void notifyTextChanged() {
        String currentText = getText() != null ? getText().toString() : "";
        if (lastKnownText == null || !lastKnownText.equals(currentText)) {
            lastKnownText = currentText;
            if (textChangedListenerSet != null) {
                String unformatted;
                if (formatWatcher == null) {
                    unformatted = currentText;
                } else {
                    unformatted = formatWatcher.getMask().toUnformattedString();
                }
                for (CardTextChangedListener cardTextChangedListener : textChangedListenerSet) {
                    cardTextChangedListener.onCardTextChanged(this, getId(), getText().toString(), unformatted);
                }
            }
        }
    }

    protected void changeColorIfNeeded() {
        setTextColor(currentTextColor());
    }

    public int currentTextColor() {
        if (isErrorTextColor()) {
            return errorColor;
        } else {
            return textColor;
        }
    }

    protected final <T extends View> T findParentView(int id) {
        if (id == NO_ID) {
            return null;
        }
        ViewGroup parent = (ViewGroup) getParent();
        return parent.findViewById(id);
    }

    public interface CardTextChangedListener {

        void onCardTextChanged(@NonNull CardBaseField field, int id, @NonNull String formatted, @NonNull String unformatted);
    }

    public interface FieldFilledCorrectListener {

        void onFieldValid(@NonNull CardBaseField field, int id, boolean isFilledCorrect);
    }

    /**
     * Custom class to detect delete click on the keyboard
     * when the field is empty
     */
    private final class DeleteInputConnection extends InputConnectionWrapper {

        private DeleteInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if ((event.getAction() == KeyEvent.ACTION_UP || event.getAction() == KeyEvent.ACTION_DOWN)
                    && event.getKeyCode() == KeyEvent.KEYCODE_DEL && TextUtils.isEmpty(getText())) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (isNeededToIgnorePrev) {
                        isNeededToIgnorePrev = false;
                        return false;
                    }
                    activatePrevious();
                    isNeededToIgnorePrev = true;
                }
                return false;
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // in latest Android, deleteSurroundingText(1, 0) will be called for backspace
            if (beforeLength == 1 && afterLength == 0) {
                // backspace
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }

            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }
}
