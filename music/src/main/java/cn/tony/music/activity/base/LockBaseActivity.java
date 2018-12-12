package cn.tony.music.activity.base;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.lang.ref.WeakReference;

import cn.tony.music.MediaAidlInterface;
import cn.tony.music.constent.IConstants;
import cn.tony.music.service.MusicPlayer;

import static cn.tony.music.service.MusicPlayer.mService;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/26.
 */

public class LockBaseActivity extends AppCompatActivity implements ServiceConnection {

    private MusicPlayer.ServiceToken mToken;
    private PlaybackStatus mPlaybackStatus; //receiver 接受播放状态变化等

    /**
     * 更新歌曲状态信息
     */
    public void updateTrackInfo() {
    }


    public void updateTrack() {

    }

    public void updateLrc() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToken = MusicPlayer.bindToService(this, this);
        mPlaybackStatus = new PlaybackStatus(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(IConstants.META_CHANGED);
        f.addAction(IConstants.MUSIC_CHANGED);
        f.addAction(IConstants.LRC_UPDATED);
        registerReceiver(mPlaybackStatus, new IntentFilter(f));
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mService = MediaAidlInterface.Stub.asInterface(service);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
        }
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        mService = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("lock", " on destroy");
        unbindService();
        // Unbind from the service
    }

    public void unbindService() {
        if (mToken != null) {
            MusicPlayer.unbindFromService(mToken);
            mToken = null;
        }
    }


    private final static class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<LockBaseActivity> mReference;


        public PlaybackStatus(final LockBaseActivity activity) {
            mReference = new WeakReference<>(activity);
        }


        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            LockBaseActivity baseActivity = mReference.get();
            if (baseActivity != null) {
                if (action.equals(IConstants.META_CHANGED)) {
                    baseActivity.updateTrackInfo();
                } else if (action.equals(IConstants.MUSIC_CHANGED)) {
                    baseActivity.updateTrack();
                } else if (action.equals(IConstants.LRC_UPDATED)) {
                    baseActivity.updateLrc();
                }

            }
        }
    }
}
