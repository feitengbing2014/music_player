package cn.tony.music.download;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Ella Group
 * <p> 下载实体类
 * <p>
 * Author by Tony, on 2018/11/3.
 */

public class DownloadFileInfo implements Parcelable {

    public String fileName;//文件名

    public String fileUrl;//文件下载url

    public long fileSize;//文件大小

    public String filePath;//文件保存路径

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fileName);
        dest.writeString(this.fileUrl);
        dest.writeLong(this.fileSize);
        dest.writeString(this.filePath);
    }

    public DownloadFileInfo() {
    }

    protected DownloadFileInfo(Parcel in) {
        this.fileName = in.readString();
        this.fileUrl = in.readString();
        this.fileSize = in.readLong();
        this.filePath = in.readString();
    }

    public static final Parcelable.Creator<DownloadFileInfo> CREATOR = new Parcelable.Creator<DownloadFileInfo>() {
        @Override
        public DownloadFileInfo createFromParcel(Parcel source) {
            return new DownloadFileInfo(source);
        }

        @Override
        public DownloadFileInfo[] newArray(int size) {
            return new DownloadFileInfo[size];
        }
    };
}
