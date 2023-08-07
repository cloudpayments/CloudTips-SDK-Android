package ru.cloudtips.edit_card.editcard.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import ru.cloudtips.edit_card.editcard.CardNumberEditText;
import ru.cloudtips.edit_card.editcard.CvcEditText;
import ru.cloudtips.edit_card.editcard.DateEditText;
import ru.cloudtips.edit_card.editcard.animation.config.CardAnimationConfig;
import ru.cloudtips.edit_card.editcard.animation.config.DefaultOpenCardAnimationConfig;

import java.util.Collection;
import java.util.LinkedList;

import static ru.cloudtips.edit_card.editcard.CardNumberEditText.SHORT_MODE_CHARS_COUNT;

/**
 * @author Stanislav Mukhametshin
 */
public class CardOpenAnimation implements CardAnimation {

    private static final CardFieldAlphaProperty CARD_FIELD_ALPHA = new CardFieldAlphaProperty("card_field_alpha");

    private final CardAnimationConfig animationConfig;

    public CardOpenAnimation() {
        animationConfig = new DefaultOpenCardAnimationConfig();
    }

    public CardOpenAnimation(CardAnimationConfig config) {
        animationConfig = config;
    }

    @Override
    public Animator createCardTextAnimation(final CardNumberEditText cardNumberEditText) {
        final MutableAlphaColorSpan span = new MutableAlphaColorSpan(cardNumberEditText.getPaint().getColor());
        span.setAlpha(0);
        cardNumberEditText.getText().setSpan(span, 0, Math.max(cardNumberEditText.length() - SHORT_MODE_CHARS_COUNT, 0), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ObjectAnimator animatorText = ObjectAnimator.ofInt(span, MutableAlphaColorSpan.ALPHA, 0, 255);
        animatorText.setDuration(animationConfig.getCardNumberTextAnimationDuration());
        animatorText.setStartDelay(animationConfig.getCardNumberTextAnimationStartDelay());
        animatorText.setInterpolator(new AccelerateInterpolator());
        animatorText.addUpdateListener(animation -> {
            span.setColor(cardNumberEditText.getPaint().getColor());
            cardNumberEditText.getText().setSpan(span, 0, Math.max(cardNumberEditText.length() - SHORT_MODE_CHARS_COUNT, 0), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        });
        return animatorText;
    }

    @Override
    public Animator createCardTranslationAnimation(final CardNumberEditText cardNumberEditText) {
        ObjectAnimator toRightAnimator = ObjectAnimator.ofFloat(cardNumberEditText, CardNumberEditText.ANIMATOR_FACTOR, 1f, 0f);
        toRightAnimator.setDuration(animationConfig.getCardNumberTranslationAnimationDuration());
        toRightAnimator.setStartDelay(animationConfig.getCardNumberTranslationAnimationStartDelay());
        return toRightAnimator;
    }

    @Override
    public Animator createDateAnimation(final DateEditText etDate) {
        etDate.setVisibility(View.VISIBLE);
        ObjectAnimator animatorDate = ObjectAnimator.ofFloat(etDate, CARD_FIELD_ALPHA, 1f, 0f);
        animatorDate.setDuration(animationConfig.getDateAnimationDuration());
        animatorDate.setStartDelay(animationConfig.getDateAnimationStartDelay());
        animatorDate.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                etDate.setVisibility(View.GONE);
            }
        });
        return animatorDate;
    }

    @Override
    public Animator createCvcAnimation(final CvcEditText etCvc) {
        ObjectAnimator animatorCvc = ObjectAnimator.ofFloat(etCvc, CARD_FIELD_ALPHA, 1f, 0f);
        animatorCvc.setDuration(animationConfig.getCvcAnimationDuration());
        animatorCvc.setStartDelay(animationConfig.getCvcAnimationStartDelay());
        animatorCvc.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                etCvc.setVisibility(View.GONE);
            }
        });
        return animatorCvc;
    }

    @Override
    public Animator createFullAnimation(final CardNumberEditText cardNumberEditText, DateEditText dateEditText, CvcEditText cvcEditText) {
        AnimatorSet set = new AnimatorSet();
        Collection<Animator> animators = new LinkedList<>();
        animators.add(createCardTextAnimation(cardNumberEditText));
        animators.add(createCardTranslationAnimation(cardNumberEditText));
        if (dateEditText != null && !dateEditText.isGoneMode()) {
            animators.add(createDateAnimation(dateEditText));
        }
        if (cvcEditText != null && !cvcEditText.isGoneMode()) {
            animators.add(createCvcAnimation(cvcEditText));
        }
        set.playTogether(animators);
        set.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                ViewGroup.LayoutParams param = cardNumberEditText.getLayoutParams();
                cardNumberEditText.setShortMode(false);
                cardNumberEditText.setAnimationFactor(1f);
                param.width = cardNumberEditText.getInitialWidth();
                cardNumberEditText.setLayoutParams(param);
                cardNumberEditText.postScrollMax();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                endState(cardNumberEditText);
            }
        });
        return set;
    }

    @Override
    public void endState(CardNumberEditText cardNumberEditText) {
        MutableAlphaColorSpan[] toRemoveSpans = cardNumberEditText.getText().getSpans(0, cardNumberEditText.getText().length(), MutableAlphaColorSpan.class);
        if (toRemoveSpans != null) {
            for (MutableAlphaColorSpan toRemoveSpan : toRemoveSpans) {
                cardNumberEditText.getText().removeSpan(toRemoveSpan);
            }
        }
        cardNumberEditText.activate();
        cardNumberEditText.onCardChanged();
        cardNumberEditText.setAnimationFactor(0f);
    }
}
