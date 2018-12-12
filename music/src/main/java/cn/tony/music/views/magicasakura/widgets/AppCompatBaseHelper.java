package cn.tony.music.views.magicasakura.widgets;

import android.util.AttributeSet;
import android.view.View;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/26.
 */

public abstract class AppCompatBaseHelper {
    protected View mView;
    protected TintManager mTintManager;

    private boolean mSkipNextApply;

    public AppCompatBaseHelper(View view, TintManager tintManager) {
        mView = view;
        mTintManager = tintManager;
    }

    protected boolean skipNextApply() {
        if (mSkipNextApply) {
            mSkipNextApply = false;
            return true;
        }
        mSkipNextApply = true;
        return false;
    }

    protected void setSkipNextApply(boolean flag) {
        mSkipNextApply = flag;
    }

    abstract void loadFromAttribute(AttributeSet attrs, int defStyleAttr);

    public abstract void tint();
}
