package cn.tony.music.service;


import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

import cn.tony.music.constent.IConstants;
import cn.tony.music.model.TrackErrorInfo;
import cn.tony.music.utils.Logger;


/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class MusicPlayerHandler extends Handler {

    private final String TAG = "MusicPlayerHandler";


    private WeakReference<MediaService> mService;
    private float mCurrentVolume = 1.0f;


    public MusicPlayerHandler(MediaService service, Looper looper) {
        super(looper);
        mService = new WeakReference<>(service);
    }

    @Override
    public void handleMessage(Message msg) {
        MediaService service = mService.get();
        if (service == null) {
            return;
        }
        synchronized (service) {
            switch (msg.what) {
                case IConstants.FADEDOWN:
                    mCurrentVolume -= .05f;
                    if (mCurrentVolume > .2f) {
                        sendEmptyMessageDelayed(IConstants.FADEDOWN, 10);
                    } else {
                        mCurrentVolume = .2f;
                    }
                    service.mPlayer.setVolume(mCurrentVolume);
                    break;
                case IConstants.FADEUP:
                    mCurrentVolume += .01f;
                    if (mCurrentVolume < 1.0f) {
                        sendEmptyMessageDelayed(IConstants.FADEUP, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    service.mPlayer.setVolume(mCurrentVolume);
                    break;
                case IConstants.SERVER_DIED:
                    if (service.isPlaying()) {
                        TrackErrorInfo info = (TrackErrorInfo) msg.obj;
                        service.sendErrorMessage(info.mTrackName);
                        service.removeTrack(info.mId);
                    } else {
                        service.openCurrentAndNext();
                    }
                    break;
                case IConstants.TRACK_WENT_TO_NEXT:
                    service.setAndRecordPlayPos(service.mNextPlayPos);
                    service.setNextTrack();
                    if (service.mCursor != null) {
                        service.mCursor.close();
                        service.mCursor = null;
                    }

                    service.updateCursor(service.mPlaylist.get(service.mPlayPos).mId);
                    service.notifyChange(IConstants.META_CHANGED);
                    service.notifyChange(IConstants.MUSIC_CHANGED);
                    service.updateNotification();
                    break;
                case IConstants.TRACK_ENDED:
                    if (service.mRepeatMode == IConstants.REPEAT_CURRENT) {
                        service.seek(0);
                        service.play();
                    } else {
                        Logger.info(TAG, "Going to  of track");
                        service.gotoNext(false);
                    }
                    break;
                case IConstants.RELEASE_WAKELOCK:
                    service.mWakeLock.release();
                    break;
                case IConstants.FOCUSCHANGE:
                    Logger.info(TAG, "Received audio focus change event " + msg.arg1);
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (service.isPlaying()) {
                                service.mPausedByTransientLossOfFocus =
                                        msg.arg1 == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
                            }
                            service.pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            removeMessages(IConstants.FADEUP);
                            sendEmptyMessage(IConstants.FADEDOWN);
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (!service.isPlaying()
                                    && service.mPausedByTransientLossOfFocus) {
                                service.mPausedByTransientLossOfFocus = false;
                                mCurrentVolume = 0f;
                                service.mPlayer.setVolume(mCurrentVolume);
                                service.play();
                            } else {
                                removeMessages(IConstants.FADEDOWN);
                                sendEmptyMessage(IConstants.FADEUP);
                            }
                            break;
                        default:
                    }
                    break;
                case IConstants.LRC_DOWNLOADED:
                    service.notifyChange(IConstants.LRC_UPDATED);
                default:
                    break;
            }
        }
    }
}
