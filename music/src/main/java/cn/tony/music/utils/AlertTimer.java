package cn.tony.music.utils;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;

import java.lang.ref.WeakReference;

/**
 * Ella Group
 * <p>   定时器 取代Alert
 * <p>
 * Author by Tony, on 2018/11/26.
 */

public class AlertTimer extends CountDownTimer {


    private WeakReference<Context> contextWeakReference;
    private Intent intent;

    /**
     * @param millisInFuture The number of millis in the future from the call
     *                       to {@link #start()} until the countdown is done and {@link #onFinish()}
     *                       is called.
     * @param intent
     */
    public AlertTimer(Context context, long millisInFuture, Intent intent) {
        super(millisInFuture, millisInFuture);
        contextWeakReference = new WeakReference<>(context);
        this.intent = intent;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        
    }

    @Override
    public void onFinish() {
        cancel();
        Context context = contextWeakReference.get();
        if (context != null) {
            context.sendBroadcast(intent);
        }
    }
}
