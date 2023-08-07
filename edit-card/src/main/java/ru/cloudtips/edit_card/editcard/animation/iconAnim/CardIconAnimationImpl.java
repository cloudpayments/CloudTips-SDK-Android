package ru.cloudtips.edit_card.editcard.animation.iconAnim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import ru.cloudtips.edit_card.editcard.CardNumberEditText;

/**
 * @author Stanislav Mukhametshin
 */
public class CardIconAnimationImpl implements CardIconAnimation {

    @NonNull
    @Override
    public Animator createShowLogoAnimation(final CardNumberEditText cardNumberEditText, final ImageView paymentSystemImage) {
        ObjectAnimator animatorAlpha = ObjectAnimator.ofFloat(paymentSystemImage, View.ALPHA, 1.0f);
        animatorAlpha.setDuration(getOpenAnimationDuration());

        ObjectAnimator translateX = ObjectAnimator.ofFloat(cardNumberEditText, View.TRANSLATION_X, getImageWidthMargin(paymentSystemImage));
        translateX.setDuration(getOpenAnimationDuration());
        translateX.setInterpolator(new OvershootInterpolator());
        translateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                paymentSystemImage.requestLayout();
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(translateX, animatorAlpha);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                paymentSystemImage.setVisibility(View.VISIBLE);
            }
        });
        return set;
    }

    @Override
    public void showLogoWithoutAnimation(final CardNumberEditText cardNumberEditText, final ImageView paymentSystemImage) {
        cardNumberEditText.setTranslationX(getImageWidthMargin(paymentSystemImage));
        paymentSystemImage.setAlpha(1f);
        paymentSystemImage.setVisibility(View.VISIBLE);
    }

    @NonNull
    @Override
    public Animator createHideLogoAnimation(final CardNumberEditText cardNumberEditText, final ImageView paymentSystemImage) {
        ObjectAnimator animatorAlpha = ObjectAnimator.ofFloat(paymentSystemImage, View.ALPHA, 0.0f);
        animatorAlpha.setDuration(getHideAnimationDuration());

        ObjectAnimator translateX = ObjectAnimator.ofFloat(cardNumberEditText, View.TRANSLATION_X, 0);
        translateX.setDuration(getHideAnimationDuration());
        translateX.setInterpolator(new OvershootInterpolator());
        translateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                paymentSystemImage.requestLayout();
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(animatorAlpha, translateX);
        set.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                paymentSystemImage.setVisibility(View.INVISIBLE);
                paymentSystemImage.setAlpha(1f);
            }
        });
        return set;
    }

    protected int getOpenAnimationDuration() {
        return 150;
    }

    protected int getHideAnimationDuration() {
        return 150;
    }

    private int getImageWidthMargin(ImageView paymentSystemImage) {
        ViewGroup.MarginLayoutParams imageParams = ((ViewGroup.MarginLayoutParams) paymentSystemImage.getLayoutParams());
        return paymentSystemImage.getWidth() + imageParams.leftMargin + imageParams.rightMargin;
    }
}
