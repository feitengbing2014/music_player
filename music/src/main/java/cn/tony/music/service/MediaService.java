package cn.tony.music.service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.audiofx.AudioEffect;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import cn.tony.music.constent.IConstants;
import cn.tony.music.model.MusicInfo;
import cn.tony.music.model.MusicTrack;
import cn.tony.music.model.TrackErrorExtra;
import cn.tony.music.notifications.MusicNotification;
import cn.tony.music.permissions.Nammu;
import cn.tony.music.provider.MediaStoreObserver;
import cn.tony.music.provider.MusicPlaybackState;
import cn.tony.music.provider.RecentStore;
import cn.tony.music.provider.SongPlayCount;
import cn.tony.music.receiver.MediaButtonIntentReceiver;
import cn.tony.music.sp.PreferencesUtility;
import cn.tony.music.utils.AlertTimer;
import cn.tony.music.utils.CommonUtils;
import cn.tony.music.utils.GsonUtil;
import cn.tony.music.utils.HandleCommandUtil;
import cn.tony.music.utils.Logger;
import cn.tony.music.utils.RecordUtils;


/**
 * Ella Group
 * <p> 音乐播放底层实现
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class MediaService extends Service {

    private static final String TAG = "MediaService";

    private static final String[] PROJECTION = new String[]{
            "audio._id AS _id", MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID
    };

    private static final Shuffler mShuffler = new Shuffler();

    private static final String[] PROJECTION_MATRIX = new String[]{
            "_id", MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID
    };


    private IBinder mBinder = new MediaServiceStub(this);

    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(final int focusChange) {
            mPlayerHandler.obtainMessage(IConstants.FOCUSCHANGE, focusChange, 0).sendToTarget();
        }
    };


    public long mLastSeekPos = 0;

    private static LinkedList<Integer> mHistory = new LinkedList<>();
    public MultiPlayer mPlayer;
    private String mFileToPlay;
    public PowerManager.WakeLock mWakeLock;
    private AlarmManager mAlarmManager;
    private PendingIntent mShutdownIntent;
    private boolean mShutdownScheduled;
    private NotificationManager mNotificationManager;
    public Cursor mCursor;
    private Cursor mAlbumCursor;
    private AudioManager mAudioManager;
    private SharedPreferences mPreferences;
    private boolean mServiceInUse = false;
    private boolean mIsSupposedToBePlaying = false;
    private long mLastPlayedTime;
    private int mNotifyMode = IConstants.NOTIFY_MODE_NONE;
    public long mNotificationPostTime = 0;
    private boolean mQueueIsSaveable = true;
    public boolean mPausedByTransientLossOfFocus = false;

    private MediaSession mSession;

    private ComponentName mMediaButtonReceiverComponent;

    private int mCardId;

    public int mPlayPos = -1;

    public int mNextPlayPos = -1;

    private int mOpenFailedCounter = 0;

    private int mMediaMountedCount = 0;

    private int mShuffleMode = IConstants.SHUFFLE_NONE;

    public int mRepeatMode = IConstants.REPEAT_ALL;

    private int mServiceStartId = -1;

    private int nextQueue = 0;

    public ArrayList<MusicTrack> mPlaylist = new ArrayList<>(100);

    private HashMap<Long, MusicInfo> mPlaylistInfo = new HashMap<>();

    private long[] mAutoShuffleList = null;

    private MusicPlayerHandler mPlayerHandler;

    private HandlerThread mHandlerThread;
    private BroadcastReceiver mUnmountReceiver = null;
    private MusicPlaybackState mPlaybackStateStore;
    private boolean mShowAlbumArtOnLockscreen;
    private SongPlayCount mSongPlayCount;
    private RecentStore mRecentStore;
    private int mNotificationId = 1000;

    private ContentObserver mMediaStoreObserver;
    private static Handler mUrlHandler;

    public boolean mIsSending = false;
    public boolean mIsLocked;

    private AlertTimer alertTimer;


    private RequestPlayUrl mRequestUrl;
    private Thread mGetUrlThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            mUrlHandler = new Handler();
            Looper.loop();
        }
    });


    private Handler mainHandler = new Handler(Looper.getMainLooper());


    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            Logger.info(TAG, "onreceive" + intent.toURI());
            HandleCommandUtil.getInstance().handleCommand(intent, MediaService.this, mPlayerHandler);
        }
    };


    public Runnable sendDuration = new Runnable() {
        @Override
        public void run() {
            notifyChange(IConstants.SEND_PROGRESS);
            mPlayerHandler.postDelayed(sendDuration, 1000);
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Logger.info(TAG, "Service bound, intent = " + intent);
        cancelShutdown();
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mIntentReceiver);
        unregisterReceiver(mUnmountReceiver);
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        Logger.info(TAG, "Service unbound");
        mServiceInUse = false;
        saveQueue(true);

        if (mIsSupposedToBePlaying || mPausedByTransientLossOfFocus) {
            return true;

        } else if (mPlaylist.size() > 0 || mPlayerHandler.hasMessages(IConstants.TRACK_ENDED)) {
            scheduleDelayedShutdown();
            return true;
        }
        stopSelf(mServiceStartId);

        return true;
    }


    @Override
    public void onRebind(final Intent intent) {
        cancelShutdown();
        mServiceInUse = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Nammu.init(this);
        }
        mGetUrlThread.start();
        Logger.info(TAG, "Creating service");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // gets a pointer to the playback state store
        mPlaybackStateStore = MusicPlaybackState.getInstance(this);
        mSongPlayCount = SongPlayCount.getInstance(this);
        mRecentStore = RecentStore.getInstance(this);


        mHandlerThread = new HandlerThread(MusicPlayerHandler.class.getSimpleName(),
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        mPlayerHandler = new MusicPlayerHandler(this, mHandlerThread.getLooper());

        mPlayer = new MultiPlayer(this);
        mPlayer.setHandler(mPlayerHandler);


        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaButtonReceiverComponent = new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);

        setUpMediaSession();

        mPreferences = getSharedPreferences(IConstants.MEDIASERVICE_PREFERENCES, 0);
        mCardId = getCardId();

//        registerExternalStorageListener();


        // Initialize the intent filter and each action
        final IntentFilter filter = new IntentFilter();
        filter.addAction(IConstants.SERVICECMD);
        filter.addAction(IConstants.TOGGLEPAUSE_ACTION);
        filter.addAction(IConstants.PAUSE_ACTION);
        filter.addAction(IConstants.STOP_ACTION);
        filter.addAction(IConstants.NEXT_ACTION);
        filter.addAction(IConstants.PREVIOUS_ACTION);
        filter.addAction(IConstants.PREVIOUS_FORCE_ACTION);
        filter.addAction(IConstants.REPEAT_ACTION);
        filter.addAction(IConstants.SHUFFLE_ACTION);
        filter.addAction(IConstants.TRY_GET_TRACKINFO);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(IConstants.LOCK_SCREEN);
        filter.addAction(IConstants.SEND_PROGRESS);
        filter.addAction(IConstants.SETQUEUE);
        filter.addAction(IConstants.DECREASEVOLUME);
        // Attach the broadcast listener
        registerReceiver(mIntentReceiver, filter);

        mMediaStoreObserver = new MediaStoreObserver(mPlayerHandler, this);
        getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI, true, mMediaStoreObserver);
        getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mMediaStoreObserver);

        // Initialize the wake lock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.setReferenceCounted(false);


        Intent shutdownIntent = new Intent(this, MediaService.class);
        shutdownIntent.setAction(IConstants.SHUTDOWN);

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mShutdownIntent = PendingIntent.getService(this, 0, shutdownIntent, 0);

        scheduleDelayedShutdown();

        reloadQueueAfterPermissionCheck();
//        notifyChange(IConstants.QUEUE_CHANGED);
//        notifyChange(IConstants.META_CHANGED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.info(TAG, "Got new intent " + intent + ", startId = " + startId);
        mServiceStartId = startId;
        if (intent != null) {
            String action = intent.getAction();
            if (IConstants.SHUTDOWN.equals(action)) {
                mShutdownScheduled = false;
                releaseServiceUiAndStop();
                return START_NOT_STICKY;
            }
            HandleCommandUtil.getInstance().handleCommand(intent, MediaService.this, mPlayerHandler);
        }

        scheduleDelayedShutdown();

        if (intent != null && intent.getBooleanExtra(IConstants.FROM_MEDIA_BUTTON, false)) {
            MediaButtonIntentReceiver.completeWakefulIntent(intent);
        }
        return START_STICKY;
    }


    //------------------实现aidl MediaServiceStub 方法 ------------------\\

    public void open(HashMap<Long, MusicInfo> infos, long[] list, int position) {
        synchronized (this) {
            mPlaylistInfo = infos;
            Logger.info(TAG, mPlaylistInfo.toString());
            if (mShuffleMode == IConstants.SHUFFLE_AUTO) {
                mShuffleMode = IConstants.SHUFFLE_NORMAL;
            }

            saveQueueToLoacal(infos);

            final long oldId = getAudioId();
            final int listlength = list.length;
            boolean newlist = true;
            if (mPlaylist.size() == listlength) {
                newlist = false;
                for (int i = 0; i < listlength; i++) {
                    if (list[i] != mPlaylist.get(i).mId) {
                        newlist = true;
                        break;
                    }
                }
            }
            if (newlist) {
                addToPlayList(list, -1);
                notifyChange(IConstants.QUEUE_CHANGED);
            }
            if (position >= 0) {
                mPlayPos = position;
            } else {
                mPlayPos = mShuffler.nextInt(mPlaylist.size());
            }

            mHistory.clear();
            openCurrentAndMaybeNext(true, false);
//            openCurrentAndNextPlay(true);
            if (oldId != getAudioId()) {
                notifyChange(IConstants.META_CHANGED);
            }
        }
    }

    private void saveQueueToLoacal(final HashMap<Long, MusicInfo> infos) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (long key : infos.keySet()) {
                    PreferencesUtility.getInstance(MediaService.this).setPlayLink(key, infos.get(key).data);
                }
            }
        }).start();
    }

    public boolean openFile(String path) {
        Logger.info(TAG, "openFile: path = " + path);
        synchronized (this) {
            if (path == null) {
                return false;
            }

            if (mCursor == null) {
                Uri uri = Uri.parse(path);
                boolean shouldAddToPlaylist = true;
                long id = -1;
                try {
                    id = Long.valueOf(uri.getLastPathSegment());
                } catch (NumberFormatException ex) {
                    // Ignore
                }

                if (id != -1 && path.startsWith(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                    updateCursor(uri);

                } else if (id != -1 && path.startsWith(
                        MediaStore.Files.getContentUri("external").toString())) {
                    updateCursor(id);

                } else if (path.startsWith("content://downloads/")) {

                    String mpUri = getValueForDownloadedFile(this, uri, "mediaprovider_uri");
                    Logger.info(TAG, "Downloaded file's MP uri : " + mpUri);
                    if (!TextUtils.isEmpty(mpUri)) {
                        if (openFile(mpUri)) {
                            notifyChange(IConstants.META_CHANGED);
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        updateCursorForDownloadedFile(this, uri);
                        shouldAddToPlaylist = false;
                    }

                } else {
                    String where = MediaStore.Audio.Media.DATA + "=?";
                    String[] selectionArgs = new String[]{path};
                    updateCursor(where, selectionArgs);
                }
                try {
                    if (mCursor != null && shouldAddToPlaylist) {
                        mPlaylist.clear();
                        mPlaylist.add(new MusicTrack(
                                mCursor.getLong(IConstants.IDCOLIDX), -1));
                        notifyChange(IConstants.QUEUE_CHANGED);
                        mPlayPos = 0;
                        mHistory.clear();
                    }
                } catch (final UnsupportedOperationException ex) {
                    // Ignore
                }
            }

            mFileToPlay = path;
            mPlayer.setDataSource(mFileToPlay);
            if (mPlayer.isInitialized()) {
                mOpenFailedCounter = 0;
                return true;
            }

            String trackName = getTrackName();
            if (TextUtils.isEmpty(trackName)) {
                trackName = path;
            }
            sendErrorMessage(trackName);
            stop(true);
            return false;
        }
    }

    public void stop() {
        stop(true);
    }

    public void play() {
        play(true);
    }

    public void pause() {
        Logger.info(TAG, "Pausing playback");
        synchronized (this) {
            mPlayerHandler.removeMessages(IConstants.FADEUP);
            if (mIsSupposedToBePlaying) {
                Intent intent = new Intent(
                        AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
                intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
                intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                sendBroadcast(intent);

                mPlayer.pause();

                setIsSupposedToBePlaying(false, true);
                notifyChange(IConstants.META_CHANGED);
            }
        }
    }


    public void prev(boolean forcePrevious) {

        synchronized (this) {

            nextQueue = 0;

            boolean goPrevious =
                    (position() < IConstants.REWIND_INSTEAD_PREVIOUS_THRESHOLD || forcePrevious);
            if (!forcePrevious) {
                goPrevious = getRepeatMode() != IConstants.REPEAT_CURRENT && goPrevious;
            }
            if (goPrevious) {
                Logger.info(TAG, "Going to previous track");
                int pos = getPreviousPlayPosition(true);

                if (pos < 0) {
                    return;
                }
                Logger.info("AAA", mNextPlayPos + "====cccc=" + pos + "==Pre====" + mPlayPos);
                mNextPlayPos = mPlayPos;
                mPlayPos = pos;
                stop(false);
                openCurrent();
                play(false);
                notifyChange(IConstants.META_CHANGED);
                notifyChange(IConstants.MUSIC_CHANGED);
            } else {
                Logger.info(TAG, "Going to beginning of track");
                seek(0);
                play(false);
            }
        }
    }

    public long seek(long position) {
        if (mPlayer.isInitialized()) {
            if (position < 0) {
                position = 0;
            } else if (position > mPlayer.duration()) {
                position = mPlayer.duration();
            }
            long result = mPlayer.seek(position);
//            notifyChange(IConstants.POSITION_CHANGED);
            return result;
        }
        return -1;
    }

    public void seekRelative(long deltaInMs) {
        synchronized (this) {
            if (mPlayer.isInitialized()) {
                final long newPos = position() + deltaInMs;
                final long duration = duration();
                if (newPos < 0) {
                    prev(true);
                    // seek to the new duration + the leftover position
                    seek(duration() + newPos);
                } else if (newPos >= duration) {
                    gotoNext(true);
                    // seek to the leftover duration
                    seek(newPos - duration);
                } else {
                    seek(newPos);
                }
            }
        }
    }


    public void releaseServiceUiAndStop() {
        if (isPlaying()
                || mPausedByTransientLossOfFocus
                || mPlayerHandler.hasMessages(IConstants.TRACK_ENDED)) {
            return;
        }

        Intent intent = new Intent(IConstants.NOTIFICATION_VIEW_CLEAR);
        sendBroadcast(intent);

        Logger.info(TAG, "Nothing is playing anymore, releasing music_notification");
        cancelNotification();
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mSession.setActive(false);

        if (!mServiceInUse) {
            saveQueue(true);
            stopSelf(mServiceStartId);
        }
    }

    public void cycleRepeat() {
        if (mRepeatMode == IConstants.REPEAT_NONE) {
            setRepeatMode(IConstants.REPEAT_CURRENT);
            if (mShuffleMode != IConstants.SHUFFLE_NONE) {
                setShuffleMode(IConstants.SHUFFLE_NONE);
            }
        } else {
            setRepeatMode(IConstants.REPEAT_NONE);
        }
    }

    public void cycleShuffle() {
        if (mShuffleMode == IConstants.SHUFFLE_NONE) {
            setShuffleMode(IConstants.SHUFFLE_NORMAL);
            if (mRepeatMode == IConstants.REPEAT_CURRENT) {
                setRepeatMode(IConstants.REPEAT_ALL);
            }
        } else if (mShuffleMode == IConstants.SHUFFLE_NORMAL || mShuffleMode == IConstants.SHUFFLE_AUTO) {
            setShuffleMode(IConstants.SHUFFLE_NONE);
        }
    }

    //--------------------------------自定义方法  ---------------------------------\\

    public MusicTrack getCurrentTrack() {
        return getTrack(mPlayPos);
    }

    public synchronized MusicTrack getTrack(int index) {
        if (index >= 0 && index < mPlaylist.size()) {
            return mPlaylist.get(index);
        }

        return null;
    }

    /**
     * 是否要开始播放
     *
     * @param value
     * @param notify
     */
    private void setIsSupposedToBePlaying(boolean value, boolean notify) {
        if (mIsSupposedToBePlaying != value) {
            mIsSupposedToBePlaying = value;


            if (!mIsSupposedToBePlaying) {
                scheduleDelayedShutdown();
                mLastPlayedTime = System.currentTimeMillis();
            }

            if (notify) {
                notifyChange(IConstants.PLAYSTATE_CHANGED);
            }
        }
    }


    public void sendErrorMessage(final String trackName) {
        final Intent i = new Intent(IConstants.TRACK_ERROR);
        i.putExtra(TrackErrorExtra.TRACK_NAME, trackName);
        sendBroadcast(i);
    }


    public void notifyChange(final String what) {
        Logger.info(TAG, "notifyChange: what = " + what);
        if (IConstants.SEND_PROGRESS.equals(what)) {
            final Intent intent = new Intent(what);
            intent.putExtra("position", position());
            intent.putExtra("duration", duration());
            sendStickyBroadcast(intent);
            return;
        }

        // Update the lockscreen controls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            updateMediaSession(what);

        if (what.equals(IConstants.POSITION_CHANGED)) {
            return;
        }

        final Intent intent = new Intent(what);
        intent.putExtra("id", getAudioId());
        intent.putExtra("artist", getArtistName());
        intent.putExtra("album", getAlbumName());
        intent.putExtra("track", getTrackName());
        intent.putExtra("playing", isPlaying());
        intent.putExtra("albumuri", getAlbumPath());
        intent.putExtra("islocal", isTrackLocal());

        sendStickyBroadcast(intent);
        final Intent musicIntent = new Intent(intent);
        musicIntent.setAction(what.replace(IConstants.TIMBER_PACKAGE_NAME, IConstants.MUSIC_PACKAGE_NAME));
        sendStickyBroadcast(musicIntent);
//        if (what.equals(TRACK_PREPARED)) {
//            return;
//        }

        if (what.equals(IConstants.META_CHANGED)) {

            mRecentStore.addSongId(getAudioId());
            mSongPlayCount.bumpSongCount(getAudioId());

        } else if (what.equals(IConstants.QUEUE_CHANGED)) {
            Intent intent1 = new Intent(IConstants.EMPTY_LIST);
            intent.putExtra("showorhide", "show");
            sendBroadcast(intent1);
            saveQueue(true);
            if (isPlaying()) {

                if (mNextPlayPos >= 0 && mNextPlayPos < mPlaylist.size()
                        && getShuffleMode() != IConstants.SHUFFLE_NONE) {
                    setNextTrack(mNextPlayPos);
                } else {
                    setNextTrack();
                }
            }
        } else {
            saveQueue(false);
        }

        if (what.equals(IConstants.PLAYSTATE_CHANGED)) {
            updateNotification();
        }
    }


    private void scheduleDelayedShutdown() {
        Logger.info(TAG, "Scheduling shutdown in " + IConstants.IDLE_DELAY + " ms");
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + IConstants.IDLE_DELAY, mShutdownIntent);
        mShutdownScheduled = true;
    }

    private void cancelShutdown() {
        Logger.info(TAG, "Cancelling delayed shutdown, scheduled = " + mShutdownScheduled);
        if (mShutdownScheduled) {
            mAlarmManager.cancel(mShutdownIntent);
            mShutdownScheduled = false;
        }
    }


    private void updateMediaSession(final String what) {
        int playState = mIsSupposedToBePlaying
                ? PlaybackState.STATE_PLAYING
                : PlaybackState.STATE_PAUSED;

        if (what.equals(IConstants.PLAYSTATE_CHANGED) || what.equals(IConstants.POSITION_CHANGED)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mSession.setPlaybackState(new PlaybackState.Builder()
                        .setState(playState, position(), 1.0f)
                        .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE |
                                PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                        .build());
            }
        } else if (what.equals(IConstants.META_CHANGED) || what.equals(IConstants.QUEUE_CHANGED)) {
            //Bitmap albumArt = ImageLoader.getInstance().loadImageSync(CommonUtils.getAlbumArtUri(getAlbumId()).toString());
            Bitmap albumArt = null;
            if (albumArt != null) {

                Bitmap.Config config = albumArt.getConfig();
                if (config == null) {
                    config = Bitmap.Config.ARGB_8888;
                }
                albumArt = albumArt.copy(config, false);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mSession.setMetadata(new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, getArtistName())
                        .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, getAlbumArtistName())
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, getAlbumName())
                        .putString(MediaMetadata.METADATA_KEY_TITLE, getTrackName())
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, duration())
                        .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, getQueuePosition() + 1)
                        .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, getQueue().length)
                        .putString(MediaMetadata.METADATA_KEY_GENRE, getGenreName())
                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,
                                mShowAlbumArtOnLockscreen ? albumArt : null)
                        .build());

                mSession.setPlaybackState(new PlaybackState.Builder()
                        .setState(playState, position(), 1.0f)
                        .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE |
                                PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                        .build());
            }
        }
    }


    public int getAudioSessionId() {
        synchronized (this) {
            return mPlayer.getAudioSessionId();
        }
    }

    public long position() {
        if (mPlayer.isInitialized() && mPlayer.isTrackPrepared()) {
            try {
                return mPlayer.position();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public int getSecondPosition() {
        if (mPlayer.isInitialized()) {
            return mPlayer.sencondaryPosition;
        }
        return -1;
    }


    private void openCurrent() {
        openCurrentAndMaybeNext(false, false);
    }


    public int getPreviousPlayPosition(boolean removeFromHistory) {
        synchronized (this) {
            if (mShuffleMode == IConstants.SHUFFLE_NORMAL) {

                final int histsize = mHistory.size();
                if (histsize == 0) {
                    return -1;
                }
                final Integer pos = mHistory.get(histsize - 1);
                if (removeFromHistory) {
                    mHistory.remove(histsize - 1);
                }
                return pos.intValue();
            } else {
                if (mPlayPos > 0) {
                    return mPlayPos - 1;
                } else {
                    return mPlaylist.size() - 1;
                }
            }
        }
    }

    public long duration() {
        if (mPlayer.isInitialized() && mPlayer.isTrackPrepared()) {
            return mPlayer.duration();
        }
        return -1;
    }


    public void setPlaying(boolean isPlaying) {
        this.mIsSupposedToBePlaying = isPlaying;
    }

    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    public void gotoNext(final boolean force) {
        Logger.info(TAG, "Going to next track");
        synchronized (this) {
            if (mPlaylist.size() <= 0) {
                Logger.info(TAG, "No play queue");
                scheduleDelayedShutdown();
                return;
            }

            int pos = mNextPlayPos;

            if (pos < 0) {
                pos = getNextPosition(force);
            }

            if (mRepeatMode == IConstants.REPEAT_CURRENT) {
                pos = getNextPosition(true);
            }


            if (pos < 0) {
                setIsSupposedToBePlaying(false, true);
                return;
            }

            stop(false);
            setAndRecordPlayPos(pos);
            openCurrentAndNext();
            play();
            notifyChange(IConstants.META_CHANGED);
            notifyChange(IConstants.MUSIC_CHANGED);
        }
    }

    public void enqueue(final long[] list, final HashMap<Long, MusicInfo> map, final int action) {
        synchronized (this) {
            mPlaylistInfo.putAll(map);
            if (action == IConstants.NEXT && mPlayPos + 1 < mPlaylist.size()) {
                addToPlayList(list, mPlayPos + 1);
                mNextPlayPos = mPlayPos + 1;
                notifyChange(IConstants.QUEUE_CHANGED);
            } else {
                addToPlayList(list, Integer.MAX_VALUE);
                notifyChange(IConstants.QUEUE_CHANGED);
            }

            if (mPlayPos < 0) {
                mPlayPos = 0;
                openCurrentAndNext();
                play();
                notifyChange(IConstants.META_CHANGED);
            }
        }
    }

    public int getQueuePosition() {
        synchronized (this) {
            return mPlayPos;
        }
    }


    public int[] getQueueHistoryList() {
        synchronized (this) {
            int[] history = new int[mHistory.size()];
            for (int i = 0; i < mHistory.size(); i++) {
                history[i] = mHistory.get(i);
            }

            return history;
        }
    }


    public int getQueueHistoryPosition(int position) {
        synchronized (this) {
            if (position >= 0 && position < mHistory.size()) {
                return mHistory.get(position);
            }
        }
        return -1;
    }

    public int getQueueHistorySize() {
        synchronized (this) {
            return mHistory.size();
        }
    }


    public void setQueuePosition(final int index) {
        synchronized (this) {
            stop(false);
            mPlayPos = index;
            openCurrentAndNext();
            play();
            notifyChange(IConstants.META_CHANGED);
            if (mShuffleMode == IConstants.SHUFFLE_AUTO) {
                doAutoShuffleUpdate();
            }
        }
    }

    public long[] getQueue() {
        synchronized (this) {
            final int len = mPlaylist.size();
            final long[] list = new long[len];
            for (int i = 0; i < len; i++) {
                list[i] = mPlaylist.get(i).mId;
            }
            return list;
        }
    }


    private void reloadQueue() {
        int id = mCardId;
        if (mPreferences.contains("cardid")) {
            id = mPreferences.getInt("cardid", ~mCardId);
        }
        if (id == mCardId) {
            mPlaylist = mPlaybackStateStore.getQueue();
            try {
                FileInputStream in = new FileInputStream(new File(getCacheDir().getAbsolutePath() + "playlist"));
                String c = CommonUtils.readTextFromSDcard(in);
                HashMap<Long, MusicInfo> play = GsonUtil.getInstance().fromJson(c, new TypeToken<HashMap<Long, MusicInfo>>() {
                }.getType());
                if (play != null && play.size() > 0) {
                    mPlaylistInfo = play;
                    Logger.info(TAG, mPlaylistInfo.keySet().toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                mPlaylist = mPlaybackStateStore.getQueue();
            }
        }
        if ((mPlaylist.size() == mPlaylistInfo.size()) && mPlaylist.size() > 0) {
            final int pos = mPreferences.getInt("curpos", 0);
            if (pos < 0 || pos >= mPlaylist.size()) {
                mPlaylist.clear();
                return;
            }
            mPlayPos = pos;
            updateCursor(mPlaylist.get(mPlayPos).mId);
            if (mCursor == null) {
                SystemClock.sleep(3000);
                updateCursor(mPlaylist.get(mPlayPos).mId);
            }
            synchronized (this) {
                closeCursor();
                mOpenFailedCounter = 20;
                openCurrentAndNext();
            }

            if (!mPlayer.isInitialized() && isTrackLocal()) {
                mPlaylist.clear();
                return;
            }
            final long seekpos = mPreferences.getLong("seekpos", 0);
            mLastSeekPos = seekpos;
            seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);

            Logger.info(TAG, "restored queue, currently at position "
                    + position() + "/" + duration()
                    + " (requested " + seekpos + ")");


            if (position() == -1 && duration() == -1)
                return;

            int repmode = mPreferences.getInt("repeatmode", IConstants.REPEAT_ALL);
            if (repmode != IConstants.REPEAT_ALL && repmode != IConstants.REPEAT_CURRENT) {
                repmode = IConstants.REPEAT_NONE;
            }
            mRepeatMode = repmode;

            int shufmode = mPreferences.getInt("shufflemode", IConstants.SHUFFLE_NONE);
            if (shufmode != IConstants.SHUFFLE_AUTO && shufmode != IConstants.SHUFFLE_NORMAL) {
                shufmode = IConstants.SHUFFLE_NONE;
            }
            if (shufmode != IConstants.SHUFFLE_NONE) {
                mHistory = mPlaybackStateStore.getHistory(mPlaylist.size());
            }
            if (shufmode == IConstants.SHUFFLE_AUTO) {
                if (!makeAutoShuffleList()) {
                    shufmode = IConstants.SHUFFLE_NONE;
                }
            }
            mShuffleMode = shufmode;
        } else {
            clearPlayInfos();
        }
//        notifyChange(IConstants.MUSIC_CHANGED);
    }


    public int getQueueSize() {
        synchronized (this) {
            return mPlaylist.size();
        }
    }

    public long getQueueItemAtPosition(int position) {
        synchronized (this) {
            if (position >= 0 && position < mPlaylist.size()) {
                return mPlaylist.get(position).mId;
            }
        }
        return -1;
    }

    private void saveQueue(final boolean full) {
        if (!mQueueIsSaveable) {
            return;
        }
        final SharedPreferences.Editor editor = mPreferences.edit();
        if (full) {
            mPlaybackStateStore.saveState(mPlaylist, mShuffleMode != IConstants.SHUFFLE_NONE ? mHistory : null);
            if (mPlaylistInfo.size() > 0) {
                String temp = GsonUtil.getInstance().toJson(mPlaylistInfo);
                try {
                    File file = new File(getCacheDir().getAbsolutePath() + "playlist");
                    RandomAccessFile ra = new RandomAccessFile(file, "rws");
                    ra.write(temp.getBytes());
                    ra.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            editor.putInt("cardid", mCardId);

        }
        editor.putInt("curpos", mPlayPos);
        if (mPlayer.isInitialized()) {
            editor.putLong("seekpos", mPlayer.position());
        }
        editor.putInt("repeatmode", mRepeatMode);
        editor.putInt("shufflemode", mShuffleMode);
        editor.apply();
    }

    public int getShuffleMode() {
        return mShuffleMode;
    }

    public void setShuffleMode(final int shufflemode) {
        synchronized (this) {
            if (mShuffleMode == shufflemode && mPlaylist.size() > 0) {
                return;
            }

            mShuffleMode = shufflemode;
            if (mShuffleMode == IConstants.SHUFFLE_AUTO) {
                if (makeAutoShuffleList()) {
                    mPlaylist.clear();
                    doAutoShuffleUpdate();
                    mPlayPos = 0;
                    openCurrentAndNext();
                    play();
                    notifyChange(IConstants.META_CHANGED);
                    return;
                } else {
                    mShuffleMode = IConstants.SHUFFLE_NONE;
                }
            } else {
                setNextTrack();
            }
            saveQueue(false);
            notifyChange(IConstants.SHUFFLEMODE_CHANGED);
        }
    }

    public int getRepeatMode() {
        return mRepeatMode;
    }

    public void setRepeatMode(final int repeatmode) {
        synchronized (this) {
            mRepeatMode = repeatmode;
            setNextTrack();
            saveQueue(false);
            notifyChange(IConstants.REPEATMODE_CHANGED);
        }
    }

    public int getMediaMountedCount() {
        return mMediaMountedCount;
    }

    public void setLockscreenAlbumArt(boolean enabled) {
        mShowAlbumArtOnLockscreen = enabled;
//        notifyChange(IConstants.META_CHANGED);
    }


    public void cancelTimingImpl() {
        cancelTiming();
    }

    public void timingImpl(int time) {
        cancelTiming();
        handleTime(time, new Intent(IConstants.PAUSE_ACTION));
    }


    /**
     * 跨进程需要主线程
     *
     * @param time
     */
    private void handleTime(final int time, final Intent intent) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                alertTimer = new AlertTimer(MediaService.this, time, intent);
                alertTimer.start();
            }
        });
    }

    public void cancelTiming() {

        if (alertTimer != null) {
            alertTimer.cancel();
            alertTimer = null;
        }

    }


    public void timing(int time) {
        boolean isFirst;
        int recordTime = time;
        if (time > 5 * 60 * 1000) {
            time -= 5 * 60 * 1000;
            isFirst = true;
        } else {
            time -= 100 * 1000;
            isFirst = false;
        }
        Intent intent = new Intent(IConstants.DECREASEVOLUME);
        int index = 0;
        Bundle bundle = new Bundle();
        bundle.putInt("timing", recordTime);
        bundle.putInt("index", index);
        bundle.putBoolean("isFirst", isFirst);
        intent.putExtras(bundle);

        cancelTiming();

        handleTime(time, intent);
    }

    public void exit() {
    }

    private void stop(boolean goToIdle) {
        Logger.info(TAG, "Stopping playback, goToIdle = " + goToIdle);
        if (mPlayer.isInitialized()) {
            mPlayer.stop();
        }
        mFileToPlay = null;
        closeCursor();
        if (goToIdle) {
            setIsSupposedToBePlaying(false, false);
        }
// else {
//            if (CommonUtils.isLollipop())
//                stopForeground(false);
//            else stopForeground(true);
//        }
    }


    public void loading(boolean l) {
        Intent intent = new Intent(IConstants.MUSIC_LODING);
        intent.putExtra("isloading", l);
        sendBroadcast(intent);
    }

    public void sendUpdateBuffer(int progress) {
        Intent intent = new Intent(IConstants.BUFFER_UP);
        intent.putExtra("progress", progress);
        sendBroadcast(intent);
    }

    public void play(boolean createNewNextTrack) {
        int status = mAudioManager.requestAudioFocus(mAudioFocusListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        Logger.info(TAG, "Starting playback: audio focus request status = " + status);

        if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(intent);

        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mSession.setActive(true);
        if (createNewNextTrack) {
            setNextTrack();
        } else {
            setNextTrack(mNextPlayPos);
        }
        if (mPlayer.isTrackPrepared()) {
            final long duration = mPlayer.duration();
            if (mRepeatMode != IConstants.REPEAT_CURRENT && duration > 2000
                    && mPlayer.position() >= duration - 2000) {
                gotoNext(true);
            }
        }

        mPlayer.start();
        mPlayerHandler.removeMessages(IConstants.FADEDOWN);
        mPlayerHandler.sendEmptyMessage(IConstants.FADEUP);
        setIsSupposedToBePlaying(true, true);
        cancelShutdown();
        updateNotification();
        notifyChange(IConstants.META_CHANGED);
    }

    public void setNextTrack() {
        setNextTrack(getNextPosition(false));
    }

    private void setNextTrack(int position) {
        if (position == -1)
            return;
        mNextPlayPos = position;
        Logger.info(TAG, "setNextTrack: next play position = " + mNextPlayPos);
        if (mNextPlayPos >= 0 && mPlaylist != null && mNextPlayPos < mPlaylist.size()) {
            final long id = mPlaylist.get(mNextPlayPos).mId;
            if (mPlaylistInfo.get(id) != null) {
                if (mPlaylistInfo.get(id).islocal) {
                    mPlayer.setNextDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id);
                } else {
                    mPlayer.setNextDataSource(null);
                }

            }
        } else {
            mPlayer.setNextDataSource(null);
        }
    }

    private int getNextPosition(final boolean force) {
        if (mPlaylist == null || mPlaylist.isEmpty()) {
            return -1;
        }
        if (!force && mRepeatMode == IConstants.REPEAT_CURRENT) {
            if (mPlayPos < 0) {
                return 0;
            }
            return mPlayPos;
        } else if (mShuffleMode == IConstants.SHUFFLE_NORMAL) {
            final int numTracks = mPlaylist.size();


            final int[] trackNumPlays = new int[numTracks];
            for (int i = 0; i < numTracks; i++) {
                trackNumPlays[i] = 0;
            }


            final int numHistory = mHistory.size();
            for (int i = 0; i < numHistory; i++) {
                final int idx = mHistory.get(i).intValue();
                if (idx >= 0 && idx < numTracks) {
                    trackNumPlays[idx]++;
                }
            }

            if (mPlayPos >= 0 && mPlayPos < numTracks) {
                trackNumPlays[mPlayPos]++;
            }

            int minNumPlays = Integer.MAX_VALUE;
            int numTracksWithMinNumPlays = 0;
            for (int i = 0; i < trackNumPlays.length; i++) {
                if (trackNumPlays[i] < minNumPlays) {
                    minNumPlays = trackNumPlays[i];
                    numTracksWithMinNumPlays = 1;
                } else if (trackNumPlays[i] == minNumPlays) {
                    numTracksWithMinNumPlays++;
                }
            }


            if (minNumPlays > 0 && numTracksWithMinNumPlays == numTracks
                    && mRepeatMode != IConstants.REPEAT_ALL && !force) {
                return -1;
            }


            int skip = mShuffler.nextInt(numTracksWithMinNumPlays);
            for (int i = 0; i < trackNumPlays.length; i++) {
                if (trackNumPlays[i] == minNumPlays) {
                    if (skip == 0) {
                        return i;
                    } else {
                        skip--;
                    }
                }
            }

            Logger.info(TAG, "Getting the next position resulted did not get a result when it should have");
            return -1;
        } else if (mShuffleMode == IConstants.SHUFFLE_AUTO) {
            doAutoShuffleUpdate();
            return mPlayPos + 1;
        } else {
            if (mPlayPos >= mPlaylist.size() - 1) {
                if (mRepeatMode == IConstants.REPEAT_NONE && !force) {
                    return 0;
                } else if (mRepeatMode == IConstants.REPEAT_ALL || force) {
                    return 0;
                }
                return -1;
            } else {
                return mPlayPos + 1;
            }
        }
    }

    public void moveQueueItem(int from, int to) {
        synchronized (this) {
            if (from >= mPlaylist.size()) {
                from = mPlaylist.size() - 1;
            }
            if (to >= mPlaylist.size()) {
                to = mPlaylist.size() - 1;
            }

            if (from == to) {
                return;
            }
            mPlaylistInfo.remove(mPlaylist.get(from).mId);
            final MusicTrack track = mPlaylist.remove(from);
            if (from < to) {
                mPlaylist.add(to, track);
                if (mPlayPos == from) {
                    mPlayPos = to;
                } else if (mPlayPos >= from && mPlayPos <= to) {
                    mPlayPos--;
                }
            } else if (to < from) {
                mPlaylist.add(to, track);
                if (mPlayPos == from) {
                    mPlayPos = to;
                } else if (mPlayPos >= to && mPlayPos <= from) {
                    mPlayPos++;
                }
            }
            notifyChange(IConstants.QUEUE_CHANGED);
        }
    }

    public void refresh() {
        notifyChange(IConstants.REFRESH);
    }

    public void playlistChanged() {
        notifyChange(IConstants.PLAYLIST_CHANGED);
    }

    /**
     * 更新随机播放
     */
    private void doAutoShuffleUpdate() {
        boolean notify = false;
        if (mPlayPos > 10) {
            removeTracks(0, mPlayPos - 9);
            notify = true;
        }
        final int toAdd = 7 - (mPlaylist.size() - (mPlayPos < 0 ? -1 : mPlayPos));
        for (int i = 0; i < toAdd; i++) {
            int lookback = mHistory.size();
            int idx = -1;
            while (true) {
                idx = mShuffler.nextInt(mAutoShuffleList.length);
                if (!RecordUtils.wasRecentlyUsed(idx, lookback, mHistory)) {
                    break;
                }
                lookback /= 2;
            }
            mHistory.add(idx);
            if (mHistory.size() > IConstants.MAX_HISTORY_SIZE) {
                mHistory.remove(0);
            }
            mPlaylist.add(new MusicTrack(mAutoShuffleList[idx], -1));
            notify = true;
        }
        if (notify) {
            notifyChange(IConstants.QUEUE_CHANGED);
        }
    }


//    private void openCurrentAndNextPlay(boolean play) {
//        openCurrentAndMaybeNext(play, true);
//    }

    public void openCurrentAndNext() {
        openCurrentAndMaybeNext(false, true);
    }

    /**
     * 预加载歌曲
     *
     * @param play
     * @param openNext
     */
    private void openCurrentAndMaybeNext(final boolean play, final boolean openNext) {
        //TODO
        synchronized (this) {
            Logger.info(TAG, "open current");
            closeCursor();
            stop(false);
            boolean shutdown = false;

            if (mPlaylist.size() == 0 || mPlaylistInfo.size() == 0 && mPlayPos >= mPlaylist.size()) {
                clearPlayInfos();
                return;
            }
            final long id = mPlaylist.get(mPlayPos).mId;
            updateCursor(id);
            getLrc(id);
            if (mPlaylistInfo.get(id) == null) {
                return;
            }
            if (!mPlaylistInfo.get(id).islocal) {
                if (mRequestUrl != null) {
                    mRequestUrl.stop();
                    mUrlHandler.removeCallbacks(mRequestUrl);
                }
                mRequestUrl = new RequestPlayUrl(id, play, MediaService.this, mPlayer);
                mUrlHandler.postDelayed(mRequestUrl, 70);

            } else {
                while (true) {
                    if (mCursor != null
                            && openFile(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/"
                            + mCursor.getLong(IConstants.IDCOLIDX))) {
                        break;
                    }

                    closeCursor();
                    if (mOpenFailedCounter++ < 10 && mPlaylist.size() > 1) {
                        final int pos = getNextPosition(false);
                        if (pos < 0) {
                            shutdown = true;
                            break;
                        }
                        mPlayPos = pos;
                        stop(false);
                        mPlayPos = pos;
                        updateCursor(mPlaylist.get(mPlayPos).mId);
                    } else {
                        mOpenFailedCounter = 0;
                        Logger.info(TAG, "Failed to open file for playback");
                        shutdown = true;
                        break;
                    }
                }
            }

            if (shutdown) {
                scheduleDelayedShutdown();
                if (mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = false;
                    notifyChange(IConstants.PLAYSTATE_CHANGED);
                }
            } else if (openNext) {
                setNextTrack();
            }
        }
    }


    public HashMap<Long, MusicInfo> getPlayinfos() {
        synchronized (this) {
            return mPlaylistInfo;
        }
    }

    private void clearPlayInfos() {
        RecordUtils.clearPlayInfos(this);
    }


    public int removeTrack(final long id) {
        int numremoved = 0;
        synchronized (this) {
            for (int i = 0; i < mPlaylist.size(); i++) {
                if (mPlaylist.get(i).mId == id) {
                    numremoved += removeTracksInternal(i, i);
                    i--;
                }
            }
            mPlaylistInfo.remove(id);
        }
        if (numremoved > 0) {
            notifyChange(IConstants.QUEUE_CHANGED);
        }
        return numremoved;
    }

    public int removeTracks(final int first, final int last) {
        final int numremoved = removeTracksInternal(first, last);
        if (numremoved > 0) {
            notifyChange(IConstants.QUEUE_CHANGED);
        }
        return numremoved;
    }

    private int removeTracksInternal(int first, int last) {
        synchronized (this) {
            if (last < first) {
                return 0;
            } else if (first < 0) {
                first = 0;
            } else if (last >= mPlaylist.size()) {
                last = mPlaylist.size() - 1;
            }

            boolean gotonext = false;
            if (first <= mPlayPos && mPlayPos <= last) {
                mPlayPos = first;
                gotonext = true;
            } else if (mPlayPos > last) {
                mPlayPos -= last - first + 1;
            }
            final int numToRemove = last - first + 1;

            if (first == 0 && last == mPlaylist.size() - 1) {
                mPlayPos = -1;
                mNextPlayPos = -1;
                mPlaylist.clear();
                mHistory.clear();
            } else {
                for (int i = 0; i < numToRemove; i++) {
                    mPlaylistInfo.remove(mPlaylist.get(first).mId);
                    mPlaylist.remove(first);

                }

                ListIterator<Integer> positionIterator = mHistory.listIterator();
                while (positionIterator.hasNext()) {
                    int pos = positionIterator.next();
                    if (pos >= first && pos <= last) {
                        positionIterator.remove();
                    } else if (pos > last) {
                        positionIterator.set(pos - numToRemove);
                    }
                }
            }
            if (gotonext) {
                if (mPlaylist.size() == 0) {
                    stop(true);
                    mPlayPos = -1;
                    closeCursor();
                } else {
                    if (mShuffleMode != IConstants.SHUFFLE_NONE) {
                        mPlayPos = getNextPosition(true);
                    } else if (mPlayPos >= mPlaylist.size()) {
                        mPlayPos = 0;
                    }
                    final boolean wasPlaying = isPlaying();
                    stop(false);
                    openCurrentAndNext();
                    if (wasPlaying) {
                        play();
                    }
                }
                notifyChange(IConstants.META_CHANGED);
            }
            return last - first + 1;
        }
    }


    public boolean removeTrackAtPosition(final long id, final int position) {
        synchronized (this) {
            if (position >= 0 &&
                    position < mPlaylist.size() &&
                    mPlaylist.get(position).mId == id) {
                mPlaylistInfo.remove(id);
                return removeTracks(position, position) > 0;
            }

        }
        return false;
    }

    private void addToPlayList(final long[] list, int position) {
        final int addlen = list.length;
        if (position < 0) {
            mPlaylist.clear();
            position = 0;
        }

        mPlaylist.ensureCapacity(mPlaylist.size() + addlen);
        if (position > mPlaylist.size()) {
            position = mPlaylist.size();
        }

        final ArrayList<MusicTrack> arrayList = new ArrayList<MusicTrack>(addlen);
        for (int i = 0; i < list.length; i++) {
            arrayList.add(new MusicTrack(list[i], i));
        }

        mPlaylist.addAll(position, arrayList);

        if (mPlaylist.size() == 0) {
            closeCursor();
            notifyChange(IConstants.META_CHANGED);
        }
    }


    public void setAndRecordPlayPos(int nextPos) {
        synchronized (this) {

            if (mShuffleMode != IConstants.SHUFFLE_NONE) {
                mHistory.add(mPlayPos);
                if (mHistory.size() > IConstants.MAX_HISTORY_SIZE) {
                    mHistory.remove(0);
                }
            }

            mPlayPos = nextPos;
        }
    }


    public boolean recentlyPlayed() {
        return isPlaying() || System.currentTimeMillis() - mLastPlayedTime < IConstants.IDLE_DELAY;
    }

    /**
     * 更新通知
     */
    public void updateNotification() {
        final int newNotifyMode;
        if (isPlaying()) {
            newNotifyMode = IConstants.NOTIFY_MODE_FOREGROUND;
        } else if (recentlyPlayed()) {
            newNotifyMode = IConstants.NOTIFY_MODE_BACKGROUND;
        } else {
            newNotifyMode = IConstants.NOTIFY_MODE_NONE;
        }

        // int mNotificationId = hashCode();

        if (mNotifyMode != newNotifyMode) {
            if (mNotifyMode == IConstants.NOTIFY_MODE_FOREGROUND) {
                if (CommonUtils.isLollipop())
                    stopForeground(newNotifyMode == IConstants.NOTIFY_MODE_NONE);
                else
                    stopForeground(newNotifyMode == IConstants.NOTIFY_MODE_NONE || newNotifyMode == IConstants.NOTIFY_MODE_BACKGROUND);
            } else if (newNotifyMode == IConstants.NOTIFY_MODE_NONE) {
                mNotificationManager.cancel(mNotificationId);
                mNotificationPostTime = 0;
            }
        }

        if (newNotifyMode == IConstants.NOTIFY_MODE_FOREGROUND) {
            startForeground(mNotificationId, MusicNotification.getInstance().getNotification(this));

        } else if (newNotifyMode == IConstants.NOTIFY_MODE_BACKGROUND) {
            mNotificationManager.notify(mNotificationId, MusicNotification.getInstance().getNotification(this));
        }

        mNotifyMode = newNotifyMode;
    }


    /**
     * 取消通知
     */
    private void cancelNotification() {
        stopForeground(true);
        //mNotificationManager.cancel(hashCode());
        mNotificationManager.cancel(mNotificationId);
        mNotificationPostTime = 0;
        mNotifyMode = IConstants.NOTIFY_MODE_NONE;
    }


    public boolean isTrackLocal() {
        synchronized (this) {
            MusicInfo info = mPlaylistInfo.get(getAudioId());
            if (info == null) {
                return true;
            }
            return info.islocal;
        }
    }

    public String getAlbumPath() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE));
        }
    }


    public String[] getAlbumPathAll() {
        synchronized (this) {
            try {
                int len = mPlaylistInfo.size();
                String[] albums = new String[len];
                long[] queue = getQueue();
                for (int i = 0; i < len; i++) {
                    albums[i] = mPlaylistInfo.get(queue[i]).albumData;
                }
                return albums;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new String[]{};
        }
    }

    public long getAlbumId() {
        synchronized (this) {
            if (mCursor == null) {
                return -1;
            }
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
        }
    }

    public String getAlbumName() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
        }
    }

    public String getAlbumArtistName() {
        synchronized (this) {
            if (mAlbumCursor == null) {
                return null;
            }
            return mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ARTIST));
        }
    }

    public long getAudioId() {
        MusicTrack track = getCurrentTrack();
        if (track != null) {
            return track.mId;
        }
        return -1;
    }

    public long getNextAudioId() {
        synchronized (this) {
            if (mNextPlayPos >= 0 && mNextPlayPos < mPlaylist.size() && mPlayer.isInitialized()) {
                return mPlaylist.get(mNextPlayPos).mId;
            }
        }
        return -1;
    }


    public long getPreviousAudioId() {
        synchronized (this) {
            if (mPlayer.isInitialized()) {
                int pos = getPreviousPlayPosition(false);
                if (pos >= 0 && pos < mPlaylist.size()) {
                    return mPlaylist.get(pos).mId;
                }
            }
        }
        return -1;
    }


    public String getArtistName() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
        }
    }

    public String getTrackName() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
        }
    }

    public String getPath() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA));
        }
    }


    public String getGenreName() {
        return null;
//        synchronized (this) {
//            if (mCursor == null || mPlayPos < 0 || mPlayPos >= mPlaylist.size()) {
//                return null;
//            }
//            String[] genreProjection = {MediaStore.Audio.Genres.NAME};
//            Uri genreUri = MediaStore.Audio.Genres.getContentUriForAudioId("external",
//                    (int) mPlaylist.get(mPlayPos).mId);
//            Cursor genreCursor = getContentResolver().query(genreUri, genreProjection,
//                    null, null, null);
//            if (genreCursor != null) {
//                try {
//                    if (genreCursor.moveToFirst()) {
//                        return genreCursor.getString(
//                                genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME));
//                    }
//                } finally {
//                    genreCursor.close();
//                }
//            }
//            return null;
//        }
    }


    private void getLrc(long id) {
    }


    //----------------------------------数据库操作----------------------------------\\
    private synchronized void closeCursor() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        if (mAlbumCursor != null) {
            mAlbumCursor.close();
            mAlbumCursor = null;
        }
    }

    public void updateCursor(final long trackId) {
        MusicInfo info = mPlaylistInfo.get(trackId);
        if (mPlaylistInfo.get(trackId) != null) {
            MatrixCursor cursor = new MatrixCursor(PROJECTION);
            cursor.addRow(new Object[]{info.songId, info.artist, info.albumName, info.musicName
                    , info.data, info.albumData, info.albumId, info.artistId});
            cursor.moveToFirst();
            mCursor = cursor;
            cursor.close();
        }
    }

    private void updateCursor(final String selection, final String[] selectionArgs) {
        synchronized (this) {
            closeCursor();
            mCursor = openCursorAndGoToFirst(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, selection, selectionArgs);
        }
    }

    private void updateCursor(final Uri uri) {
        synchronized (this) {
            closeCursor();
            mCursor = openCursorAndGoToFirst(uri, PROJECTION, null, null);
        }
    }

    private String getValueForDownloadedFile(Context context, Uri uri, String column) {
        Cursor cursor = null;
        final String[] projection = {
                column
        };
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private void updateCursorForDownloadedFile(Context context, Uri uri) {
        synchronized (this) {
            closeCursor();
            MatrixCursor cursor = new MatrixCursor(PROJECTION_MATRIX);
            String title = getValueForDownloadedFile(this, uri, "title");
            cursor.addRow(new Object[]{
                    null,
                    null,
                    null,
                    title,
                    null,
                    null,
                    null,
                    null
            });
            mCursor = cursor;
            mCursor.moveToFirst();
        }
    }

    private Cursor openCursorAndGoToFirst(Uri uri, String[] projection,
                                          String selection, String[] selectionArgs) {
        Cursor c = getContentResolver().query(uri, projection,
                selection, selectionArgs, null);
        if (c == null) {
            return null;
        }
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }


    private int getmCardId() {
        final ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(Uri.parse("content://media/external/fs_id"), null, null,
                null, null);
        int mCardId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            mCardId = cursor.getInt(0);
            cursor.close();
        }
        return mCardId;
    }

    public long getArtistId() {
        synchronized (this) {
            if (mCursor == null) {
                return -1;
            }
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST_ID));
        }
    }

    private boolean makeAutoShuffleList() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{
                            MediaStore.Audio.Media._ID
                    }, MediaStore.Audio.Media.IS_MUSIC + "=1", null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return false;
            }
            final int len = cursor.getCount();
            final long[] list = new long[len];
            for (int i = 0; i < len; i++) {
                cursor.moveToNext();
                list[i] = cursor.getLong(0);
            }
            mAutoShuffleList = list;
            return true;
        } catch (final RuntimeException e) {
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return false;
    }


    private void setUpMediaSession() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSession = new MediaSession(this, IConstants.MUSIC_TAG);
            mSession.setCallback(new MediaSession.Callback() {
                @Override
                public void onPause() {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                }

                @Override
                public void onPlay() {
                    play();
                }

                @Override
                public void onSeekTo(long pos) {
                    seek(pos);
                }

                @Override
                public void onSkipToNext() {
                    gotoNext(true);
                }

                @Override
                public void onSkipToPrevious() {
                    prev(false);
                }

                @Override
                public void onStop() {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                    seek(0);
                    releaseServiceUiAndStop();
                }
            });
            mSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        }
    }


    private int getCardId() {
        if (CommonUtils.isMarshmallow()) {
            if (Nammu.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                return getmCardId();
            } else return 0;
        } else {
            return getmCardId();
        }
    }

//    public void registerExternalStorageListener() {
//        if (mUnmountReceiver == null) {
//            mUnmountReceiver = new BroadcastReceiver() {
//
//                @Override
//                public void onReceive(final Context context, final Intent intent) {
//                    final String action = intent.getAction();
//                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
//                        saveQueue(true);
//                        mQueueIsSaveable = false;
//                        closeExternalStorageFiles(intent.getData().getPath());
//                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
//                        mMediaMountedCount++;
//                        mCardId = getCardId();
//                        reloadQueueAfterPermissionCheck();
//                        mQueueIsSaveable = true;
//                        notifyChange(IConstants.QUEUE_CHANGED);
//                        notifyChange(IConstants.META_CHANGED);
//                    }
//                }
//            };
//            final IntentFilter filter = new IntentFilter();
//            filter.addAction(Intent.ACTION_MEDIA_EJECT);
//            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
//            filter.addDataScheme("file");
//            registerReceiver(mUnmountReceiver, filter);
//        }
//    }

    public void closeExternalStorageFiles(final String storagePath) {
//        stop(true);
//        notifyChange(IConstants.QUEUE_CHANGED);
//        notifyChange(IConstants.META_CHANGED);
    }

    private void reloadQueueAfterPermissionCheck() {
        if (CommonUtils.isMarshmallow()) {
            if (Nammu.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                reloadQueue();
            }
        } else {
            reloadQueue();
        }
    }

    public void handleVolume(Bundle bundle) {
        boolean isFirst = bundle.getBoolean("isFirst");
        Intent intent = new Intent(IConstants.DECREASEVOLUME);
        int index = bundle.getInt("index");
        int timeLeft = bundle.getInt("timing");
        Logger.info("VVV", timeLeft + "-before--index-----" + index);
        if (timeLeft <= 19000) {
            Intent intentPause = new Intent(IConstants.PAUSE_ACTION);
            sendBroadcast(intentPause);
            return;
        }
        index++;
        bundle.putInt("index", index);

        int interTime = 10 * 1000;
        if (isFirst) {
            bundle.putBoolean("isFirst", false);
            interTime = 5 * 60 * 1000 - 100 * 1000;
            timeLeft = interTime;
        } else {
            timeLeft -= interTime;
        }

        bundle.putInt("timing", timeLeft);
        intent.putExtras(bundle);
        cancelTiming();
        handleTime(interTime, intent);
        Logger.info("VVV", timeLeft + "-after--index-----" + index);
        setVolume(isFirst);

    }


    private void setVolume(boolean isFirst) {
        if (!isFirst && PreferencesUtility.getInstance(this).isInteruptVolume())
            return;
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float fVolume;
        int volume;
        if (isFirst) {
            fVolume = (float) (currentVolume * 0.7);
            volume = (int) fVolume;
        } else {
            fVolume = (float) (currentVolume * 0.9);
            volume = (int) fVolume;
        }
        Logger.info("VVV", fVolume + "==+==" + volume + "==+==" + currentVolume);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND);
    }


    /**
     * 是否处于倒计时状态
     *
     * @return
     */
    public boolean isCountDownState() {
        return alertTimer != null;
    }
}
