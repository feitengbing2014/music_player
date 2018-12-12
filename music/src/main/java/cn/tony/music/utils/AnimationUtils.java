package cn.tony.music.utils;

import android.support.v4.view.ViewPager;
import android.view.animation.LinearInterpolator;

import java.lang.reflect.Field;

import cn.tony.music.views.AlbumViewPager;
import cn.tony.music.views.MyScroller;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/30.
 */

public class AnimationUtils {


    public static void addViewPagerAnimation(AlbumViewPager mViewPager) {
        // 改变viewpager动画时间
        try {
            Field mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);
            MyScroller mScroller = new MyScroller(mViewPager.getContext().getApplicationContext(), new LinearInterpolator());
            mField.set(mViewPager, mScroller);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
