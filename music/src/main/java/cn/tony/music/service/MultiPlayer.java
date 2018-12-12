package cn.tony.music.service;

import android.annotation.TargetApi;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;

import java.io.IOException;
import java.lang.ref.WeakReference;

import cn.tony.music.constent.IConstants;
import cn.tony.music.model.TrackErrorInfo;
import cn.tony.music.utils.Logger;
import cn.tony.music.utils.MusicUtils;

/**
 * Ella Group
 * <p> 音乐播放数据源处理
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class MultiPlayer implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final String TAG = "MultiPlayer";

    private WeakReference<MediaService> mService;

    private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();

    private MediaPlayer mNextMediaPlayer;

    private Handler mHandler;

    private boolean mIsInitialized = false;

    private String mNextMediaPath;

    private boolean isFirstLoad = true;


    boolean mIsTrackPrepared = false;
    boolean mIsTrackNet = false;
    boolean mIsNextTrackPrepared = false;
    boolean mIsNextInitialized = false;
    boolean mIllegalState = false;


    public static int sencondaryPosition = 0;

    private Handler handler = new Handler();


    public MultiPlayer(MediaService service) {
        mService = new WeakReference<>(service);
        mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);

    }

    public void setDataSource(String path) {
        Logger.info(TAG, "====setDataSource=====" + path);
        mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
        if (mIsInitialized) {
            setNextDataSource(null);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setNextDataSource(String path) {
        mNextMediaPath = null;
        mIsNextInitialized = false;
        try {
            mCurrentMediaPlayer.setNextMediaPlayer(null);
        } catch (IllegalArgumentException e) {
            Logger.info(TAG, "Next media player is current one, continuing");
        } catch (IllegalStateException e) {
            Logger.info(TAG, "Media player not initialized!");
            return;
        }
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.release();
            mNextMediaPlayer = null;
        }
        if (path == null) {
            return;
        }
        mNextMediaPlayer = new MediaPlayer();
        mNextMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
        mNextMediaPlayer.setAudioSessionId(getAudioSessionId());

        if (setNextDataSourceImpl(mNextMediaPlayer, path)) {
            mNextMediaPath = path;
            mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            // mHandler.post(setNextMediaPlayerIfPrepared);

        } else {
            if (mNextMediaPlayer != null) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
        }
    }


    private boolean setDataSourceImpl(final MediaPlayer player, String path) {
        mIsTrackNet = false;
        mIsTrackPrepared = false;
        try {
            player.reset();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            path = MusicUtils.getDownFilePath(mService.get(), path);
            if (path.startsWith("content://")) {
                player.setOnPreparedListener(null);
                player.setDataSource(mService.get(), Uri.parse(path));
                player.prepare();
                mIsTrackPrepared = true;
                player.setOnCompletionListener(this);
            } else {
                player.setDataSource(path);
                player.setOnPreparedListener(preparedListener);
                player.prepareAsync();
                mIsTrackNet = true;
            }
            if (mIllegalState) {
                mIllegalState = false;
            }

        } catch (final IOException todo) {

            return false;
        } catch (final IllegalArgumentException todo) {

            return false;
        } catch (final IllegalStateException todo) {
            todo.printStackTrace();
            if (!mIllegalState) {
                Logger.info(TAG, "mcurrentmediaplayer invoke IllegalState");
                mCurrentMediaPlayer = null;
                mCurrentMediaPlayer = new MediaPlayer();
                mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
                mCurrentMediaPlayer.setAudioSessionId(getAudioSessionId());
                setDataSourceImpl(mCurrentMediaPlayer, path);
                mIllegalState = true;
            } else {
                Logger.info(TAG, "mcurrentmediaplayer invoke IllegalState ,and try set again failed ,setnext");
                mIllegalState = false;
                return false;
            }
        }

        player.setOnErrorListener(this);
        player.setOnBufferingUpdateListener(bufferingUpdateListener);
        return true;
    }

    private boolean setNextDataSourceImpl(final MediaPlayer player, String path) {
        mIsNextTrackPrepared = false;
        try {
            player.reset();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            path = MusicUtils.getDownFilePath(mService.get(), path);
            if (path.startsWith("content://")) {
                player.setOnPreparedListener(preparedNextListener);
                player.setDataSource(mService.get(), Uri.parse(path));
                player.prepare();
            } else {
                player.setDataSource(path);
                player.setOnPreparedListener(preparedNextListener);
                player.prepare();
                mIsNextTrackPrepared = false;
            }
        } catch (final IOException todo) {
            return false;
        } catch (final IllegalArgumentException todo) {
            return false;
        }
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        //  player.setOnBufferingUpdateListener(this);
        return true;
    }

    public void setHandler(final Handler handler) {
        mHandler = handler;
    }


    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isTrackPrepared() {
        return mIsTrackPrepared;
    }


    public void start() {
        Logger.info(TAG, "mIsTrackNet, " + mIsTrackNet);
        if (!mIsTrackNet) {
            mService.get().sendUpdateBuffer(100);
            sencondaryPosition = 100;
            mCurrentMediaPlayer.start();
        } else {
            sencondaryPosition = 0;
            mService.get().loading(true);
            handler.postDelayed(startMediaPlayerIfPrepared, 50);
        }
        mService.get().notifyChange(IConstants.MUSIC_CHANGED);
    }

    MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            if (isFirstLoad) {
                long seekpos = mService.get().mLastSeekPos;
                Logger.info(TAG, "seekpos = " + seekpos);
                seek(seekpos >= 0 ? seekpos : 0);
                isFirstLoad = false;
            }
            // mService.get().notifyChange(TRACK_PREPARED);
            mService.get().notifyChange(IConstants.META_CHANGED);
            mp.setOnCompletionListener(MultiPlayer.this);
            mIsTrackPrepared = true;
        }
    };

    MediaPlayer.OnPreparedListener preparedNextListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mIsNextTrackPrepared = true;
        }
    };

    MediaPlayer.OnBufferingUpdateListener bufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (sencondaryPosition != 100)
                mService.get().sendUpdateBuffer(percent);
            sencondaryPosition = percent;
        }
    };

    Runnable setNextMediaPlayerIfPrepared = new Runnable() {
        int count = 0;

        @Override
        public void run() {
            if (mIsNextTrackPrepared && mIsInitialized) {

//                    mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            } else if (count < 60) {
                handler.postDelayed(setNextMediaPlayerIfPrepared, 100);
            }
            count++;
        }
    };

    Runnable startMediaPlayerIfPrepared = new Runnable() {

        @Override
        public void run() {
            Logger.info(TAG, "mIsTrackPrepared, " + mIsTrackPrepared);
            if (mIsTrackPrepared) {
                mCurrentMediaPlayer.start();
                final long duration = duration();
                if (mService.get().mRepeatMode != IConstants.REPEAT_CURRENT && duration > 2000
                        && position() >= duration - 2000) {
                    mService.get().gotoNext(true);
                    Logger.info(TAG, "play to go");
                }
                mService.get().loading(false);
            } else {
                handler.postDelayed(startMediaPlayerIfPrepared, 700);
            }
        }
    };


    public void stop() {
        handler.removeCallbacks(setNextMediaPlayerIfPrepared);
        handler.removeCallbacks(startMediaPlayerIfPrepared);
        mCurrentMediaPlayer.reset();
        mIsInitialized = false;
        mIsTrackPrepared = false;
    }


    public void release() {
        mCurrentMediaPlayer.release();
    }


    public void pause() {
        handler.removeCallbacks(startMediaPlayerIfPrepared);
        mCurrentMediaPlayer.pause();
    }


    public long duration() {
        if (mIsTrackPrepared) {
            return mCurrentMediaPlayer.getDuration();
        }
        return -1;
    }


    public long position() {
        if (mIsTrackPrepared) {
            try {
                return mCurrentMediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public long secondPosition() {
        if (mIsTrackPrepared) {
            return sencondaryPosition;
        }
        return -1;
    }


    public long seek(final long whereto) {
        mCurrentMediaPlayer.seekTo((int) whereto);
        return whereto;
    }


    public void setVolume(final float vol) {
        try {
            mCurrentMediaPlayer.setVolume(vol, vol);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getAudioSessionId() {
        return mCurrentMediaPlayer.getAudioSessionId();
    }

    public void setAudioSessionId(final int sessionId) {
        mCurrentMediaPlayer.setAudioSessionId(sessionId);
    }


    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        Logger.info(TAG, "Music Server Error what: " + what + " extra: " + extra);
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                MediaService service = mService.get();
                TrackErrorInfo errorInfo = new TrackErrorInfo(service.getAudioId(),
                        service.getTrackName());
                mIsInitialized = false;
                mIsTrackPrepared = false;
                mCurrentMediaPlayer.release();
                mCurrentMediaPlayer = new MediaPlayer();
                mCurrentMediaPlayer.setWakeMode(service, PowerManager.PARTIAL_WAKE_LOCK);
                Message msg = mHandler.obtainMessage(IConstants.SERVER_DIED, errorInfo);
                mHandler.sendMessageDelayed(msg, 2000);
                return true;
            default:
                break;
        }
        return false;
    }


    @Override
    public void onCompletion(final MediaPlayer mp) {
        Logger.info(TAG, "completion");
        if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
            mCurrentMediaPlayer.release();
            mCurrentMediaPlayer = mNextMediaPlayer;
            mNextMediaPath = null;
            mNextMediaPlayer = null;
            mHandler.sendEmptyMessage(IConstants.TRACK_WENT_TO_NEXT);
        } else {
            mService.get().mWakeLock.acquire(30000);
            mHandler.sendEmptyMessage(IConstants.TRACK_ENDED);
            mHandler.sendEmptyMessage(IConstants.RELEASE_WAKELOCK);
        }
    }

}
