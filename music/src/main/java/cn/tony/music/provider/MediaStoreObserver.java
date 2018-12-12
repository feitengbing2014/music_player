package cn.tony.music.provider;

import android.database.ContentObserver;
import android.os.Handler;

import cn.tony.music.service.MediaService;
import cn.tony.music.utils.Logger;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class MediaStoreObserver extends ContentObserver implements Runnable {

    private static final long REFRESH_DELAY = 500;
    private Handler mHandler;
    private MediaService service;

    public MediaStoreObserver(Handler handler, MediaService service) {
        super(handler);
        mHandler = handler;
        this.service = service;
    }

    @Override
    public void onChange(boolean selfChange) {
        mHandler.removeCallbacks(this);
        mHandler.postDelayed(this, REFRESH_DELAY);
    }

    @Override
    public void run() {
        Logger.info("MediaStoreObserver", "calling refresh!");
        service.refresh();
    }
}