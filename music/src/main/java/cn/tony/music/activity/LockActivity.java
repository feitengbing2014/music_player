package cn.tony.music.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;

import cn.tony.music.R;
import cn.tony.music.activity.base.LockBaseActivity;
import cn.tony.music.constent.IConstants;
import cn.tony.music.service.MusicPlayer;
import cn.tony.music.sp.PreferencesUtility;
import cn.tony.music.utils.Logger;
import cn.tony.music.views.SildingFinishLayout;

/**
 * Ella Group
 * <p> 锁屏页面
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class LockActivity extends LockBaseActivity implements View.OnClickListener {

    private TextView mMusicName;
    private AppCompatImageView pre, play, next;
    private SildingFinishLayout mView;
    private SimpleDraweeView ivCoaxCover;

    private final String TAG = "LockActivity";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Intent intent = new Intent();
        intent.setAction(IConstants.LOCK_SCREEN);
        sendBroadcast(intent);
        super.onCreate(savedInstanceState);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav
                        // bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        setContentView(R.layout.music_activity_lock);

        mMusicName = findViewById(R.id.lock_music_name);
        pre = findViewById(R.id.lock_music_pre);
        play = findViewById(R.id.lock_music_play);
        next = findViewById(R.id.lock_music_next);
        mView = findViewById(R.id.lock_root);
        ivCoaxCover = findViewById(R.id.ivCoaxCover);
        mView.setOnSildingFinishListener(new SildingFinishLayout.OnSildingFinishListener() {

            @Override
            public void onSildingFinish() {
                finish();
            }
        });
        mView.setTouchView(getWindow().getDecorView());
        play.setOnClickListener(this);


        if (PreferencesUtility.getInstance(this).isHideControl()) {
            pre.setVisibility(View.GONE);
            next.setVisibility(View.GONE);

        } else {
            pre.setVisibility(View.VISIBLE);
            next.setVisibility(View.VISIBLE);

            pre.setOnClickListener(this);
            next.setOnClickListener(this);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        Logger.info(TAG, "onUserLeaveHint");
        super.onUserLeaveHint();

        Intent intent = new Intent();
        intent.setAction(IConstants.LOCK_SCREEN);
        intent.putExtra("islock", false);
        sendBroadcast(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.info(TAG, " on resume");
        updateTrackInfo();
        updateTrack();
    }


    @Override
    protected void onDestroy() {
        Intent intent = new Intent();
        intent.setAction(IConstants.LOCK_SCREEN);
        intent.putExtra("islock", false);
        sendBroadcast(intent);
        super.onDestroy();
        Logger.info(TAG, " on destroy");

    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

    public void updateTrackInfo() {
        mMusicName.setText(MusicPlayer.getTrackName());
        if (MusicPlayer.isPlaying()) {
            play.setImageResource(R.mipmap.sleep_btn_suspend);
        } else {
            play.setImageResource(R.mipmap.sleep_btn_play_2);
        }
    }

    public void updateTrack() {
        try {
            String url = MusicPlayer.getAlbumPath();
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setUri(url)
                    .setAutoPlayAnimations(true)
                    .build();
            ivCoaxCover.setController(controller);

            RoundingParams roundingParams = ivCoaxCover.getHierarchy().getRoundingParams();
            roundingParams.setRoundAsCircle(true);
            ivCoaxCover.getHierarchy().setRoundingParams(roundingParams);

        } catch (Exception e) {
            e.printStackTrace();
            Logger.info(TAG, "------circle-----" + e.getLocalizedMessage());
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.lock_music_pre) {
            MusicPlayer.previous(this, true);
        } else if (id == R.id.lock_music_play) {
            MusicPlayer.playOrPause();
        } else if (id == R.id.lock_music_next) {
            MusicPlayer.next();
        }
    }
}
