package ru.cloudtips.edit_card.editcard.util;

import androidx.annotation.NonNull;
import android.widget.ImageView;

/**
 * @author Stanislav Mukhametshin
 * <p>
 * You can use it to load new image when payment types is changed
 * You should set this parameter through
 * {@link ru.cloudtips.edit_card.editcard.CardNumberEditText#setCardImageLoader(CardImageLoader)}
 */
public interface CardImageLoader {

    /**
     * The method should be used to loadImage into cardImageView.
     *
     * @param cardImageView  - ImageView that we should use to load an image
     * @param numberPart - known part of the number that is now shown in {@link ru.cloudtips.edit_card.editcard.CardNumberEditText}
     * @return false if logo is empty or we made it empty.
     */
    boolean loadImage(@NonNull ImageView cardImageView, @NonNull String numberPart);
}
