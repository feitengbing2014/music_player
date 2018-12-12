package cn.tony.music.views;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import cn.tony.music.constent.IConstants;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/26.
 */

public class MyScroller extends Scroller {

    private int animTime = IConstants.VIEWPAGER_SCROLL_TIME;

    public MyScroller(Context context) {
        super(context);
    }

    public MyScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        super.startScroll(startX, startY, dx, dy, animTime);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy) {
        super.startScroll(startX, startY, dx, dy, animTime);
    }

    public void setmDuration(int animTime) {
        this.animTime = animTime;
    }
}
