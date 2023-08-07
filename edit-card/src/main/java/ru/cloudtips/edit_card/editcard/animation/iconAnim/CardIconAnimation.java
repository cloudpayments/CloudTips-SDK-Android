package ru.cloudtips.edit_card.editcard.animation.iconAnim;

import android.animation.Animator;
import androidx.annotation.NonNull;
import android.widget.ImageView;
import ru.cloudtips.edit_card.editcard.CardNumberEditText;

/**
 * @author Stanislav Mukhametshin
 * <p>
 * The class needed to change the animation of the
 * payment image
 * @see CardNumberEditText#setCardIconAnimation(CardIconAnimation)
 */
public interface CardIconAnimation {

    /**
     * @param cardNumberEditText number to animate
     * @param paymentSystemImage logo to animate
     * @return the set of animations that should show the logo
     */
    @NonNull
    Animator createShowLogoAnimation(CardNumberEditText cardNumberEditText, ImageView paymentSystemImage);

    /**
     * Basically it is used to immediately show the logo after rotation
     *
     * @param cardNumberEditText the number to move to the end state
     * @param paymentSystemImage the view to move to the end state
     * @see CardNumberEditText#onAttachedToWindow()
     */
    void showLogoWithoutAnimation(CardNumberEditText cardNumberEditText, ImageView paymentSystemImage);

    /**
     * @param cardNumberEditText the number to animate
     * @param paymentSystemImage the logo to hide
     * @return the set of animations that should hide the logo
     */
    @NonNull
    Animator createHideLogoAnimation(CardNumberEditText cardNumberEditText, ImageView paymentSystemImage);
}
