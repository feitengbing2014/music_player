package cn.tony.music.service;

import android.os.RemoteException;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import cn.tony.music.MediaAidlInterface;
import cn.tony.music.model.MusicInfo;
import cn.tony.music.model.MusicTrack;

/**
 * Ella Group
 * <p>  音乐播放底层实现
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class MediaServiceStub extends MediaAidlInterface.Stub {

    private WeakReference<MediaService> mService;

    public MediaServiceStub(MediaService service) {
        mService = new WeakReference<>(service);
    }


    @Override
    public void openFile(String path) throws RemoteException {
        mService.get().openFile(path);
    }

    @Override
    public void open(Map infos, long[] list, int position) throws RemoteException {
        mService.get().open((HashMap<Long, MusicInfo>) infos, list, position);

    }

    @Override
    public void stop() throws RemoteException {
        mService.get().stop();
    }

    @Override
    public void pause() throws RemoteException {
        mService.get().pause();
    }

    @Override
    public void play() throws RemoteException {
        mService.get().play();
    }

    @Override
    public void prev(boolean forcePrevious) throws RemoteException {
        mService.get().prev(forcePrevious);
    }

    @Override
    public void next() throws RemoteException {
        mService.get().gotoNext(true);
    }

    @Override
    public void enqueue(long[] list, Map infos, int action) throws RemoteException {
        mService.get().enqueue(list, (HashMap<Long, MusicInfo>) infos, action);
    }


    @Override
    public HashMap getPlayinfos() throws RemoteException {
        return mService.get().getPlayinfos();
    }

    @Override
    public void setQueuePosition(int index) throws RemoteException {
        mService.get().setQueuePosition(index);
    }

    @Override
    public void setShuffleMode(int shufflemode) throws RemoteException {
        mService.get().setShuffleMode(shufflemode);
    }

    @Override
    public void setRepeatMode(int repeatmode) throws RemoteException {
        mService.get().setRepeatMode(repeatmode);
    }

    @Override
    public void moveQueueItem(int from, int to) throws RemoteException {
        mService.get().moveQueueItem(from, to);
    }

    @Override
    public void refresh() throws RemoteException {
        mService.get().refresh();
    }

    @Override
    public void playlistChanged() throws RemoteException {
        mService.get().playlistChanged();
    }

    @Override
    public boolean isPlaying() throws RemoteException {
        return mService.get().isPlaying();
    }

    @Override
    public long[] getQueue() throws RemoteException {
        return mService.get().getQueue();
    }

    @Override
    public long getQueueItemAtPosition(int position) throws RemoteException {
        return mService.get().getQueueItemAtPosition(position);
    }

    @Override
    public int getQueueSize() throws RemoteException {
        return mService.get().getQueueSize();
    }

    @Override
    public int getQueuePosition() throws RemoteException {
        return mService.get().getQueuePosition();
    }

    @Override
    public int getQueueHistoryPosition(int position) throws RemoteException {
        return mService.get().getQueueHistoryPosition(position);
    }

    @Override
    public int getQueueHistorySize() throws RemoteException {
        return mService.get().getQueueHistorySize();
    }

    @Override
    public int[] getQueueHistoryList() throws RemoteException {
        return mService.get().getQueueHistoryList();
    }

    @Override
    public long duration() throws RemoteException {
        return mService.get().duration();
    }

    @Override
    public long position() throws RemoteException {
        return mService.get().position();
    }

    @Override
    public int secondPosition() throws RemoteException {
        return mService.get().getSecondPosition();
    }

    @Override
    public long seek(long pos) throws RemoteException {
        return mService.get().seek(pos);
    }

    @Override
    public void seekRelative(long deltaInMs) throws RemoteException {
        mService.get().seekRelative(deltaInMs);
    }

    @Override
    public long getAudioId() throws RemoteException {
        return mService.get().getAudioId();
    }

    @Override
    public MusicTrack getCurrentTrack() throws RemoteException {
        return mService.get().getCurrentTrack();
    }

    @Override
    public MusicTrack getTrack(int index) throws RemoteException {
        return mService.get().getTrack(index);
    }

    @Override
    public long getNextAudioId() throws RemoteException {
        return mService.get().getNextAudioId();
    }

    @Override
    public long getPreviousAudioId() throws RemoteException {
        return mService.get().getPreviousAudioId();
    }

    @Override
    public long getArtistId() throws RemoteException {
        return mService.get().getArtistId();
    }

    @Override
    public long getAlbumId() throws RemoteException {
        return mService.get().getAlbumId();
    }

    @Override
    public String getArtistName() throws RemoteException {
        return mService.get().getArtistName();
    }

    @Override
    public String getTrackName() throws RemoteException {
        return mService.get().getTrackName();
    }

    @Override
    public boolean isTrackLocal() throws RemoteException {
        return mService.get().isTrackLocal();
    }

    @Override
    public String getAlbumName() throws RemoteException {
        return mService.get().getAlbumName();
    }

    @Override
    public String getAlbumPath() throws RemoteException {
        return mService.get().getAlbumPath();
    }

    @Override
    public String[] getAlbumPathtAll() throws RemoteException {
        return mService.get().getAlbumPathAll();
    }

    @Override
    public String getPath() throws RemoteException {
        return mService.get().getPath();
    }

    @Override
    public int getShuffleMode() throws RemoteException {
        return mService.get().getShuffleMode();
    }

    @Override
    public int removeTracks(int first, int last) throws RemoteException {
        return mService.get().removeTracks(first, last);
    }

    @Override
    public int removeTrack(long id) throws RemoteException {
        return mService.get().removeTrack(id);
    }

    @Override
    public boolean removeTrackAtPosition(long id, int position) throws RemoteException {
        return mService.get().removeTrackAtPosition(id, position);
    }

    @Override
    public int getRepeatMode() throws RemoteException {
        return mService.get().getRepeatMode();
    }

    @Override
    public int getMediaMountedCount() throws RemoteException {
        return mService.get().getMediaMountedCount();
    }

    @Override
    public int getAudioSessionId() throws RemoteException {
        return mService.get().getAudioSessionId();
    }

    @Override
    public void setLockscreenAlbumArt(boolean enabled) throws RemoteException {
        mService.get().setLockscreenAlbumArt(enabled);
    }

    @Override
    public void exit() throws RemoteException {
        mService.get().exit();
    }

    @Override
    public void timing(int time) throws RemoteException {
        mService.get().timing(time);
    }

    @Override
    public void cancelTiming() throws RemoteException {
        mService.get().cancelTiming();
    }

    @Override
    public void timingImpl(int time) throws RemoteException {
        mService.get().timingImpl(time);
    }

    @Override
    public void cancelTimingImpl() throws RemoteException {
        mService.get().cancelTimingImpl();
    }

    @Override
    public void setPlaying(boolean isPlaying) {
        mService.get().setPlaying(isPlaying);
    }
}
