package cn.tony.music.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Ella Group
 *
 *  This is used by the music playback service to track the music tracks it is playing
 *  It has extra meta data to determine where the track came from so that we can show the appropriate
 *  song playing indicator
 *
 * Author by Tony, on 2018/10/25.
 */

public class MusicTrack implements Parcelable {

    public long mId;
    public int mSourcePosition;

    public MusicTrack(long mId, int mSourcePosition) {
        this.mId = mId;
        this.mSourcePosition = mSourcePosition;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mId);
        dest.writeInt(this.mSourcePosition);
    }

    public MusicTrack() {
    }

    protected MusicTrack(Parcel in) {
        this.mId = in.readLong();
        this.mSourcePosition = in.readInt();
    }

    public static final Parcelable.Creator<MusicTrack> CREATOR = new Parcelable.Creator<MusicTrack>() {
        @Override
        public MusicTrack createFromParcel(Parcel source) {
            return new MusicTrack(source);
        }

        @Override
        public MusicTrack[] newArray(int size) {
            return new MusicTrack[size];
        }
    };
}
