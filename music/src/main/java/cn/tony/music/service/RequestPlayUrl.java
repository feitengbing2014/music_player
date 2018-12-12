package cn.tony.music.service;

import cn.tony.music.sp.PreferencesUtility;
import cn.tony.music.utils.Logger;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/29.
 */

public class RequestPlayUrl implements Runnable {

    private final String TAG = "RequestPlayUrl";

    private long id;
    private boolean play;
    private boolean stop;
    private MediaService service;
    private MultiPlayer mPlayer;

    public RequestPlayUrl(long id, boolean play, MediaService service, MultiPlayer mPlayer) {
        this.id = id;
        this.play = play;
        this.service = service;
        this.mPlayer = mPlayer;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        try {
            String url = PreferencesUtility.getInstance(service.getBaseContext()).getPlayLink(id);
            if (url != null) {
                Logger.info(TAG, "current url = " + url);
            } else {
                service.gotoNext(true);
            }

            if (!stop) {
                // String urlEn = HttpUtil.urlEncode(url);
                String urlEn = url;
                mPlayer.setDataSource(urlEn);
            }


            if (play && !stop) {
                service.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
