package ru.cloudtips.edit_card.editcard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ru.cloudtips.edit_card.R;
import ru.tinkoff.decoro.MaskDescriptor;
import ru.tinkoff.decoro.watchers.FormatWatcher;
import ru.cloudtips.edit_card.editcard.animation.CardAnimation;
import ru.cloudtips.edit_card.editcard.animation.CardCloseAnimation;
import ru.cloudtips.edit_card.editcard.animation.CardOpenAnimation;
import ru.cloudtips.edit_card.editcard.animation.iconAnim.CardIconAnimation;
import ru.cloudtips.edit_card.editcard.animation.iconAnim.CardIconAnimationImpl;
import ru.cloudtips.edit_card.editcard.mask.CardMaskUtil;
import ru.cloudtips.edit_card.editcard.mask.MaskCreator;
import ru.cloudtips.edit_card.editcard.mask.MaskCreatorImpl;
import ru.cloudtips.edit_card.editcard.util.CardImageLoader;
import ru.cloudtips.edit_card.editcard.util.CardValidator;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * @author Stanislav Mukhametshin
 * The field for a card number.
 * The whole animations are controlled from this class.
 * If you need a field only for a card number and you do not need cvc and date.
 * You should use this class, not a {@link EditCard}
 */
public class CardNumberEditText extends CardBaseField {

    public static final int SHORT_MODE_CHARS_COUNT = 4;

    // used when we are trying to show the number that already have a mask.
    public static final int MASK_DO_NOT_USE = -1;

    private static final int DEFAULT_MIN_CHECK_LENGTH = 4;
    private static final int FLAG_CARD_LOGO_SHOWN = 1;
    private static final int FLAG_FULL_CARD_NUMBER = 1 << 1;
    private static final int FLAG_IN_ANIMATION = 1 << 5;
    private static final int FLAG_LOGO_IN_ANIMATION = 1 << 6;
    private static final int FLAG_CARD_OPENED = 1 << 7;
    private static final int FLAG_MASKED_MODE = 1 << 8;
    private static final int FLAG_BLOCKED_MODE = 1 << 9;
    private static final int FLAG_SAVED_STATE = (1 << 10) | FLAG_BLOCKED_MODE;

    private boolean isShortMode;
    private boolean isMaskChanged;
    private boolean isAnimationAllowed;
    private boolean isLogoEmpty;
    private boolean isSetManually = false;
    private boolean isLayoutInitialized = false;
    private boolean isOnPreDrawProceed;
    private boolean isWidthInitialized = false;
    private float animationFactor = 0f;
    private int cursorPosition;
    private String start = "";
    private String lastKnownText;
    private String maskedNumber;
    private float closableRangeTextSize;
    private int initialWidth;
    private int textLength;
    private int cvcId;
    private int dateId;
    private int paymentImageId;
    private int flags;
    private int currentMinHeight;
    private CardAnimation openAnimation;
    private CardAnimation closeAnimation;
    private CardIconAnimation cardIconAnimation;
    @Nullable
    private CardImageLoader cardImageLoader;
    private MaskCreator maskCreator;
    private Animator iconAnimator;
    private Animator cardAnimator;
    private OnFocusChangeListener customOnFocusChangedListener;
    private ShortModeListener shortModeListener;
    @Nullable
    private OnCardViewChanged onCardViewChanged = null;
    boolean ignoreFormatting = false;

    public CardNumberEditText(Context context) {
        super(context);
        initCard(null);
    }

    public CardNumberEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCard(attrs);
    }

    public CardNumberEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCard(attrs);
    }

    protected CardNumberEditText(Context context, AttributeSet attrs, boolean withParentAttrs) {
        super(context, attrs, withParentAttrs);
        initCard(attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!TextUtils.isEmpty(getText())) {
            if (isShortMode) {
                removeFlag(FLAG_CARD_OPENED);
                closeAnimation.endState(this);
            }
        }
        onCardChanged();
        updateIconIfNeeded(getNumber());
        if (!isLogoEmpty && paymentImageId != NO_ID) {
            ImageView paymentImage = findParentView(paymentImageId);
            if (paymentImage != null) {
                addFlag(FLAG_CARD_LOGO_SHOWN);
                doOnLayout(paymentImage, () -> cardIconAnimation.showLogoWithoutAnimation(this, findParentView(paymentImageId)));
            }
        }
        isSetManually = false;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.isShortMode = isShortMode;
        ss.isAnimationAllowed = isAnimationAllowed;
        ss.animationFactor = animationFactor;
        ss.cursorPosition = cursorPosition;
        ss.start = start;
        ss.lastKnownText = lastKnownText;
        ss.closableRangeTextSize = closableRangeTextSize;
        ss.initialWidth = initialWidth;
        ss.textLength = textLength;
        ss.cvcId = cvcId;
        ss.dateId = dateId;
        ss.paymentImageId = paymentImageId;
        ss.flags = flags;
        ss.maskedNumber = maskedNumber;
        ss.isSetManually = isSetManually;
        ss.ignoreFormatting = ignoreFormatting;
        ss.translationX = getTranslationX();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        flags = ss.flags;
        isAnimationAllowed = ss.isAnimationAllowed;
        isShortMode = ss.isShortMode;
        animationFactor = ss.animationFactor;
        cursorPosition = ss.cursorPosition;
        start = ss.start;
        lastKnownText = ss.lastKnownText;
        closableRangeTextSize = ss.closableRangeTextSize;
        initialWidth = ss.initialWidth;
        textLength = ss.textLength;
        cvcId = ss.cvcId;
        dateId = ss.dateId;
        paymentImageId = ss.paymentImageId;
        maskedNumber = ss.maskedNumber;
        isSetManually = ss.isSetManually;
        ignoreFormatting = ss.ignoreFormatting;
        enableEditing(!checkFlag(FLAG_BLOCKED_MODE));
        if (!isShortMode && initialWidth != 0) {
            getLayoutParams().width = initialWidth;
        }
        setTranslationX(ss.translationX);
    }

    @Override
    public boolean bringPointIntoView(int offset) {
        return !isShortMode && super.bringPointIntoView(offset);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (isShortMode) {
                    return true;
                }
            }
            //falls through
            case MotionEvent.ACTION_UP: {
                if (isAnimationAllowed && isShortMode) {
                    doAnimation(openAnimation);
                    break;
                }
            }
        }
        return !isShortMode && super.onTouchEvent(event);
    }

    @Override
    public boolean isValid() {
        return CardValidator.validateNumber(getNumber()) || checkFlag(FLAG_MASKED_MODE);
    }

    @Override
    public boolean isErrorTextColor() {
        return CardValidator.validateCardNumberLength(getNumber()) && !isValid();
    }

    @Override
    public boolean beforeFormatting(String oldValue, String newValue) {
        cursorPosition = getSelectionEnd();
        isMaskChanged = false;
        if (newValue.length() >= DEFAULT_MIN_CHECK_LENGTH) {
            String currentStart = newValue;
            if (!start.equals(currentStart) || checkFlag(FLAG_MASKED_MODE)) {
                start = currentStart;
                MaskDescriptor newDescriptor = maskCreator.createMaskDescriptor(getNormalizedCardNumber(newValue), maskedNumber);
                if (newDescriptor != null && !newDescriptor.equals(maskDescriptor)) {
                    isMaskChanged = true;
                    newDescriptor.setInitialValue(getNormalizedCardNumber(newValue));
                    formatWatcher.changeMask(newDescriptor);
                    maskDescriptor = newDescriptor;
                }
            }
        }
        return ignoreFormatting;
    }

    @Override
    public void onTextFormatted(FormatWatcher formatter, String newFormattedText) {
        super.onTextFormatted(formatter, newFormattedText);
        String number = getNumber();
        boolean isValid = isValid();
        boolean isLimited = CardValidator.isDefaultNumberFormat(number) || CardValidator.isMaxSymbols(number);
        final boolean isFullCorrectDefaultNumber = isValid && isLimited;
        final boolean isNotStandardValid = CardValidator.isNotStandardValidNumber(isValid, number);
        updateIconIfNeeded(number);
        if (isFullCorrectDefaultNumber || isNotStandardValid ) {
            addFlag(FLAG_FULL_CARD_NUMBER);
            post(this::doAnimationOrActivateNext);
        } else {
            removeFlag(FLAG_FULL_CARD_NUMBER);
            onCardChanged();
        }
        if (isMaskChanged) {
            setSelection(length());
        }
        sendFilledCorrectEvent(isValid);

        if (!isFullCorrectDefaultNumber || checkFlag(FLAG_SAVED_STATE)) {
            post(this::animateIconIfNeeded);
        }
        isSetManually = false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        super.beforeTextChanged(s, start, count, after);
        closableRangeTextSize = getPaint().measureText(
                lastKnownText.substring(0, Math.max(0, textLength - SHORT_MODE_CHARS_COUNT)));
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        changeColorIfNeeded();
        if (isValid() && length() == getMaxLength()) {
            activateNext();
        }
        if (s != null) {
            lastKnownText = s.toString();
            textLength = lastKnownText.length();
            closableRangeTextSize = getPaint().measureText(
                    lastKnownText.substring(0, Math.max(0, textLength - SHORT_MODE_CHARS_COUNT)));
        }
        if (checkFlag(FLAG_MASKED_MODE) && !isSetManually) {
            if (s != null && CardMaskUtil.compareMaskNumbers(maskedNumber, s.toString())) {
                return;
            }
            ignoreFormatting = true;
            post(this::clear);
        }
    }

    @Override
    protected MaskDescriptor getMask() {
        return maskCreator.createMaskDescriptor(getNormalizedCardNumber(getNumber()), maskedNumber);
    }

    @Override
    public boolean onPreDraw() {
        isOnPreDrawProceed = super.onPreDraw();
        if (!isLayoutInitialized && !isShortMode) {
            isLayoutInitialized = true;
            scrollMax();
        }
        return isOnPreDrawProceed;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        getPaint().setColor(getTextColors().getDefaultColor());
        if (isShortMode) {
            canvas.drawText(lastKnownText, Math.max(0, textLength - SHORT_MODE_CHARS_COUNT),
                    textLength, getTotalPaddingLeft(), getBaseline(), getPaint());
        } else {
            canvas.save();
            canvas.translate(-(closableRangeTextSize - getScrollX()) * animationFactor, 0);
            super.onDraw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (customOnFocusChangedListener != null) {
            customOnFocusChangedListener.onFocusChange(this, focused);
        }
    }

    @Override
    protected void initFilters() {
        //empty
    }

    @Override
    protected int getMaxLength() {
        // max length = slots + 1 symbol (need for next char input or changing mask)
        if (maskDescriptor != null && maskDescriptor.getSlots() != null) {
            return maskDescriptor.getSlots().length + 1;
        } else
            return CardValidator.DEFAULT_MAX_SYMBOL_LENGTH + 1;
    }

    @Override
    protected void setFormat() {
        super.setFormat();
        if (!TextUtils.isEmpty(getText())) {
            notifyTextChanged();
        }
    }

    public void enableSavedCardState(String number, boolean isMaskedCardNumber) {
        setAnimationAllowed(false);
        addFlag(FLAG_SAVED_STATE);
        if (isMaskedCardNumber) {
            setMaskedCardNumber(number);
        } else {
            removeFlag(FLAG_MASKED_MODE);
            setTextInner(number);
        }
        if (checkFlag(FLAG_CARD_OPENED)) {
            setShortMode(true);
            closeAnimation.endState(this);
            removeFlag(FLAG_CARD_OPENED);
        }
        enableEditing(false);
    }

    public void disableSavedCardState(boolean isAnimationAllowed) {
        removeFlag(FLAG_SAVED_STATE);
        enableEditing(true);
        setAnimationAllowed(isAnimationAllowed);
    }

    private void setTextInner(String text) {
        setText(text);
        if (!isShortMode) {
            scrollMax();
        }
    }

    public void scrollMax() {
        if (getLayout() != null) {
            int scrollMax = (int) (getLayout().getLineWidth(0) - getWidth() + getPaddingLeft() + getPaddingRight());
            setScrollX(scrollMax < 0 ? 0 : scrollMax);
        }
    }

    public void postScrollMax() {
        post(this::scrollMax);
    }

    public void setMaskCreator(@NonNull MaskCreator maskCreator) {
        this.maskCreator = maskCreator;
    }

    public void setOpenAnimation(@NonNull CardAnimation openAnimation) {
        this.openAnimation = openAnimation;
    }

    public void setCloseAnimation(@NonNull CardAnimation closeAnimation) {
        this.closeAnimation = closeAnimation;
    }

    public void setShortModeListener(@Nullable ShortModeListener shortModeListener) {
        this.shortModeListener = shortModeListener;
    }

    public float getAnimationFactor() {
        return animationFactor;
    }

    public void setAnimationFactor(float animationFactor) {
        this.animationFactor = animationFactor;
        invalidate();
    }

    public OnFocusChangeListener getCustomOnFocusChangedListener() {
        return customOnFocusChangedListener;
    }

    public void setCustomOnFocusChangedListener(@Nullable OnFocusChangeListener customOnFocusChangedListener) {
        this.customOnFocusChangedListener = customOnFocusChangedListener;
    }

    public boolean isShortMode() {
        return isShortMode;
    }

    public void setShortMode(boolean isShortMode) {
        this.isShortMode = isShortMode;
        if (isShortMode) {
            clearFocus();
            setScrollX(0);
        }
        setAnimationFactor(isShortMode ? 1f : 0f);
        if (shortModeListener != null) {
            shortModeListener.onCardShort(isShortMode);
        }
    }

    @NonNull
    public String getNumber() {
        if (checkFlag(FLAG_MASKED_MODE)) {
            return maskedNumber;
        }
        if (formatWatcher == null) {
            return getText().toString();
        }
        return formatWatcher.getMask().toUnformattedString();
    }

    public String getMaskedNumber() {
        return maskedNumber;
    }

    public void setCardIconAnimation(@NonNull CardIconAnimation cardIconAnimation) {
        this.cardIconAnimation = cardIconAnimation;
    }

    public void setCardImageLoader(@Nullable CardImageLoader cardImageLoader) {
        this.cardImageLoader = cardImageLoader;
    }

    private void initMinimumHeight(@Nullable Drawable compoundDrawable) {
        if (compoundDrawable == null) {
            return;
        }
        int newMin = compoundDrawable.getIntrinsicHeight() + getCompoundPaddingTop() + getCompoundPaddingBottom();
        if (newMin > currentMinHeight) {
            currentMinHeight = newMin;
            setMinHeight(newMin);
        }
    }

    public void setCvcId(@IdRes int cvcId) {
        this.cvcId = cvcId;
    }

    public void setDateId(@IdRes int dateId) {
        this.dateId = dateId;
    }

    public void setPaymentImageId(@IdRes int paymentImageId) {
        this.paymentImageId = paymentImageId;
    }

    public void setAnimationAllowed(boolean animationAllowed) {
        if (isAnimationAllowed == animationAllowed) {
            return;
        }
        isAnimationAllowed = animationAllowed;
        if (checkFlag(FLAG_FULL_CARD_NUMBER) && checkFlag(FLAG_CARD_OPENED) && isAnimationAllowed) {
            doAnimation(closeAnimation);
        } else if (!isAnimationAllowed) {
            onCardChanged();
        }
    }

    public boolean isAnimationAllowed() {
        return isAnimationAllowed;
    }

    public void expand() {
        if (isShortMode) {
            doAnimation(openAnimation);
        }
    }

    public void initInitialWidth() {
        if (!isWidthInitialized) {
            isWidthInitialized = true;
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            if (layoutParams.width == MATCH_PARENT || layoutParams.width == WRAP_CONTENT) {
                this.initialWidth = layoutParams.width;
                return;
            }
            this.initialWidth = getWidth();
        }
    }

    public int getInitialWidth() {
        return initialWidth;
    }

    public void setCardNumber(String number) {
        isSetManually = true;
        removeFlag(FLAG_MASKED_MODE);
        maskedNumber = null;
        if (formatWatcher != null) {
            formatWatcher.refreshMask("");
        }
        setTextInner(number);
    }

    public void setMaskedCardNumber(@NonNull String maskedNumber) {
        setMaskedCardNumber(maskedNumber, MASK_DO_NOT_USE);
    }

    public void setMaskedCardNumber(@NonNull String maskedNumber, int visibleCharsRight) {
        isSetManually = true;
        addFlag(FLAG_MASKED_MODE);
        this.maskedNumber = maskedNumber;
        if (visibleCharsRight != MASK_DO_NOT_USE) {
            int length = maskedNumber.length();
            String dataToShow = new String(new char[length - visibleCharsRight]).replace('\0', CardMaskUtil.MASK_CHAR);
            dataToShow += maskedNumber.substring(length - visibleCharsRight, length);
            setTextInner(dataToShow);
            return;
        }
        setTextInner(maskedNumber);
    }

    public void setBlockedMaskedCardNumber(@NonNull String maskedNumber) {
        setBlockedMaskedCardNumber(maskedNumber, MASK_DO_NOT_USE);
    }

    public void setBlockedMaskedCardNumber(@NonNull String maskedNumber, int visibleCharsRight) {
        isSetManually = true;
        addFlag(FLAG_BLOCKED_MODE);
        showWithoutAnimation();
        setMaskedCardNumber(maskedNumber, visibleCharsRight);
        enableEditing(false);
    }

    public void showWithoutAnimation() {
        if (!checkFlag(FLAG_CARD_OPENED)) {
            ViewGroup.LayoutParams param = getLayoutParams();
            setShortMode(false);
            param.width = getInitialWidth();
            setLayoutParams(param);
            openAnimation.endState(this);
            postScrollMax();
            addFlag(FLAG_CARD_OPENED);
        }
    }

    public void setEditingBlocked(boolean isBlocked) {
        if (isBlocked) {
            addFlag(FLAG_BLOCKED_MODE);
        } else {
            removeFlag(FLAG_BLOCKED_MODE);
        }
        enableEditing(!isBlocked);
    }

    public void removeBlockedMode() {
        if (checkFlag(FLAG_BLOCKED_MODE)) {
            enableEditing(true);
            removeFlag(FLAG_BLOCKED_MODE);
        }
    }

    public boolean isBlockedMode() {
        return checkFlag(FLAG_BLOCKED_MODE);
    }

    public boolean isSavedState() {
        return checkFlag(FLAG_SAVED_STATE);
    }

    public void enableEditing(boolean isEnabled) {
        setCursorVisible(isEnabled);
        setFocusableInTouchMode(isEnabled);
        setFocusable(isEnabled);
    }

    @Override
    public void clear() {
        removeFlag(FLAG_MASKED_MODE);
        removeFlag(FLAG_SAVED_STATE);
        if (cardAnimator != null) {
            cardAnimator.cancel();
        }
        ignoreFormatting = false;
        maskedNumber = null;
        setEditingBlocked(false);
        showWithoutAnimation();
        setTextInner("");
        onCardChanged();
        isLogoEmpty = true;
        animateIconIfNeeded();
        isAnimationAllowed = true;
    }

    private String getNormalizedCardNumber(String cardNumber) {
        return cardNumber.replaceAll("\\s+", "");
    }

    private void initCard(AttributeSet attrs) {
        isLogoEmpty = true;
        openAnimation = new CardOpenAnimation();
        closeAnimation = new CardCloseAnimation();
        cardIconAnimation = new CardIconAnimationImpl();
        maskCreator = new MaskCreatorImpl();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(AUTOFILL_HINT_CREDIT_CARD_NUMBER);
        }

        flags = 0;
        addFlag(FLAG_CARD_OPENED);
        setCustomOnFocusChangedListener((v, hasFocus) -> {
            if (hasFocus && isAnimationAllowed && isShortMode) {
                doAnimation(openAnimation);
            }
        });
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CoreCardNumberEditText,
                0, 0);
        cvcId = a.getResourceId(R.styleable.CoreCardNumberEditText_core_cvc_id, NO_ID);
        dateId = a.getResourceId(R.styleable.CoreCardNumberEditText_core_date_id, NO_ID);
        paymentImageId = a.getResourceId(R.styleable.CoreCardNumberEditText_core_card_icon_id, NO_ID);
        isAnimationAllowed = a.getBoolean(R.styleable.CoreCardNumberEditText_core_animation_enabled, false);

        a.recycle();
    }

    private void doAnimation(final CardAnimation cardAnimation) {
        if (checkFlag(FLAG_BLOCKED_MODE)) {
            animateIconIfNeeded();
            onCardChanged();
            return;
        }
        if (!isAnimationAllowed) {
            return;
        }
        if ((cardAnimation == openAnimation && checkFlag(FLAG_CARD_OPENED))
                || (cardAnimation == closeAnimation && (!checkFlag(FLAG_CARD_OPENED)))) {
            onCardChanged();
            return;
        }
        if (cardAnimation == openAnimation) {
            addFlag(FLAG_CARD_OPENED);
        } else if (cardAnimation == closeAnimation) {
            removeFlag(FLAG_CARD_OPENED);
        }
        if (checkFlag(FLAG_IN_ANIMATION) && cardAnimator != null) {
            cardAnimator.cancel();
        }
        onCardAnimationStarted();
        CvcEditText cvcEditText = (cvcId == NO_ID) ? null : (CvcEditText) findParentView(cvcId);
        DateEditText dateEditText = (dateId == NO_ID) ? null : (DateEditText) findParentView(dateId);
        cardAnimator = cardAnimation.createFullAnimation(this, dateEditText, cvcEditText);

        cardAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                removeFlag(FLAG_IN_ANIMATION);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                removeFlag(FLAG_IN_ANIMATION);
            }
        });
        addFlag(FLAG_IN_ANIMATION);

        setSelection(length());

        if (!checkFlag(FLAG_CARD_LOGO_SHOWN) && paymentImageId != NO_ID) {
            Animator iconAnimator = createIconAnimIfNeeded();
            if (iconAnimator != null && isShowLogoAnimationPossible()) {
                prepareIconAnimation(iconAnimator, true);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playSequentially(cardAnimator, iconAnimator);
                animatorSet.start();
                return;
            }
        }
        cardAnimator.start();
    }

    private boolean checkFlag(int flag) {
        return (this.flags & flag) == flag;
    }

    private void addFlag(int flag) {
        this.flags |= flag;
    }

    private void removeFlag(int flag) {
        this.flags &= ~flag;
    }

    protected void doAnimationOrActivateNext() {
        if (!isAnimationAllowed) {
            animateIconIfNeeded();
            activateNext();
            onCardChanged();
        } else {
            doAnimation(closeAnimation);
        }
    }

    protected void setOnCardViewChanged(@Nullable OnCardViewChanged onCardViewChanged) {
        this.onCardViewChanged = onCardViewChanged;
    }

    private void updateIconIfNeeded(@NonNull String number) {
        if (paymentImageId != NO_ID && cardImageLoader != null) {
            ImageView numberIcon = findParentView(paymentImageId);
            if (numberIcon != null) {
                isLogoEmpty = !cardImageLoader.loadImage(numberIcon, number);
            }
        }
    }

    @Nullable
    private Animator createIconAnimIfNeeded() {
        if (paymentImageId == NO_ID) {
            return null;
        }
        Animator animator = null;
        if (isShowLogoAnimationPossible()) {
            animator = cardIconAnimation.createShowLogoAnimation(this, (ImageView) findParentView(paymentImageId));
        } else if (isLogoEmpty && checkFlag(FLAG_CARD_LOGO_SHOWN)) {
            animator = cardIconAnimation.createHideLogoAnimation(this, (ImageView) findParentView(paymentImageId));
        }
        return animator;
    }

    private boolean isShowLogoAnimationPossible() {
        return !checkFlag(FLAG_CARD_LOGO_SHOWN) && !isLogoEmpty;
    }

    private void prepareIconAnimation(Animator animator, boolean isOpenAnimation) {
        if (animator != null) {
            cancelIconAnimIfNeeded();
            iconAnimator = animator;
            if (isOpenAnimation) {
                addFlag(FLAG_CARD_LOGO_SHOWN);
            } else {
                removeFlag(FLAG_CARD_LOGO_SHOWN);
            }
            iconAnimator.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    addFlag(FLAG_LOGO_IN_ANIMATION);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    removeFlag(FLAG_LOGO_IN_ANIMATION);
                }
            });
        }
    }

    private void animateIconIfNeeded() {
        Animator animator = createIconAnimIfNeeded();
        if (animator != null) {
            prepareIconAnimation(animator, isShowLogoAnimationPossible());
            iconAnimator.start();
        }
    }

    private void cancelIconAnimIfNeeded() {
        if (iconAnimator != null) {
            iconAnimator.cancel();
        }
    }

    public static final Property<CardNumberEditText, Float> ANIMATOR_FACTOR = new Property<CardNumberEditText, Float>(Float.class, "animator_factor") {

        @Override
        public Float get(CardNumberEditText object) {
            return object.getAnimationFactor();
        }

        @Override
        public void set(CardNumberEditText object, Float value) {
            object.setAnimationFactor(value);
        }
    };

    public void onCardChanged() {
        if (onCardViewChanged != null) {
            onCardViewChanged.onCardChanged();
        }
    }

    private void onCardAnimationStarted() {
        if (onCardViewChanged != null) {
            onCardViewChanged.onCardAnimationStarted();
        }
    }

    public interface ShortModeListener {

        void onCardShort(boolean isShort);
    }

    protected interface OnCardViewChanged {

        void onCardChanged();

        void onCardAnimationStarted();
    }

    static class SavedState extends BaseSavedState {

        boolean isShortMode;
        boolean isAnimationAllowed;
        float animationFactor = 0f;
        int cursorPosition;
        String start = "";
        String lastKnownText;
        String maskedNumber;
        float closableRangeTextSize;
        int initialWidth;
        int textLength;
        int cvcId;
        int dateId;
        int paymentImageId;
        int flags;
        boolean isSetManually;
        boolean ignoreFormatting;
        float translationX;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            start = in.readString();
            lastKnownText = in.readString();
            closableRangeTextSize = in.readFloat();
            animationFactor = in.readFloat();
            flags = in.readInt();
            cursorPosition = in.readInt();
            initialWidth = in.readInt();
            textLength = in.readInt();
            cvcId = in.readInt();
            dateId = in.readInt();
            paymentImageId = in.readInt();
            isShortMode = in.readInt() == 1;
            isAnimationAllowed = in.readInt() == 1;
            maskedNumber = in.readString();
            isSetManually = in.readInt() == 1;
            ignoreFormatting = in.readInt() == 1;
            translationX = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(start);
            out.writeString(lastKnownText);
            out.writeFloat(closableRangeTextSize);
            out.writeFloat(animationFactor);
            out.writeInt(this.flags);
            out.writeInt(cursorPosition);
            out.writeInt(initialWidth);
            out.writeInt(textLength);
            out.writeInt(cvcId);
            out.writeInt(dateId);
            out.writeInt(paymentImageId);
            out.writeInt(isShortMode ? 1 : 0);
            out.writeInt(isAnimationAllowed ? 1 : 0);
            out.writeString(maskedNumber);
            out.writeInt(isSetManually ? 1 : 0);
            out.writeInt(ignoreFormatting ? 1 : 0);
            out.writeFloat(translationX);
        }

        public static final Parcelable.Creator<CardNumberEditText.SavedState> CREATOR
                = new Parcelable.Creator<CardNumberEditText.SavedState>() {

            public CardNumberEditText.SavedState createFromParcel(Parcel in) {
                return new CardNumberEditText.SavedState(in);
            }

            public CardNumberEditText.SavedState[] newArray(int size) {
                return new CardNumberEditText.SavedState[size];
            }
        };
    }

    private void doOnLayout(@NonNull View view, Runnable runnable) {
        if (view.isLaidOut()) {
            runnable.run();
        } else {
            view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    v.removeOnLayoutChangeListener(this);
                    runnable.run();
                }
            });
        }
    }
}
