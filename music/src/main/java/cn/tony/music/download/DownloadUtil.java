package cn.tony.music.download;

import android.content.Context;

import cn.tony.music.provider.DownFileStore;

/**
 * Ella Group
 * <p>  下载文件记录
 * <p>
 * Author by Tony, on 2018/11/3.
 */

public class DownloadUtil {

    private static DownloadUtil instance;

    public static DownloadUtil getInstance() {
        if (instance == null) {
            synchronized (DownloadUtil.class) {
                if (instance == null) {
                    instance = new DownloadUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 保存下载信息
     *
     * @param context
     * @param info
     */
    public void setDownloadInfo(Context context, DownloadFileInfo info) {
        if (info == null)
            return;
        DownloadDBEntity dbEntity = new DownloadDBEntity((info.fileUrl).hashCode() + "", info.fileSize,
                info.fileSize, info.fileUrl, info.filePath, info.fileName, "tony", DownloadStatus.DOWNLOAD_STATUS_INIT);
        DownFileStore.getInstance(context).insert(dbEntity);
    }

}
