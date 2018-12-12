package cn.tony.music.utils;

import android.content.Intent;
import android.os.Bundle;

import cn.tony.music.activity.LockActivity;
import cn.tony.music.constent.IConstants;
import cn.tony.music.service.MediaService;
import cn.tony.music.service.MusicPlayerHandler;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class HandleCommandUtil {

    private static final String TAG = "HandleCommandUtil";

    private static HandleCommandUtil instance = null;

    public static HandleCommandUtil getInstance() {
        if (instance == null) {
            instance = new HandleCommandUtil();
        }
        return instance;
    }

    public void handleCommand(Intent intent, MediaService service, MusicPlayerHandler mPlayerHandler) {

        final String action = intent.getAction();
        final String command = IConstants.SERVICECMD.equals(action) ? intent.getStringExtra(IConstants.CMDNAME) : null;

        Logger.info(TAG, "handleCommandIntent: action = " + action + ", command = " + command);

        if (IConstants.DECREASEVOLUME.equals(action)) {
            Bundle bundle = intent.getExtras();
            service.handleVolume(bundle);
        } else if (IConstants.CMDNEXT.equals(command) || IConstants.NEXT_ACTION.equals(action)) {
            service.gotoNext(true);
        } else if (IConstants.CMDPREVIOUS.equals(command) || IConstants.PREVIOUS_ACTION.equals(action)
                || IConstants.PREVIOUS_FORCE_ACTION.equals(action)) {
            service.prev(IConstants.PREVIOUS_FORCE_ACTION.equals(action));
        } else if (IConstants.CMDTOGGLEPAUSE.equals(command) || IConstants.TOGGLEPAUSE_ACTION.equals(action)) {
            if (service.isPlaying()) {
                service.pause();
                service.mPausedByTransientLossOfFocus = false;
            } else {
                service.play();
            }
        } else if (IConstants.CMDPAUSE.equals(command) || IConstants.PAUSE_ACTION.equals(action)) {
            service.pause();
            service.mPausedByTransientLossOfFocus = false;
        } else if (IConstants.CMDPLAY.equals(command)) {
            service.play();
        } else if (IConstants.CMDSTOP.equals(command) || IConstants.STOP_ACTION.equals(action)) {
            service.pause();
            service.mPausedByTransientLossOfFocus = false;
            service.seek(0);
            service.releaseServiceUiAndStop();
        } else if (IConstants.REPEAT_ACTION.equals(action)) {
            service.cycleRepeat();
        } else if (IConstants.SHUFFLE_ACTION.equals(action)) {
            service.cycleShuffle();
        } else if (IConstants.TRY_GET_TRACKINFO.equals(action)) {
//            getLrc(mPlaylist.get(mPlayPos).mId);
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            if (service.isPlaying() && !service.mIsLocked) {
                Intent lockscreen = new Intent(service, LockActivity.class);
                lockscreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                service.startActivity(lockscreen);
            }
        } else if (IConstants.LOCK_SCREEN.equals(action)) {
            service.mIsLocked = intent.getBooleanExtra("islock", true);
            Logger.info(TAG, "isloced = " + service.mIsLocked);
        } else if (IConstants.SEND_PROGRESS.equals(action)) {
            if (service.isPlaying() && !service.mIsSending) {
                mPlayerHandler.post(service.sendDuration);
                service.mIsSending = true;
            } else if (!service.isPlaying()) {
                mPlayerHandler.removeCallbacks(service.sendDuration);
                service.mIsSending = false;
            }

        } else if (IConstants.SETQUEUE.equals(action)) {
            Logger.info(TAG, "action");
            service.setQueuePosition(intent.getIntExtra("position", 0));
        }
    }

}
