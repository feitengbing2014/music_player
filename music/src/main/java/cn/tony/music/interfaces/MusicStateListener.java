package cn.tony.music.interfaces;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/26.
 */

public interface MusicStateListener {

    /**
     * 更新歌曲状态信息
     */
    void updateTrackInfo();

    void updateTime();

    void changeTheme();

    void reloadAdapter();
}
