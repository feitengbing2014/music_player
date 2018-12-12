package cn.tony.music.activity.base;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import cn.tony.music.interfaces.MusicStateListener;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/26.
 */

public class BaseFragment extends Fragment implements MusicStateListener {

    public Activity mContext;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseActivity) getActivity()).setMusicStateListenerListener(this);
        reloadAdapter();
    }

    @Override
    public void onStop() {
        super.onStop();
        ((BaseActivity) getActivity()).removeMusicStateListenerListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void updateTrackInfo() {

    }

    @Override
    public void updateTime() {

    }

    @Override
    public void changeTheme() {

    }

    @Override
    public void reloadAdapter() {

    }


}
