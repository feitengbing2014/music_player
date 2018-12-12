package cn.tony.music.receiver;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.view.KeyEvent;

import cn.tony.music.constent.IConstants;
import cn.tony.music.service.MediaService;
import cn.tony.music.utils.Logger;

/**
 * Ella Group
 * <p>
 * <p>
 * Used to control headset playback.
 * Single press: pause/resume
 * Double press: next track
 * Triple press: previous track
 * Long press: voice search
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class MediaButtonIntentReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = "ButtonIntentReceiver";

    private static PowerManager.WakeLock mWakeLock = null;
    private static int mClickCounter = 0;
    private static long mLastClickTime = 0;
    private static boolean mDown = false;
    private static boolean mLaunched = false;

    private static Handler mHandler = new Handler() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case IConstants.MSG_LONGPRESS_TIMEOUT:
//                    Logger.info(TAG, "Handling longpress timeout, launched " + mLaunched);
//                    if (!mLaunched) {
//                        final Context context = (Context) msg.obj;
//                        final Intent i = new Intent();
//                        i.setClass(context, MainActivity.class);
//                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        context.startActivity(i);
//                        mLaunched = true;
//                    }
                    break;

                case IConstants.MSG_HEADSET_DOUBLE_CLICK_TIMEOUT:
                    final int clickCount = msg.arg1;
                    final String command;

                    Logger.info(TAG, "Handling headset click, count = " + clickCount);
                    switch (clickCount) {
                        case 1:
                            command = IConstants.CMDTOGGLEPAUSE;
                            break;
                        case 2:
                            command = IConstants.CMDNEXT;
                            break;
                        case 3:
                            command = IConstants.CMDPREVIOUS;
                            break;
                        default:
                            command = null;
                            break;
                    }

                    if (command != null) {
                        final Context context = (Context) msg.obj;
                        startService(context, command);
                    }
                    break;
            }
            releaseWakeLockIfHandlerIdle();
        }
    };

    private static void startService(Context context, String command) {
        final Intent i = new Intent(context, MediaService.class);
        i.setAction(IConstants.SERVICECMD);
        i.putExtra(IConstants.CMDNAME, command);
        i.putExtra(IConstants.FROM_MEDIA_BUTTON, true);
        startWakefulService(context, i);
    }

    private static void acquireWakeLockAndSendMessage(Context context, Message msg, long delay) {
        if (mWakeLock == null) {
            Context appContext = context.getApplicationContext();
            PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicApp:headset button");
            mWakeLock.setReferenceCounted(false);
        }
        Logger.info(TAG, "Acquiring wake lock and sending " + msg.what);
        // Make sure we don't indefinitely hold the wake lock under any circumstances
        mWakeLock.acquire(10000);

        mHandler.sendMessageDelayed(msg, delay);
    }

    private static void releaseWakeLockIfHandlerIdle() {
        if (mHandler.hasMessages(IConstants.MSG_LONGPRESS_TIMEOUT)
                || mHandler.hasMessages(IConstants.MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)) {
            Logger.info(TAG, "Handler still has messages pending, not releasing wake lock");
            return;
        }

        if (mWakeLock != null) {
            Logger.info(TAG, "Releasing wake lock");
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            startService(context, IConstants.CMDPAUSE);
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            final int keycode = event.getKeyCode();
            final int action = event.getAction();
            final long eventtime = event.getEventTime();

            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = IConstants.CMDSTOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = IConstants.CMDTOGGLEPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = IConstants.CMDNEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = IConstants.CMDPREVIOUS;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    command = IConstants.CMDPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    command = IConstants.CMDPLAY;
                    break;
            }
            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mDown) {
                        if (IConstants.CMDTOGGLEPAUSE.equals(command)
                                || IConstants.CMDPLAY.equals(command)) {
                            if (mLastClickTime != 0
                                    && eventtime - mLastClickTime > IConstants.LONG_PRESS_DELAY) {
                                acquireWakeLockAndSendMessage(context,
                                        mHandler.obtainMessage(IConstants.MSG_LONGPRESS_TIMEOUT, context), 0);
                            }
                        }
                    } else if (event.getRepeatCount() == 0) {

                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                            if (eventtime - mLastClickTime >= IConstants.DOUBLE_CLICK) {
                                mClickCounter = 0;
                            }

                            mClickCounter++;
                            Logger.info(TAG, "Got headset click, count = " + mClickCounter);
                            mHandler.removeMessages(IConstants.MSG_HEADSET_DOUBLE_CLICK_TIMEOUT);

                            Message msg = mHandler.obtainMessage(
                                    IConstants.MSG_HEADSET_DOUBLE_CLICK_TIMEOUT, mClickCounter, 0, context);

                            long delay = mClickCounter < 3 ? IConstants.DOUBLE_CLICK : 0;
                            if (mClickCounter >= 3) {
                                mClickCounter = 0;
                            }
                            mLastClickTime = eventtime;
                            acquireWakeLockAndSendMessage(context, msg, delay);
                        } else {
                            startService(context, command);
                        }
                        mLaunched = false;
                        mDown = true;
                    }
                } else {
                    mHandler.removeMessages(IConstants.MSG_LONGPRESS_TIMEOUT);
                    mDown = false;
                }
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }
                releaseWakeLockIfHandlerIdle();
            }
        }
    }
}