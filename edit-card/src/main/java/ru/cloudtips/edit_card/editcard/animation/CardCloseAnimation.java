package ru.cloudtips.edit_card.editcard.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.text.Spanned;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.LinkedList;

import ru.cloudtips.edit_card.editcard.CardNumberEditText;
import ru.cloudtips.edit_card.editcard.CvcEditText;
import ru.cloudtips.edit_card.editcard.DateEditText;
import ru.cloudtips.edit_card.editcard.animation.config.CardAnimationConfig;
import ru.cloudtips.edit_card.editcard.animation.config.DefaultCloseCardAnimationConfig;

import static android.view.View.VISIBLE;
import static ru.cloudtips.edit_card.editcard.CardNumberEditText.SHORT_MODE_CHARS_COUNT;

/**
 * @author Stanislav Mukhametshin
 */
public class CardCloseAnimation implements CardAnimation {

    private static final CardFieldAlphaProperty CARD_FIELD_ALPHA = new CardFieldAlphaProperty("card_field_alpha");

    private final CardAnimationConfig animationConfig;

    public CardCloseAnimation() {
        animationConfig = new DefaultCloseCardAnimationConfig();
    }

    public CardCloseAnimation(CardAnimationConfig config) {
        animationConfig = config;
    }

    @Override
    public Animator createCardTextAnimation(final CardNumberEditText cardNumberEditText) {
        final MutableAlphaColorSpan span = new MutableAlphaColorSpan(cardNumberEditText.getCurrentTextColor());
        cardNumberEditText.getText().setSpan(span, 0,
                Math.max(cardNumberEditText.length() - SHORT_MODE_CHARS_COUNT, 0), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ObjectAnimator animatorText = ObjectAnimator.ofInt(span, MutableAlphaColorSpan.ALPHA, 255, 0);
        animatorText.setDuration(animationConfig.getCardNumberTextAnimationDuration());
        animatorText.setStartDelay(animationConfig.getCardNumberTextAnimationStartDelay());
        animatorText.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (!cardNumberEditText.isShortMode()) {
                    cardNumberEditText.getText().setSpan(span, 0,
                            Math.max(cardNumberEditText.length() - SHORT_MODE_CHARS_COUNT, 0), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        });
        return animatorText;
    }

    @Override
    public Animator createCardTranslationAnimation(final CardNumberEditText cardNumberEditText) {
        ObjectAnimator toLeftAnimator = ObjectAnimator.ofFloat(cardNumberEditText, CardNumberEditText.ANIMATOR_FACTOR, 0f, 1f);
        toLeftAnimator.setStartDelay(animationConfig.getCardNumberTranslationAnimationStartDelay());
        toLeftAnimator.setDuration(animationConfig.getCardNumberTranslationAnimationDuration());
        toLeftAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cardNumberEditText.setShortMode(true);
            }
        });
        return toLeftAnimator;
    }

    @Override
    public Animator createDateAnimation(DateEditText etDate) {
        etDate.setVisibility(VISIBLE);
        etDate.setAlpha(0f);
        ObjectAnimator animatorDate = ObjectAnimator.ofFloat(etDate, CARD_FIELD_ALPHA, 0f, 1f);
        animatorDate.setDuration(animationConfig.getDateAnimationDuration());
        animatorDate.setStartDelay(animationConfig.getDateAnimationStartDelay());
        return animatorDate;
    }

    @Override
    public Animator createCvcAnimation(CvcEditText etCvc) {
        etCvc.setVisibility(VISIBLE);
        etCvc.setAlpha(0f);
        ObjectAnimator animatorCvc = ObjectAnimator.ofFloat(etCvc, CARD_FIELD_ALPHA, 0f, 1f);
        animatorCvc.setDuration(animationConfig.getCvcAnimationDuration());
        animatorCvc.setStartDelay(animationConfig.getCvcAnimationStartDelay());
        return animatorCvc;
    }

    @Override
    public Animator createFullAnimation(final CardNumberEditText cardNumberEditText, DateEditText dateEditText, CvcEditText cvcEditText) {
        Collection<Animator> animators = new LinkedList<>();
        animators.add(createCardTextAnimation(cardNumberEditText));
        animators.add(createCardTranslationAnimation(cardNumberEditText));
        if (dateEditText != null && !dateEditText.isGoneMode()) {
            animators.add(createDateAnimation(dateEditText));
        }
        if (cvcEditText != null && !cvcEditText.isGoneMode()) {
            animators.add(createCvcAnimation(cvcEditText));
        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                endState(cardNumberEditText);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                cardNumberEditText.clearFocus();
                cardNumberEditText.activateNext();
                cardNumberEditText.scrollMax();
            }
        });
        return set;
    }

    @Override
    public void endState(CardNumberEditText cardNumberEditText) {
        ViewGroup.LayoutParams param = cardNumberEditText.getLayoutParams();
        cardNumberEditText.initInitialWidth();
        String text = cardNumberEditText.getText().toString();
        int textLength = cardNumberEditText.length();
        param.width = (int) (cardNumberEditText.getPaint().measureText(
                text.substring(Math.max(textLength - SHORT_MODE_CHARS_COUNT - 2, 0), textLength)))
                + cardNumberEditText.getTotalPaddingLeft() + cardNumberEditText.getTotalPaddingRight();
        cardNumberEditText.onCardChanged();
        cardNumberEditText.setAnimationFactor(0f);
        cardNumberEditText.setLayoutParams(param);
    }
}
