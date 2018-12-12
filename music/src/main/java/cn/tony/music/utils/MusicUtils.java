package cn.tony.music.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.github.promeg.pinyinhelper.Pinyin;

import java.io.File;

import cn.tony.music.R;
import cn.tony.music.download.DownloadDBEntity;
import cn.tony.music.model.MusicInfo;
import cn.tony.music.provider.DownFileStore;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class MusicUtils {

    private static String[] proj_music = new String[]{
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.SIZE};
    private static String[] proj_album = new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART,
            MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.NUMBER_OF_SONGS, MediaStore.Audio.Albums.ARTIST};
    private static String[] proj_artist = new String[]{
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
            MediaStore.Audio.Artists._ID};
    private static String[] proj_folder = new String[]{MediaStore.Files.FileColumns.DATA};


    public static String getAlbumdata(Context context, long musicid) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj_music, "_id = " + String.valueOf(musicid), null, null);
        if (cursor == null) {
            return null;
        }
        long albumId = -1;
        if (cursor.moveToNext()) {
            albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
        }

        if (albumId != -1) {
            cursor = cr.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, proj_album, MediaStore.Audio.Albums._ID + " = " + String.valueOf(albumId), null, null);
        }
        if (cursor == null) {
            return null;
        }
        String data = "";
        if (cursor.moveToNext()) {
            data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        }
        cursor.close();

        return data;
    }


    public static Uri getAlbumArtUri(long albumId) {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
    }

    public static MusicInfo getMusicInfo(Context context, long id) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj_music, "_id = " + String.valueOf(id), null, null);
        if (cursor == null) {
            return null;
        }
        MusicInfo music = new MusicInfo();
        while (cursor.moveToNext()) {
            music.songId = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media._ID));
            music.albumId = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
            music.albumName = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Albums.ALBUM));
            music.albumData = getAlbumArtUri(music.albumId) + "";
            music.duration = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DURATION));
            music.musicName = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.TITLE));
            music.size = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
            music.artist = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.ARTIST));
            music.artistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));
            String filePath = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DATA));
            music.data = filePath;
            String folderPath = filePath.substring(0,
                    filePath.lastIndexOf(File.separator));
            music.folder = folderPath;
            music.sort = Pinyin.toPinyin(music.musicName.charAt(0)).substring(0, 1).toUpperCase();
        }
        cursor.close();
        return music;
    }


    public static String makeTimeString(long milliSecs) {
        StringBuffer sb = new StringBuffer();
        long m = milliSecs / (60 * 1000);
        sb.append(m < 10 ? "0" + m : m);
        sb.append(":");
        long s = (milliSecs % (60 * 1000)) / 1000;
        sb.append(s < 10 ? "0" + s : s);
        return sb.toString();
    }


    public static String makeShortTimeString(final Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs %= 3600;
        mins = secs / 60;
        secs %= 60;

        final String durationFormat = context.getResources().getString(hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }


    /**
     * 设置下载文件路径
     *
     * @param context
     * @return
     */
    public static String getDownSave(Context context) {
        String cachePath = context.getFilesDir().getPath() + File.separator;
        File userFile = new File(cachePath, "tonymusic");
        if (!userFile.exists())
            userFile.mkdir();
        String userPath = userFile.getAbsolutePath() + File.separator;
        Logger.info("MediaService", "==getCachePath===" + userPath);
        return userPath;
    }


    /**
     * 获取下载的文件路径
     *
     * @param context
     * @param path
     * @return
     */
    public static String getDownFilePath(Context context, String path) {
        DownloadDBEntity entity = DownFileStore.getInstance(context).getDownLoadedList(path.hashCode() + "");
        if (entity != null) {
            path = entity.getSaveDirPath() + entity.getFileName() + ".mp3";
        }
        Logger.info("MediaService", "==getDownFilePath===" + path);
        return path;
    }

}
