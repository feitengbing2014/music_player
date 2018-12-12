package cn.tony.music.utils;

import android.content.Context;

import java.io.File;
import java.util.LinkedList;

import cn.tony.music.provider.MusicPlaybackState;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class RecordUtils {

    public static boolean wasRecentlyUsed(final int idx, int lookbacksize, LinkedList<Integer> mHistory) {
        if (lookbacksize == 0) {
            return false;
        }
        final int histsize = mHistory.size();
        if (histsize < lookbacksize) {
            lookbacksize = histsize;
        }
        final int maxidx = histsize - 1;
        for (int i = 0; i < lookbacksize; i++) {
            final long entry = mHistory.get(maxidx - i);
            if (entry == idx) {
                return true;
            }
        }
        return false;
    }


    public static void clearPlayInfos(Context context) {
        File file = new File(context.getCacheDir().getAbsolutePath() + "playlist");
        if (file.exists()) {
            file.delete();
        }
        MusicPlaybackState.getInstance(context).clearQueue();
    }


}
