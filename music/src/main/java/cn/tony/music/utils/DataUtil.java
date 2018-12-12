package cn.tony.music.utils;

import java.util.List;
import java.util.Set;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/30.
 */

public class DataUtil {
    public static boolean isEmpty(List<?> list) {
        return list == null || list.size() == 0;
    }

    public static boolean isEmpty(Set<?> list) {
        return list == null || list.size() == 0;
    }
}
