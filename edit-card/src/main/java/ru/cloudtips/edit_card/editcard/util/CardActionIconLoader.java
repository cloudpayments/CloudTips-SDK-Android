package ru.cloudtips.edit_card.editcard.util;

import android.os.CancellationSignal;
import android.widget.ImageView;

import androidx.annotation.NonNull;

/**
 * @author k.voskrebentsev
 */
public interface CardActionIconLoader {
    /**
     * The method should be used to loadImage into cardActionImageView.
     *
     * @param cardActionImageView  - ImageView that we should use to load an image
     * @return an object to cancel the load by invoking the cancel() method
     */
    CancellationSignal loadIcon(@NonNull ImageView cardActionImageView);
}
