package cn.tony.music.activity.base;

import android.app.Activity;
import android.support.v4.app.DialogFragment;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/26.
 */

public class AttachDialogFragment extends DialogFragment {

    public Activity mContext;

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        this.mContext = activity;
    }


}
