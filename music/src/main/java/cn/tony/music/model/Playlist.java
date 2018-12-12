package cn.tony.music.model;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class Playlist {

    public final long id;
    public final String name;
    public final int songCount;
    public final String albumArt;
    public final String author;

    public Playlist() {
        this.id = -1;
        this.name = "";
        this.songCount = -1;
        this.albumArt = "";
        this.author = "";
    }

    public Playlist(long _id, String _name, int _songCount, String _albumArt, String author) {
        this.id = _id;
        this.name = _name;
        this.songCount = _songCount;
        this.albumArt = _albumArt;
        this.author = author;
    }
}
