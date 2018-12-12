package cn.tony.music.download;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public interface DownloadStatus {

    int DOWNLOAD_STATUS_INIT = -1;
    int DOWNLOAD_STATUS_PREPARE = 0;
    int DOWNLOAD_STATUS_START = 1;
    int DOWNLOAD_STATUS_DOWNLOADING = 2;
    int DOWNLOAD_STATUS_CANCEL = 3;
    int DOWNLOAD_STATUS_ERROR = 4;
    int DOWNLOAD_STATUS_COMPLETED = 5;
    int DOWNLOAD_STATUS_PAUSE = 6;
}
