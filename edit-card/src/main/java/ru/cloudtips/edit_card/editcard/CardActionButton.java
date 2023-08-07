package ru.cloudtips.edit_card.editcard;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.CancellationSignal;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import java.lang.annotation.Retention;

import ru.cloudtips.edit_card.editcard.util.CardActionButtonShowRule;
import ru.cloudtips.edit_card.editcard.util.CardActionIconLoader;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * @author Stanislav Mukhametshin
 */
public class CardActionButton extends AppCompatImageButton implements CardNumberEditText.OnCardViewChanged {

    public static final int NO_IMAGE = 0;
    public static final int ACTION = 1;
    public static final int NEXT = 2;

    private ActionButtonClickListener actionButtonClickListener;
    @State
    private int state = NO_IMAGE;
    private boolean withActionButton = false;
    @Nullable
    private CardActionButtonShowRule actionButtonShowRule;
    private Drawable nextIcon;
    private Drawable actionIcon;
    private CardActionIconLoader actionIconLoader;
    private CancellationSignal cancelingActionIconLoad;
    private CardNumberEditText cardNumberEditText;
    private boolean isActionOnlyInOpenedCard = true;

    public CardActionButton(Context context) {
        super(context);
    }

    public CardActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CardActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(final CardNumberEditText cardNumberEditText) {
        this.cardNumberEditText = cardNumberEditText;
        cardNumberEditText.setOnCardViewChanged(this);
        setOnClickListener(v -> {
            if (state == NEXT) {
                state = NO_IMAGE;
                setImageDrawable(null);
                cardNumberEditText.doAnimationOrActivateNext();
                return;
            }
            if (state == ACTION && actionButtonClickListener != null) {
                actionButtonClickListener.onActionClick();
            }
        });
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        ActionButtonBaseState ss = new ActionButtonBaseState(superState);
        ss.isActionOnlyInOpenedCard = isActionOnlyInOpenedCard;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        ActionButtonBaseState ss = (ActionButtonBaseState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        isActionOnlyInOpenedCard = ss.isActionOnlyInOpenedCard;
    }

    public void setNextIcon(@DrawableRes int res) {
        if (res == NO_ID) {
            setNextIcon(null);
        }
        setNextIcon(ContextCompat.getDrawable(getContext(), res));
    }

    public void setActionIcon(@DrawableRes int res) {
        setActionIcon(res, null);
    }

    public void setActionIcon(@DrawableRes int res, @Nullable CardActionButtonShowRule buttonShowRule) {
        if (res == NO_ID) {
            setActionIcon(null);
        }
        setActionIcon(AppCompatResources.getDrawable(getContext(), res), buttonShowRule);
    }

    public void setActionIcon(@Nullable Drawable actionIcon) {
        setActionIcon(actionIcon, null);
    }

    public void setActionIcon(@Nullable Drawable actionIcon, @Nullable CardActionButtonShowRule buttonShowRule) {
        this.actionButtonShowRule = buttonShowRule;
        this.actionIcon = actionIcon;
        checkWithActionButtonFlag();
    }

    public void setActionIconLoader(@Nullable CardActionIconLoader actionIconLoader) {
        this.actionIconLoader = actionIconLoader;
        checkWithActionButtonFlag();
    }

    private void checkWithActionButtonFlag() {
        if (actionIcon != null || actionIconLoader != null) {
            withActionButton = true;
            updateIcon();
        } else {
            withActionButton = false;
        }
    }

    public void setNextIcon(@Nullable Drawable nextIcon) {
        this.nextIcon = nextIcon;
    }

    public void setActionOnlyInOpenedCard(boolean isActionOnlyInOpenedCard) {
        this.isActionOnlyInOpenedCard = isActionOnlyInOpenedCard;
        updateIcon();
    }

    public void updateIcon() {
        boolean isNeedToShowRule = actionButtonShowRule != null
                && actionButtonShowRule.isNeedToShow(cardNumberEditText.getNumber(), cardNumberEditText.getMaskedNumber());
        if (cardNumberEditText.isValid() && cardNumberEditText.isAnimationAllowed() && !cardNumberEditText.isShortMode()
                && !cardNumberEditText.isBlockedMode()) {
            state = NEXT;
            changeVisibility(true);
            setImageDrawable(nextIcon);
        } else if (withActionButton && (actionButtonShowRule == null || isNeedToShowRule)
                && (!isActionOnlyInOpenedCard || !cardNumberEditText.isShortMode())) {
            state = ACTION;
            changeVisibility(true);
            if (actionIconLoader != null) {
                cancelingActionIconLoad = actionIconLoader.loadIcon(this);
            } else {
                setImageDrawable(actionIcon);
            }
        } else {
            state = NO_IMAGE;
            changeVisibility(false);
            setImageDrawable(null);
        }
    }

    private void cancelActionIconLoad() {
        if (cancelingActionIconLoad != null) {
            cancelingActionIconLoad.cancel();
            cancelingActionIconLoad = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelActionIconLoad();
        super.onDetachedFromWindow();
    }

    public void setActionButtonClickListener(@Nullable ActionButtonClickListener actionButtonClickListener) {
        this.actionButtonClickListener = actionButtonClickListener;
    }

    public int getState() {
        return state;
    }

    @Override
    public void onCardChanged() {
        updateIcon();
    }

    @Override
    public void onCardAnimationStarted() {
        if (isActionOnlyInOpenedCard && !cardNumberEditText.isShortMode()) {
            state = NO_IMAGE;
            setImageDrawable(null);
        }
    }

    private void changeVisibility(boolean changeToVisible) {
        if (changeToVisible && !isShown()) {
            setVisibility(VISIBLE);
        } else if (!changeToVisible) {
            setVisibility(INVISIBLE);
        }
    }

    @IntDef({NO_IMAGE, ACTION, NEXT})
    @Retention(SOURCE)
    private @interface State {
    }

    public interface ActionButtonClickListener {

        void onActionClick();
    }

    protected static class ActionButtonBaseState extends BaseSavedState {

        public static final ClassLoaderCreator<ActionButtonBaseState> CREATOR
                = new ClassLoaderCreator<ActionButtonBaseState>() {
            @Override
            public ActionButtonBaseState createFromParcel(Parcel source, ClassLoader loader) {
                return new ActionButtonBaseState(source, loader);
            }

            @Override
            public ActionButtonBaseState createFromParcel(Parcel source) {
                return createFromParcel(source, null);
            }

            public ActionButtonBaseState[] newArray(int size) {
                return new ActionButtonBaseState[size];
            }
        };
        boolean isActionOnlyInOpenedCard;

        ActionButtonBaseState(Parcelable superState) {
            super(superState);
        }

        private ActionButtonBaseState(Parcel in, ClassLoader classLoader) {
            super(in);
            isActionOnlyInOpenedCard = in.readInt() == 1;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.isActionOnlyInOpenedCard ? 1 : 0);
        }
    }
}
