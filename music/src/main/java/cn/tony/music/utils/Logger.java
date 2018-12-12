package cn.tony.music.utils;

import android.util.Log;

import cn.tony.music.BuildConfig;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class Logger {

    private static final String TAG = "music ";

    public static void info(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG + tag, msg);
        }
        Log.i(TAG + tag, msg);

    }
}
