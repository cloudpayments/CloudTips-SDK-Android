package ru.cloudtips.edit_card.editcard;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import ru.cloudtips.edit_card.R;
import ru.cloudtips.edit_card.editcard.animation.iconAnim.CardIconAnimation;
import ru.cloudtips.edit_card.editcard.util.CardActionButtonShowRule;
import ru.cloudtips.edit_card.editcard.util.CardActionIconLoader;
import ru.cloudtips.edit_card.editcard.util.CardImageLoader;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static ru.cloudtips.edit_card.editcard.CardNumberEditText.SHORT_MODE_CHARS_COUNT;

/**
 * @author Stanislav Mukhametshin
 * <p>
 * Base ViewGroup, that just creates relation between
 * {@link CardNumberEditText}, {@link DateEditText}, {@link CvcEditText},
 * {@link CardActionButton}.
 * It has several modes
 * {@link EditCard#FLAG_STANDARD} - when all fields are visible.
 * {@link EditCard#FLAG_WITHOUT_DATE} - when date field is invisible.
 * {@link EditCard#FLAG_WITHOUT_CVC} - when cvc field is invisible.
 */
public class EditCard extends ViewGroup {

    private static final int FLAG_STANDARD = 1;
    private static final int FLAG_WITHOUT_DATE = 1 << 1;
    private static final int FLAG_WITHOUT_CVC = 1 << 2;
    private static final int FLAG_AUTOFILL_IMPORTANT = 1 << 3;
    private static final int FLAG_AUTOFILL_ENABLED = 1 << 4;

    private static final int DEFAULT = -1;
    protected ImageView paymentImage;
    protected CardNumberEditText cardNumberEditText;
    protected DateEditText dateEditText;
    protected CvcEditText cvcEditText;
    protected CardActionButton actionButton;
    private int flags;
    private int fontId;
    private float textSize;
    private int hintColor;
    private int iconHeight;
    private int iconWidth;
    private int iconPaddingRight;
    private int iconMarginRight;
    private boolean isMoveToPreviousAllowed;
    private boolean isSavedMode;
    private int resNextIcon = NO_ID;
    private int resActionIcon = NO_ID;
    private int actionButtonWidth = WRAP_CONTENT;
    private int actionButtonHeight = WRAP_CONTENT;
    private int actionButtonMarginLeft = 0;
    private boolean actionButtonInShortMode = false;

    public EditCard(Context context) {
        super(context);
        init(context, null);
    }

    public EditCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public EditCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        EditCardBaseState ss = new EditCardBaseState(superState);
        ss.childrenStates = new SparseArray();
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).saveHierarchyState(ss.childrenStates);
        }
        ss.flags = flags;
        ss.isSavedMode = isSavedMode;
        ss.paymentImageVisibility = paymentImage.getVisibility();
        return ss;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRestoreInstanceState(Parcelable state) {
        EditCardBaseState ss = (EditCardBaseState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).restoreHierarchyState(ss.childrenStates);
        }
        this.flags = ss.flags;
        isSavedMode = ss.isSavedMode;
        paymentImage.setVisibility(ss.paymentImageVisibility);
        if (checkFlag(FLAG_WITHOUT_CVC)) {
            cvcEditText.setGoneMode(true);
            cvcEditText.setVisibility(INVISIBLE);
        }
        if (checkFlag(FLAG_WITHOUT_DATE)) {
            dateEditText.setGoneMode(true);
            cvcEditText.setVisibility(INVISIBLE);
        }
        enableAutofillIfPossible(checkFlag(FLAG_AUTOFILL_IMPORTANT));
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                maxWidth = Math.max(maxWidth,
                        child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                maxHeight = Math.max(maxHeight,
                        child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                childState = combineMeasuredStates(childState, child.getMeasuredState());
            }
        }
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();

        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int relativeBottom = bottom - top;
        int relativeRight = right - left - getPaddingRight();
        int relativeLeft = getPaddingLeft();
        MarginLayoutParams actionParams = (MarginLayoutParams) actionButton.getLayoutParams();
        int actionLeftPosition = relativeRight - actionButton.getMeasuredWidth() - actionParams.leftMargin - actionParams.rightMargin;
        int actionWidth = actionParams.rightMargin + actionParams.leftMargin + actionButton.getMeasuredWidth();
        if (actionButtonInShortMode && actionButton.getState() == CardActionButton.ACTION) {
            relativeRight = relativeRight - actionWidth;
        }
        int iconRight = iconWidth;
        paymentImage.layout(relativeLeft, getAlignCenterTop(paymentImage, relativeBottom),
                relativeLeft + iconRight, getAlignCenterBottom(paymentImage, relativeBottom));
        int cvcLeft = relativeRight - cvcEditText.getMeasuredWidth();
        cvcEditText.layout(cvcLeft, getAlignCenterTop(cvcEditText, relativeBottom),
                relativeRight, getAlignCenterBottom(cvcEditText, relativeBottom));
        int dateWidth = dateEditText.getMeasuredWidth();
        CharSequence formattedNumber = cardNumberEditText.getText();
        if (formattedNumber.length() < SHORT_MODE_CHARS_COUNT) {
            dateEditText.layout((relativeRight - dateWidth) >> 1, getAlignCenterTop(dateEditText, relativeBottom),
                    (relativeRight + dateWidth) >> 1, getAlignCenterBottom(dateEditText, relativeBottom));
        } else {
            float closableTextSize = cardNumberEditText.getPaint().measureText(
                    formattedNumber.toString().substring(0, SHORT_MODE_CHARS_COUNT));
            int paymentRightMargin = ((MarginLayoutParams) paymentImage.getLayoutParams()).rightMargin;
            int dateLeftRange = relativeLeft + iconRight + (int) closableTextSize + paymentRightMargin
                    + dateEditText.getTotalPaddingLeft() + dateEditText.getTotalPaddingRight()
                    + cardNumberEditText.getTotalPaddingLeft() + cardNumberEditText.getTotalPaddingRight() + ((MarginLayoutParams) cardNumberEditText.getLayoutParams()).rightMargin;
            int dateRange = cvcLeft + cvcEditText.getTotalPaddingLeft() - dateLeftRange;
            dateEditText.layout(dateLeftRange + ((dateRange - dateWidth) >> 1),
                    getAlignCenterTop(dateEditText, relativeBottom),
                    dateLeftRange + ((dateRange + dateWidth) >> 1),
                    getAlignCenterBottom(dateEditText, relativeBottom));
        }

        int cardNumberWidth = relativeLeft + cardNumberEditText.getMeasuredWidth();
        if (cardNumberWidth > actionLeftPosition) {
            cardNumberWidth = actionLeftPosition - (int) cardNumberEditText.getTranslationX();
        }
        cardNumberEditText.layout(relativeLeft, getAlignCenterTop(cardNumberEditText, relativeBottom),
                cardNumberWidth, getAlignCenterBottom(cardNumberEditText, relativeBottom));

        actionButton.layout(actionLeftPosition + actionParams.leftMargin, getAlignCenterTop(actionButton, relativeBottom),
                right - left - getPaddingRight(), getAlignCenterBottom(actionButton, relativeBottom));
    }

    private int getAlignCenterTop(View view, int height) {
        return (height - view.getMeasuredHeight()) >> 1;
    }

    private int getAlignCenterBottom(View view, int height) {
        return (height + view.getMeasuredHeight()) >> 1;
    }

    public ImageView getPaymentImage() {
        return paymentImage;
    }

    public CardNumberEditText getCardNumberEditText() {
        return cardNumberEditText;
    }

    public DateEditText getDateEditText() {
        return dateEditText;
    }

    public CvcEditText getCvcEditText() {
        return cvcEditText;
    }

    public CardActionButton getActionButton() {
        return actionButton;
    }

    public void setPaymentIconAnimation(@NonNull CardIconAnimation cardIconAnimation) {
        cardNumberEditText.setCardIconAnimation(cardIconAnimation);
    }

    public void addCardTextChangedListener(@NonNull CardBaseField.CardTextChangedListener cardTextChangedListener) {
        cardNumberEditText.addTextChangedListener(cardTextChangedListener);
        cvcEditText.addTextChangedListener(cardTextChangedListener);
        dateEditText.addTextChangedListener(cardTextChangedListener);
    }

    public void removeCardTextChangedListener(@NonNull CardBaseField.CardTextChangedListener cardTextChangedListener) {
        cardNumberEditText.removeTextChangedListener(cardTextChangedListener);
        cvcEditText.removeTextChangedListener(cardTextChangedListener);
        dateEditText.removeTextChangedListener(cardTextChangedListener);
    }

    public void setRequisites(String cardNumber, @Nullable String date) {
        setRequisites(cardNumber, date, null);
    }

    public void setRequisites(String cardNumber, @Nullable String date, @Nullable String cvc) {
        setRequisites(cardNumber, date, cvc, false);
    }

    public void setRequisites(String cardNumber, @Nullable String date, @Nullable String cvc, boolean isMaskedCardNumber) {
        cardNumberEditText.removeBlockedMode();
        disableSavedCardState();
        if (isMaskedCardNumber) {
            setMaskedCardNumber(cardNumber);
        } else {
            setNumber(cardNumber);
        }
        setDate(date);
        setCvc(cvc);
    }

    public void setSavedRequisites(String cardNumber, boolean isMaskedCardNumber) {
        setSavedRequisites(cardNumber, null, isMaskedCardNumber);
    }

    public void setSavedRequisites(String cardNumber, @Nullable String date, boolean isMaskedCardNumber) {
        setSavedRequisites(cardNumber, date, isMaskedCardNumber, false);
    }

    public void setSavedRequisites(String cardNumber, @Nullable String date, boolean isMaskedCardNumber, boolean isCvcSaved) {
        isSavedMode = true;
        cardNumberEditText.removeBlockedMode();
        cardNumberEditText.enableSavedCardState(cardNumber, isMaskedCardNumber);
        enableAutofillIfPossible(false);
        if (!dateEditText.isGoneMode()) {
            dateEditText.setEnabled(true);
            setDate(date);
            dateEditText.setSavedCardState(!TextUtils.isEmpty(date));
            dateEditText.setVisibility(VISIBLE);
            dateEditText.setAlpha(1f);
        }
        if (!cvcEditText.isGoneMode()) {
            cvcEditText.setVisibility(VISIBLE);
            cvcEditText.setAlpha(1f);
        }
        if (date == null && !dateEditText.isGoneMode()) {
            dateEditText.activate();
        } else if (!cvcEditText.isGoneMode()) {
            cvcEditText.activate();
        }
        cvcEditText.setSavedCardState(isCvcSaved);
    }

    public void disableSavedCardState() {
        disableSavedCardState(true);
    }

    private void disableSavedCardState(boolean isAnimationAllowed) {
        if (isSavedMode) {
            isSavedMode = false;
            cardNumberEditText.disableSavedCardState(isAnimationAllowed);
            dateEditText.setSavedCardState(false);
            cvcEditText.setSavedCardState(false);
        }
    }

    public void setMaskedCardNumber(@NonNull String cardNumber) {
        disableSavedCardState();
        cardNumberEditText.setEditingBlocked(false);
        cardNumberEditText.setMaskedCardNumber(cardNumber);
    }

    public void setMaskedCardNumber(@NonNull String cardNumber, int visibleCharsRight) {
        disableSavedCardState();
        cardNumberEditText.removeBlockedMode();
        cardNumberEditText.setMaskedCardNumber(cardNumber, visibleCharsRight);
    }

    public void setBlockedMaskedCardNumber(@NonNull String maskedNumber) {
        setBlockedMaskedCardNumber(maskedNumber, CardNumberEditText.MASK_DO_NOT_USE);
    }

    public void setBlockedMaskedCardNumber(@NonNull String maskedNumber, int visibleCharsRight) {
        disableSavedCardState(true);
        cardNumberEditText.setBlockedMaskedCardNumber(maskedNumber, visibleCharsRight);
        setDate(null);
        setCvc(null);
        dateEditText.setVisibility(GONE);
        cvcEditText.setVisibility(GONE);
    }

    public void setNumberEditingBlocked(boolean isBlocked) {
        cardNumberEditText.setEditingBlocked(isBlocked);
    }

    public String getNumber() {
        return cardNumberEditText.getNumber();
    }

    public void setNumber(@Nullable String cardNumber) {
        cardNumberEditText.setCardNumber(cardNumber);
    }

    public String getDate() {
        return dateEditText.getText().toString();
    }

    public void setDate(@Nullable String date) {
        dateEditText.setText(date);
    }

    public String getCvc() {
        return cvcEditText.getText().toString();
    }

    public void setCvc(@Nullable String cvc) {
        cvcEditText.setText(cvc);
    }

    public void hideCvc() {
        addFlag(FLAG_WITHOUT_CVC);
        cvcEditText.setGoneMode(true);
        cvcEditText.setVisibility(INVISIBLE);
        enableDisableAnimation();
    }

    public void showCvc() {
        removeFlag(FLAG_WITHOUT_CVC);
        cvcEditText.setGoneMode(false);
        if (cardNumberEditText.isShortMode()) {
            cvcEditText.setAlpha(1f);
            cvcEditText.setVisibility(VISIBLE);
        }
        enableDisableAnimation();
    }

    public void hideDate() {
        addFlag(FLAG_WITHOUT_DATE);
        dateEditText.setGoneMode(true);
        dateEditText.setVisibility(INVISIBLE);
        enableDisableAnimation();
    }

    public void showDate() {
        removeFlag(FLAG_WITHOUT_DATE);
        dateEditText.setGoneMode(false);
        if (cardNumberEditText.isShortMode()) {
            dateEditText.setAlpha(1f);
            dateEditText.setVisibility(VISIBLE);
        }
        enableDisableAnimation();
    }

    public void setNumberAnimationAllowed(boolean isAllowed) {
        cardNumberEditText.setAnimationAllowed(isAllowed);
    }

    public void setShortMode() {
        cardNumberEditText.setShortMode(true);
        if (!dateEditText.isGoneMode()) {
            dateEditText.setVisibility(VISIBLE);
            dateEditText.setAlpha(1f);
        }
        if (!cvcEditText.isGoneMode()) {
            cvcEditText.setVisibility(VISIBLE);
            cvcEditText.setAlpha(1f);
        }
    }

    public void setFont(int fontId) {
        this.fontId = fontId;
        if (fontId != NO_ID) {
            try {
                Typeface typeface = ResourcesCompat.getFont(getContext(), fontId);
                cardNumberEditText.setTypeface(typeface);
                dateEditText.setTypeface(typeface);
                cvcEditText.setTypeface(typeface);
            } catch (Resources.NotFoundException e) {
                // Can happen sometimes, when device has not play services or font are not downloaded yet
                // default font instead of crashing
            }
        }
    }

    public void setHintColor(int hintColor) {
        this.hintColor = hintColor;
        cardNumberEditText.setHintTextColor(hintColor);
        cvcEditText.setHintTextColor(hintColor);
        dateEditText.setHintTextColor(hintColor);
    }

    public void setTextSize(float textSize) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
    }

    public void setTextSize(int unit, float textSize) {
        this.textSize = textSize;
        cardNumberEditText.setTextSize(unit, textSize);
        cvcEditText.setTextSize(unit, textSize);
        dateEditText.setTextSize(unit, textSize);
    }

    public void enableMoveToPreviousFields(boolean moveToPreviousAllowed) {
        isMoveToPreviousAllowed = moveToPreviousAllowed;
        cardNumberEditText.enableMoveToPreviousFields(moveToPreviousAllowed);
        cvcEditText.enableMoveToPreviousFields(moveToPreviousAllowed);
        dateEditText.enableMoveToPreviousFields(moveToPreviousAllowed);
    }

    public void setImageLoader(@Nullable CardImageLoader imageLoader) {
        cardNumberEditText.setCardImageLoader(imageLoader);
    }

    public void setFieldFilledCorrectListener(@Nullable CardBaseField.FieldFilledCorrectListener fieldFilledCorrectListener) {
        cardNumberEditText.setFieldFilledCorrectListener(fieldFilledCorrectListener);
        dateEditText.setFieldFilledCorrectListener(fieldFilledCorrectListener);
        cvcEditText.setFieldFilledCorrectListener(fieldFilledCorrectListener);
    }

    public void expandNumber() {
        cardNumberEditText.expand();
    }

    public boolean isSavedState() {
        return cardNumberEditText.isSavedState();
    }

    public boolean isBlockedMode() {
        return cardNumberEditText.isBlockedMode();
    }

    public void setNextIcon(@DrawableRes int res) {
        actionButton.setNextIcon(res);
    }

    public void setActionIcon(@DrawableRes int res) {
        actionButton.setActionIcon(res);
    }

    public void setActionIcon(@DrawableRes int res, @Nullable CardActionButtonShowRule buttonShowRule) {
        actionButton.setActionIcon(res, buttonShowRule);
    }

    public void setActionIcon(@Nullable Drawable actionIcon) {
        actionButton.setActionIcon(actionIcon);
    }

    public void setActionIconLoader(@Nullable CardActionIconLoader actionIconLoader) {
        this.actionButton.setActionIconLoader(actionIconLoader);
    }

    public void setActionButtonClickListener(@Nullable CardActionButton.ActionButtonClickListener actionButtonClickListener) {
        actionButton.setActionButtonClickListener(actionButtonClickListener);
    }

    public void showActionButtonInShortMode(boolean actionButtonInShortMode) {
        this.actionButtonInShortMode = actionButtonInShortMode;
        actionButton.setActionOnlyInOpenedCard(!actionButtonInShortMode);
        requestLayout();
    }

    public void setAutofillEnabled(boolean enabled) {
        if (enabled) {
            addFlag(FLAG_AUTOFILL_ENABLED);
        } else {
            removeFlag(FLAG_AUTOFILL_ENABLED);
        }
        enableAutofillIfPossible(enabled);
    }

    public boolean isAutoFillEnabled() {
        return checkFlag(FLAG_AUTOFILL_ENABLED);
    }

    private void enableAutofillIfPossible(boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (enable && checkFlag(FLAG_AUTOFILL_ENABLED) && !isSavedState()) {
                addFlag(FLAG_AUTOFILL_IMPORTANT);
                setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_YES);
                cvcEditText.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_YES);
                dateEditText.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_YES);
                cardNumberEditText.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_YES);
            } else {
                removeFlag(FLAG_AUTOFILL_IMPORTANT);
                setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO);
                cvcEditText.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO);
                dateEditText.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO);
                cardNumberEditText.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO);
            }
        }
    }

    public void clear() {
        dateEditText.setSavedCardState(false);
        cvcEditText.setSavedCardState(false);
        dateEditText.clear();
        cardNumberEditText.clear();
        cvcEditText.clear();
        dateEditText.setVisibility(INVISIBLE);
        cvcEditText.setVisibility(INVISIBLE);
    }

    protected final boolean checkFlag(int flag) {
        return (this.flags & flag) == flag;
    }

    protected final void addFlag(int flag) {
        this.flags |= flag;
    }

    protected final void removeFlag(int flag) {
        this.flags &= ~flag;
    }

    protected void configureFields() {
        setFont(fontId);
        if (hintColor != DEFAULT) {
            setHintColor(hintColor);
        }
        if (textSize != DEFAULT) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }

        cardNumberEditText.setPaymentImageId(R.id.core_edit_card_payment);
        if (!checkFlag(FLAG_WITHOUT_CVC)) {
            cardNumberEditText.setCvcId(R.id.core_edit_card_cvc);
            dateEditText.setNextViewId(R.id.core_edit_card_cvc);
            cvcEditText.setPreviousViewId(R.id.core_edit_card_date);
        }
        if (!checkFlag(FLAG_WITHOUT_DATE)) {
            cardNumberEditText.setDateId(R.id.core_edit_card_date);
            cardNumberEditText.setNextViewId(R.id.core_edit_card_date);
            dateEditText.setPreviousViewId(R.id.core_edit_card_number);
        }

        if (checkFlag(FLAG_WITHOUT_DATE) && !checkFlag(FLAG_WITHOUT_CVC)) {
            cardNumberEditText.setNextViewId(R.id.core_edit_card_cvc);
            cvcEditText.setPreviousViewId(R.id.core_edit_card_number);
        }
        enableDisableAnimation();

        addHintsIfNeeded(cardNumberEditText, R.string.cardfield_number_placeholder);
        addHintsIfNeeded(dateEditText, R.string.cardfield_date_placeholder);
        addHintsIfNeeded(cvcEditText, R.string.cardfield_cvc_placeholder);
        enableMoveToPreviousFields(isMoveToPreviousAllowed);

        actionButton.setBackgroundDrawable(null);
        if (resNextIcon != NO_ID) {
            setNextIcon(resNextIcon);
        } else {
            setNextIcon(R.drawable.core_ic_editcard_next);
        }
        if (resActionIcon != NO_ID) {
            setActionIcon(resActionIcon);
        }
        cardNumberEditText.addTextChangedListener(numberChangedListener);
    }

    protected void createViews(Context context, AttributeSet attrs) {
        paymentImage = createPaymentImageView(context);
        paymentImage.setId(R.id.core_edit_card_payment);
        paymentImage.setVisibility(INVISIBLE);
        MarginLayoutParams imageParams = setLayoutParams(paymentImage, iconWidth, iconHeight);
        imageParams.rightMargin = iconMarginRight;
        paymentImage.setPadding(0, 0, iconPaddingRight, 0);
        addView(paymentImage);

        cvcEditText = new CvcEditText(context, attrs, true);
        cvcEditText.setId(R.id.core_edit_card_cvc);
        if (checkFlag(FLAG_WITHOUT_CVC)) {
            cvcEditText.setGoneMode(true);
        }
        cvcEditText.setVisibility(INVISIBLE);
        setLayoutParams(cvcEditText, WRAP_CONTENT, MATCH_PARENT);
        addView(cvcEditText);

        dateEditText = new DateEditText(context, attrs, true);
        dateEditText.setId(R.id.core_edit_card_date);
        dateEditText.setVisibility(INVISIBLE);
        setLayoutParams(dateEditText, WRAP_CONTENT, MATCH_PARENT);
        addView(dateEditText);
        if (checkFlag(FLAG_WITHOUT_DATE)) {
            dateEditText.setGoneMode(true);
        }

        actionButton = new CardActionButton(context);
        MarginLayoutParams actionParams = setLayoutParams(actionButton, actionButtonWidth, actionButtonHeight);
        actionParams.leftMargin = actionButtonMarginLeft;
        actionButton.setId(R.id.core_edit_card_action_button);

        cardNumberEditText = new CardNumberEditText(context, attrs, true);
        actionButton.init(cardNumberEditText);
        actionButton.setActionOnlyInOpenedCard(!actionButtonInShortMode);

        cardNumberEditText.setId(R.id.core_edit_card_number);
        setLayoutParams(cardNumberEditText, MATCH_PARENT, MATCH_PARENT);
        addView(cardNumberEditText);
        addView(actionButton);
    }

    protected ImageView createPaymentImageView(Context context) {
        return new ImageView(context);
    }

    private void init(Context context, AttributeSet attrs) {
        int defaultIconSize = getResources().getDimensionPixelSize(R.dimen.core_payment_icon_size);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.CoreEditCard, 0, 0);
        flags = a.getInteger(R.styleable.CoreEditCard_core_mode, FLAG_STANDARD);
        fontId = a.getResourceId(R.styleable.CoreEditCard_core_font, NO_ID);
        textSize = a.getDimensionPixelSize(R.styleable.CoreEditCard_core_text_size, DEFAULT);
        hintColor = a.getColor(R.styleable.CoreEditCard_core_hint_color, DEFAULT);
        isMoveToPreviousAllowed = a.getBoolean(R.styleable.CoreEditCard_core_move_to_prev, true);
        iconHeight = a.getDimensionPixelSize(R.styleable.CoreEditCard_core_icon_height, defaultIconSize);
        iconWidth = a.getDimensionPixelSize(R.styleable.CoreEditCard_core_icon_width, defaultIconSize);
        iconPaddingRight = a.getDimensionPixelSize(R.styleable.CoreEditCard_core_icon_padding_right, 0);
        iconMarginRight = a.getDimensionPixelSize(R.styleable.CoreEditCard_core_icon_margin_right, 0);
        resNextIcon = a.getResourceId(R.styleable.CoreEditCard_core_card_next_icon, NO_ID);
        resActionIcon = a.getResourceId(R.styleable.CoreEditCard_core_card_action_icon, NO_ID);
        actionButtonHeight = a.getDimensionPixelSize(R.styleable.CoreEditCard_core_action_button_height, WRAP_CONTENT);
        actionButtonWidth = a.getDimensionPixelSize(R.styleable.CoreEditCard_core_action_button_width, WRAP_CONTENT);
        actionButtonMarginLeft = a.getDimensionPixelSize(R.styleable.CoreEditCard_core_action_button_margin_left, 0);
        actionButtonInShortMode = a.getBoolean(R.styleable.CoreEditCard_core_action_button_in_short_mode, false);
        a.recycle();
        createViews(context, attrs);
        configureFields();
        addFlag(FLAG_AUTOFILL_ENABLED);
        enableAutofillIfPossible(false);

        OnFocusChangeListener onFocusChangeListener = (v, hasFocus) -> {
            hideKeyboardIfNeeded();
            changeAutoFill();
        };
        cardNumberEditText.setFocusChangeListenerInternal(onFocusChangeListener);
        cvcEditText.setFocusChangeListenerInternal(onFocusChangeListener);
        dateEditText.setFocusChangeListenerInternal(onFocusChangeListener);
    }

    private void hideKeyboardIfNeeded() {
        if (!cardNumberEditText.hasFocus() && !dateEditText.hasFocus() && !cvcEditText.hasFocus()) {
            postDelayed(() -> {
                if (getContext() instanceof Activity) {
                    View focusedView = ((Activity) getContext()).getCurrentFocus();
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && focusedView == null) {
                        imm.hideSoftInputFromWindow(getWindowToken(), 0);
                    }
                }
            }, 200);
        }
    }

    private void changeAutoFill() {
        if (cardNumberEditText.hasFocus() || dateEditText.hasFocus() || cvcEditText.hasFocus()) {
            enableAutofillIfPossible(true);
        } else {
            enableAutofillIfPossible(false);
        }
    }

    private void enableDisableAnimation() {
        if (checkFlag(FLAG_WITHOUT_DATE) && checkFlag(FLAG_WITHOUT_CVC)) {
            cardNumberEditText.expand();
            cardNumberEditText.setAnimationAllowed(false);
        } else {
            cardNumberEditText.setAnimationAllowed(true);
        }
    }

    private void addHintsIfNeeded(EditText editText, @StringRes int stringRes) {
        if (editText != null && TextUtils.isEmpty(editText.getHint())) {
            editText.setHint(stringRes);
        }
    }

    private MarginLayoutParams setLayoutParams(View view, int width, int height) {
        MarginLayoutParams layoutParams = new MarginLayoutParams(width, height);
        view.setLayoutParams(layoutParams);
        return layoutParams;
    }

    protected final CardBaseField.CardTextChangedListener numberChangedListener = new CardBaseField.CardTextChangedListener() {
        @Override
        public void onCardTextChanged(@NonNull CardBaseField field, int id, @NonNull String formatted, @NonNull String unformatted) {
            if (TextUtils.isEmpty(formatted)) {
                dateEditText.clear();
                cvcEditText.clear();
                changeAutoFill();
            }
        }
    };

    protected static class EditCardBaseState extends BaseSavedState {

        public static final ClassLoaderCreator<EditCardBaseState> CREATOR
                = new ClassLoaderCreator<EditCardBaseState>() {
            @Override
            public EditCardBaseState createFromParcel(Parcel source, ClassLoader loader) {
                return new EditCardBaseState(source, loader);
            }

            @Override
            public EditCardBaseState createFromParcel(Parcel source) {
                return createFromParcel(source, null);
            }

            public EditCardBaseState[] newArray(int size) {
                return new EditCardBaseState[size];
            }
        };
        int flags;
        SparseArray childrenStates;
        int paymentImageVisibility;
        boolean isSavedMode;

        EditCardBaseState(Parcelable superState) {
            super(superState);
        }

        private EditCardBaseState(Parcel in, ClassLoader classLoader) {
            super(in);
            childrenStates = in.readSparseArray(classLoader);
            flags = in.readInt();
            isSavedMode = in.readInt() == 1;
            paymentImageVisibility = in.readInt();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeSparseArray(childrenStates);
            out.writeInt(this.flags);
            out.writeInt(isSavedMode ? 1 : 0);
            out.writeInt(paymentImageVisibility);
        }
    }
}
