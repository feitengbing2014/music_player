package cn.tony.music.utils;

import com.google.gson.Gson;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/11/1.
 */

public class GsonUtil {

    private static Gson gson = null;

    public static Gson getInstance() {
        if (gson == null)
            gson = new Gson();
        return gson;
    }

}
