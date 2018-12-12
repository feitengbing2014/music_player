package cn.tony.music.permissions;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public interface PermissionListener {
    /**
     * Gets called each time we run Nammu.permissionCompare() and some Permission is revoke/granted to us
     *
     * @param permissionChanged
     */
    void permissionsChanged(String permissionChanged);

    /**
     * Gets called each time we run Nammu.permissionCompare() and some Permission is granted
     *
     * @param permissionGranted
     */
    void permissionsGranted(String permissionGranted);

    /**
     * Gets called each time we run Nammu.permissionCompare() and some Permission is removed
     *
     * @param permissionRemoved
     */
    void permissionsRemoved(String permissionRemoved);
}

