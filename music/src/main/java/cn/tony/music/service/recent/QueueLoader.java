package cn.tony.music.service.recent;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import cn.tony.music.model.MusicInfo;
import cn.tony.music.model.PlayQueueCursor;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/26.
 */

public class QueueLoader {

    private static PlayQueueCursor mCursor;

    public static ArrayList<MusicInfo> getQueueSongs(Context context) {

        final ArrayList<MusicInfo> mMusicQueues = new ArrayList<>();
        Log.e("queueloader", "created");
        mCursor = new PlayQueueCursor(context);

        while (mCursor.moveToNext()) {
            MusicInfo music = new MusicInfo();
            music.songId = mCursor.getInt(0);
            music.albumName = mCursor.getString(4);
            music.musicName = mCursor.getString(1);
            music.artist = mCursor.getString(2);
            mMusicQueues.add(music);
        }
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        return mMusicQueues;
    }
}
