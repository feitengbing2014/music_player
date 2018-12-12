package cn.tony.music.activity.base;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import cn.tony.music.MediaAidlInterface;
import cn.tony.music.R;
import cn.tony.music.constent.IConstants;
import cn.tony.music.interfaces.MusicStateListener;
import cn.tony.music.model.TrackErrorExtra;
import cn.tony.music.service.MusicPlayer;
import cn.tony.music.utils.Logger;

import static cn.tony.music.service.MusicPlayer.mService;


/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/26.
 */

public abstract class BaseActivity extends AppCompatActivity implements ServiceConnection {

    private String TAG = "BaseActivity";

    private MusicPlayer.ServiceToken mToken;
    private PlaybackStatus mPlaybackStatus; //receiver 接受播放状态变化等
    //    private QuickControlsFragment fragment; //底部播放控制栏
    private ArrayList<MusicStateListener> mMusicListener = new ArrayList<>();


    /**
     * 更新播放队列
     */
    public void updateQueue() {

    }

    /**
     * 更新歌曲状态信息
     */
    public void updateTrackInfo() {
        for (final MusicStateListener listener : mMusicListener) {
            if (listener != null) {
                listener.reloadAdapter();
                listener.updateTrackInfo();
            }
        }
    }

    /**
     * fragment界面刷新
     */
    public void refreshUI() {
        for (final MusicStateListener listener : mMusicListener) {
            if (listener != null) {
                listener.reloadAdapter();
            }
        }

    }

    public void updateTime() {
        for (final MusicStateListener listener : mMusicListener) {
            if (listener != null) {
                listener.updateTime();
            }
        }
    }

    /**
     * 歌曲切换
     */
    public void updateTrack() {

    }

    /**
     * @param p 更新歌曲缓冲进度值，p取值从0~100
     */
    public void updateBuffer(int p) {

    }

    public void changeTheme() {
        for (final MusicStateListener listener : mMusicListener) {
            if (listener != null) {
                listener.changeTheme();
            }
        }
    }

    /**
     * @param isLoading 歌曲是否加载中
     */
    public void loading(boolean isLoading) {

    }


    /**
     * @param outState 取消保存状态
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * @param savedInstanceState 取消保存状态
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }


    public abstract void showQuickControl(boolean show);

//    /**
//     * @param show 显示或关闭底部播放控制栏
//     */
//    protected void showQuickControl(boolean show) {
//        Logger.info(TAG, MusicPlayer.getQueue().length + "");
//        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        if (show) {
//            if (fragment == null) {
//                fragment = QuickControlsFragment.newInstance();
//                ft.add(R.id.bottom_container, fragment).commitAllowingStateLoss();
//            } else {
//                ft.show(fragment).commitAllowingStateLoss();
//            }
//        } else {
//            if (fragment != null)
//                ft.hide(fragment).commitAllowingStateLoss();
//        }
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToken = MusicPlayer.bindToService(this, this);
        mPlaybackStatus = new PlaybackStatus(this);
//
        IntentFilter f = new IntentFilter();
        f.addAction(IConstants.PLAYSTATE_CHANGED);
        f.addAction(IConstants.META_CHANGED);
        f.addAction(IConstants.QUEUE_CHANGED);
        f.addAction(IConstants.MUSIC_COUNT_CHANGED);
        f.addAction(IConstants.TRACK_PREPARED);
        f.addAction(IConstants.BUFFER_UP);
        f.addAction(IConstants.EMPTY_LIST);
        f.addAction(IConstants.MUSIC_CHANGED);
        f.addAction(IConstants.LRC_UPDATED);
        f.addAction(IConstants.PLAYLIST_COUNT_CHANGED);
        f.addAction(IConstants.MUSIC_LODING);
        registerReceiver(mPlaybackStatus, new IntentFilter(f));
        showQuickControl(true);
    }


    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mService = MediaAidlInterface.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        mService = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        unbindService();
        try {
            unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
        }
        mMusicListener.clear();

    }

    public void unbindService() {
        if (mToken != null) {
            MusicPlayer.unbindFromService(mToken);
            mToken = null;
        }
    }

    public void setMusicStateListenerListener(final MusicStateListener status) {
        if (status == this) {
            throw new UnsupportedOperationException("Override the method, don't add a listener");
        }

        if (status != null) {
            mMusicListener.add(status);
        }
    }

    public void removeMusicStateListenerListener(final MusicStateListener status) {
        if (status != null) {
            mMusicListener.remove(status);
        }
    }


    private class PlaybackStatus extends BroadcastReceiver {

        private WeakReference<BaseActivity> mReference;

        public PlaybackStatus(BaseActivity activity) {
            mReference = new WeakReference<>(activity);
        }


        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            BaseActivity baseActivity = mReference.get();
            if (baseActivity != null) {
                if (action.equals(IConstants.META_CHANGED)) {
                    baseActivity.updateTrackInfo();

                } else if (action.equals(IConstants.PLAYSTATE_CHANGED)) {

                } else if (action.equals(IConstants.TRACK_PREPARED)) {
                    baseActivity.updateTime();
                } else if (action.equals(IConstants.BUFFER_UP)) {
                    baseActivity.updateBuffer(intent.getIntExtra("progress", 0));
                } else if (action.equals(IConstants.MUSIC_LODING)) {
                    baseActivity.loading(intent.getBooleanExtra("isloading", false));
                } else if (action.equals(IConstants.REFRESH)) {

                } else if (action.equals(IConstants.MUSIC_COUNT_CHANGED)) {
                    baseActivity.refreshUI();
                } else if (action.equals(IConstants.PLAYLIST_COUNT_CHANGED)) {
                    baseActivity.refreshUI();
                } else if (action.equals(IConstants.QUEUE_CHANGED)) {
                    baseActivity.updateQueue();
                } else if (action.equals(IConstants.TRACK_ERROR)) {
                    final String errorMsg = String.format("%s%s", context.getString(R.string.exit),
                            intent.getStringExtra(TrackErrorExtra.TRACK_NAME));
                    Toast.makeText(baseActivity, errorMsg, Toast.LENGTH_SHORT).show();
                } else if (action.equals(IConstants.MUSIC_CHANGED)) {
                    baseActivity.updateTrack();
                }

            }
        }
    }
}
