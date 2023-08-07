package ru.cloudtips.edit_card.editcard.animation;

import android.animation.Animator;
import ru.cloudtips.edit_card.editcard.CardNumberEditText;
import ru.cloudtips.edit_card.editcard.CvcEditText;
import ru.cloudtips.edit_card.editcard.DateEditText;

/**
 * @author Stanislav Mukhametshin
 * <p>
 * Used to Animate one of the cases: expanding the {@link CardNumberEditText}
 * or hiding it. You can change the animation by {@link CardNumberEditText#setOpenAnimation(CardAnimation)}
 * and {@link CardNumberEditText#setCloseAnimation(CardAnimation)}
 * @see CardCloseAnimation
 * @see CardOpenAnimation
 */
public interface CardAnimation {

    /**
     * @param cardNumberEditText the view to animate
     * @return animation of the text of the {@link CardNumberEditText}
     */
    Animator createCardTextAnimation(CardNumberEditText cardNumberEditText);

    /**
     * @param cardNumberEditText the view to animate
     * @return animation of the movement of {@link CardNumberEditText}
     */
    Animator createCardTranslationAnimation(CardNumberEditText cardNumberEditText);

    /**
     * @param etDate the date to animate
     * @return animation of date
     */
    Animator createDateAnimation(DateEditText etDate);

    /**
     * @param etCvc the cvc code to animate
     * @return animation of cvc
     */
    Animator createCvcAnimation(CvcEditText etCvc);

    /**
     * Here we are join all animation together and saying in what
     * order they should play.
     *
     * @param cardNumberEditText the number to animate
     * @param dateEditText       the date to animate
     * @param cvcEditText        the cvc to animate
     * @return a close or open animation for a view.
     */
    Animator createFullAnimation(CardNumberEditText cardNumberEditText, DateEditText dateEditText, CvcEditText cvcEditText);

    /**
     * We can call it for example when animation finished
     * Also it is used when the screen is rotating
     *
     * @param cardNumberEditText - the edit text with number
     *                           that is needed to change to the end state
     */
    void endState(CardNumberEditText cardNumberEditText);
}
