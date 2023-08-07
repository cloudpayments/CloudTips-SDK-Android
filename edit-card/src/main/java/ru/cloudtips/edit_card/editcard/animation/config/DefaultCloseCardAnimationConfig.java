package ru.cloudtips.edit_card.editcard.animation.config;

public class DefaultCloseCardAnimationConfig implements CardAnimationConfig {
    private static final long DEFAULT_CARD_NUMBER_TEXT_ANIM_DURATION = 200;
    private static final long DEFAULT_CARD_NUMBER_TEXT_ANIM_START_DELAY = 0;
    private static final long DEFAULT_CARD_NUMBER_TRANSLATION_ANIM_DURATION = 210;
    private static final long DEFAULT_CARD_NUMBER_TRANSLATION_ANIM_START_DELAY = 140;
    private static final long DEFAULT_DATE_ANIM_DURATION = 200;
    private static final long DEFAULT_DATE_ANIM_START_DELAY = 350;
    private static final long DEFAULT_CVC_ANIMATION_DURATION = 200;
    private static final long DEFAULT_CVC_ANIMATION_START_DELAY = 350;

    @Override
    public long getCardNumberTextAnimationDuration() {
        return DEFAULT_CARD_NUMBER_TEXT_ANIM_DURATION;
    }

    @Override
    public long getCardNumberTextAnimationStartDelay() {
        return DEFAULT_CARD_NUMBER_TEXT_ANIM_START_DELAY;
    }

    @Override
    public long getCardNumberTranslationAnimationDuration() {
        return DEFAULT_CARD_NUMBER_TRANSLATION_ANIM_DURATION;
    }

    @Override
    public long getCardNumberTranslationAnimationStartDelay() {
        return DEFAULT_CARD_NUMBER_TRANSLATION_ANIM_START_DELAY;
    }

    @Override
    public long getDateAnimationDuration() {
        return DEFAULT_DATE_ANIM_DURATION;
    }

    @Override
    public long getDateAnimationStartDelay() {
        return DEFAULT_DATE_ANIM_START_DELAY;
    }

    @Override
    public long getCvcAnimationDuration() {
        return DEFAULT_CVC_ANIMATION_DURATION;
    }

    @Override
    public long getCvcAnimationStartDelay() {
        return DEFAULT_CVC_ANIMATION_START_DELAY;
    }
}
