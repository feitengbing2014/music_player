package cn.tony.music.constent;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public interface IConstants {


    int VIEWPAGER_SCROLL_TIME = 390;
    int TIME_DELAY = 800;

    int FAV_PLAYLIST = 10;

    int MAX_HISTORY_SIZE = 1000;

    String MUSIC_COUNT_CHANGED = "com.tony.music.musiccountchanged";
    String PLAYLIST_COUNT_CHANGED = "com.tony.music.playlistcountchanged";
    String EMPTY_LIST = "com.tony.music.emptyplaylist";

    //消息通知
    String TOGGLEPAUSE_ACTION = "cn.tony.music.togglepause";
    String PAUSE_ACTION = "cn.tony.music.pause";
    String STOP_ACTION = "cn.tony.music.stop";
    String NEXT_ACTION = "cn.tony.music.next";


    int NOTIFY_MODE_NONE = 0;
    int NOTIFY_MODE_FOREGROUND = 1;
    int NOTIFY_MODE_BACKGROUND = 2;


    //MediaButtonIntentReceiver
    int MSG_LONGPRESS_TIMEOUT = 1;
    int MSG_HEADSET_DOUBLE_CLICK_TIMEOUT = 2;

    int LONG_PRESS_DELAY = 1000;
    int DOUBLE_CLICK = 800;


    //Service
    String PLAYSTATE_CHANGED = "cn.tony.music.playstatechanged";
    String POSITION_CHANGED = "cn.tony.music.positionchanged";
    String META_CHANGED = "cn.tony.music.metachanged";
    String PLAYLIST_ITEM_MOVED = "cn.tony.music.mmoved";
    String QUEUE_CHANGED = "cn.tony.music.queuechanged";
    String PLAYLIST_CHANGED = "cn.tony.music.playlistchanged";
    String REPEATMODE_CHANGED = "cn.tony.music.repeatmodechanged";
    String SHUFFLEMODE_CHANGED = "cn.tony.music.shufflemodechanged";
    String TRACK_ERROR = "cn.tony.music.trackerror";
    String TIMBER_PACKAGE_NAME = "cn.tony.music";
    String MUSIC_PACKAGE_NAME = "com.android.music";
    String SERVICECMD = "cn.tony.music.musicservicecommand";

    String PREVIOUS_ACTION = "cn.tony.music.previous";
    String PREVIOUS_FORCE_ACTION = "cn.tony.music.previous.force";

    String MUSIC_CHANGED = "cn.tony.music.change_music";
    String REPEAT_ACTION = "cn.tony.music.repeat";
    String SHUFFLE_ACTION = "cn.tony.music.shuffle";
    String FROM_MEDIA_BUTTON = "frommediabutton";
    String REFRESH = "cn.tony.music.refresh";
    String LRC_UPDATED = "cn.tony.music.updatelrc";
    String UPDATE_LOCKSCREEN = "cn.tony.music.updatelockscreen";

    String NOTIFICATION_VIEW_CLEAR = "cn.tony.music.NOTIFICATION_VIEW_CLEAR";

    String CMDNAME = "command";
    String CMDTOGGLEPAUSE = "togglepause";
    String CMDSTOP = "stop";
    String CMDPAUSE = "pause";
    String CMDPLAY = "play";
    String CMDPREVIOUS = "previous";
    String CMDNEXT = "next";
    String CMDNOTIF = "buttonId";


    String TRACK_PREPARED = "cn.tony.music.prepared";
    String TRY_GET_TRACKINFO = "cn.tony.music.gettrackinfo";
    String BUFFER_UP = "cn.tony.music.bufferup";
    String LOCK_SCREEN = "cn.tony.music.lock";
    String SEND_PROGRESS = "cn.tony.music.progress";
    String MUSIC_LODING = "cn.tony.music.loading";
    String SHUTDOWN = "cn.tony.music.shutdown";
    String SETQUEUE = "cn.tony.music.setqueue";
    String DECREASEVOLUME = "cn.tony.music.decreasevolume";


    int NEXT = 2;
    int LAST = 3;
    int SHUFFLE_NONE = 0;
    int SHUFFLE_NORMAL = 1;//随机播放
    int REPEAT_CURRENT = 1;//单曲循环
    int REPEAT_ALL = 2;//列表循环
    int SHUFFLE_AUTO = 2;
    int REPEAT_NONE = 2;


    int IDCOLIDX = 0;

    int IDLE_DELAY = 5 * 60 * 1000;
    long REWIND_INSTEAD_PREVIOUS_THRESHOLD = 3000;


    int LRC_DOWNLOADED = -10;

    int TRACK_ENDED = 1;
    int TRACK_WENT_TO_NEXT = 2;
    int RELEASE_WAKELOCK = 3;

    int FADEDOWN = 6;
    int FADEUP = 7;
    int SERVER_DIED = 4;
    int FOCUSCHANGE = 5;


    String MUSIC_TAG = "tonymusic";
    String MEDIASERVICE_PREFERENCES = "MusicMediaService";
}
