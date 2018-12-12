package cn.tony.music.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import cn.tony.music.R;
import cn.tony.music.constent.IConstants;
import cn.tony.music.service.MediaService;
import cn.tony.music.sp.PreferencesUtility;
import cn.tony.music.utils.CommonUtils;
import cn.tony.music.utils.ImageUtils;

/**
 * Ella Group
 * <p>
 * <p>
 * Author by Tony, on 2018/10/25.
 */

public class MusicNotification {

    private Notification mNotification;
    private Bitmap mNoBit;

    private static final MusicNotification ourInstance = new MusicNotification();

    public static MusicNotification getInstance() {
        return ourInstance;
    }

    private MusicNotification() {
    }


    public Notification getNotification(final MediaService service) {
        RemoteViews remoteViews;
        final int PAUSE_FLAG = 0x1;
        final int NEXT_FLAG = 0x2;
        final int STOP_FLAG = 0x3;

        String albumName = service.getAlbumName();
        String artistName = service.getAlbumName();
        String trackName = service.getTrackName();
        boolean isPlaying = service.isPlaying();
        boolean isTrackLocal = service.isTrackLocal();

        remoteViews = new RemoteViews(service.getPackageName(), R.layout.music_notification);
        String text = TextUtils.isEmpty(albumName) ? artistName : artistName + " - " + albumName;
        remoteViews.setTextViewText(R.id.title, trackName);
        remoteViews.setTextViewText(R.id.text, text);

        //此处action不能是一样的 如果一样的 接受的flag参数只是第一个设置的值
        Intent pauseIntent = new Intent(IConstants.TOGGLEPAUSE_ACTION);
        pauseIntent.putExtra("FLAG", PAUSE_FLAG);
        PendingIntent pausePIntent = PendingIntent.getBroadcast(service, 0, pauseIntent, 0);
        remoteViews.setImageViewResource(R.id.iv_pause, isPlaying ? R.mipmap.note_btn_pause : R.mipmap.note_btn_play);
        remoteViews.setOnClickPendingIntent(R.id.iv_pause, pausePIntent);


        if (PreferencesUtility.getInstance(service).isHideControl()) {
            remoteViews.setViewVisibility(R.id.iv_next, View.GONE);
        } else {
            remoteViews.setViewVisibility(R.id.iv_next, View.VISIBLE);
            Intent nextIntent = new Intent(IConstants.NEXT_ACTION);
            nextIntent.putExtra("FLAG", NEXT_FLAG);
            PendingIntent nextPIntent = PendingIntent.getBroadcast(service, 0, nextIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.iv_next, nextPIntent);
        }

        Intent preIntent = new Intent(IConstants.STOP_ACTION);
        preIntent.putExtra("FLAG", STOP_FLAG);
        PendingIntent prePIntent = PendingIntent.getBroadcast(service, 0, preIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.iv_stop, prePIntent);

//        PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0,
//                new Intent(this.getApplicationContext(), PlayingActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        final Intent nowPlayingIntent = new Intent();
        nowPlayingIntent.setComponent(new ComponentName(service.getPackageName(), "cn.tony.music.activity.PlayingActivity"));
        nowPlayingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent clickIntent = PendingIntent.getBroadcast(service, 0, nowPlayingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent click = PendingIntent.getActivity(service, 0, nowPlayingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final Bitmap bitmap = ImageUtils.getArtworkQuick(service, service.getAlbumId(), 160, 160);
        if (bitmap != null) {
            remoteViews.setImageViewBitmap(R.id.image, bitmap);
            // remoteViews.setImageViewUri(R.id.image, MusicUtils.getAlbumUri(this, getAudioId()));
            mNoBit = null;

        } else if (!isTrackLocal) {
            if (mNoBit != null) {
                remoteViews.setImageViewBitmap(R.id.image, mNoBit);
                mNoBit = null;

            } else {
                Uri uri = null;
                if (service.getAlbumPath() != null) {
                    try {
                        uri = Uri.parse(service.getAlbumPath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (service.getAlbumPath() == null || uri == null) {
                    mNoBit = BitmapFactory.decodeResource(service.getResources(), R.mipmap.placeholder_disk_210);
                    service.updateNotification();
                } else {
                    ImageRequest imageRequest = ImageRequestBuilder
                            .newBuilderWithSource(uri)
                            .setProgressiveRenderingEnabled(true)
                            .build();
                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    DataSource<CloseableReference<CloseableImage>>
                            dataSource = imagePipeline.fetchDecodedImage(imageRequest, service);

                    dataSource.subscribe(new BaseBitmapDataSubscriber() {

                                             @Override
                                             public void onNewResultImpl(@Nullable Bitmap bitmap) {
                                                 // You can use the bitmap in only limited ways
                                                 // No need to do any cleanup.
                                                 if (bitmap != null) {
                                                     mNoBit = bitmap;
                                                 }
                                                 service.updateNotification();
                                             }

                                             @Override
                                             public void onFailureImpl(DataSource dataSource) {
                                                 // No cleanup required here.
                                                 mNoBit = BitmapFactory.decodeResource(service.getResources(), R.mipmap.placeholder_disk_210);
                                                 service.updateNotification();
                                             }
                                         },
                            CallerThreadExecutor.getInstance());
                }
            }

        } else {
            remoteViews.setImageViewResource(R.id.image, R.mipmap.placeholder_disk_210);
        }


        if (service.mNotificationPostTime == 0) {
            service.mNotificationPostTime = System.currentTimeMillis();
        }
        if (mNotification == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("ellaBookNotification_id", "ellaBookNotification_name",
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.canBypassDnd();//是否绕过请勿打扰模式
                channel.enableLights(true);//闪光灯
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_SECRET);//锁屏显示通知
                channel.setLightColor(Color.RED);//闪关灯的灯光颜色
                channel.canShowBadge();//桌面launcher的消息角标
                channel.enableVibration(true);//是否允许震动
//                channel.getAudioAttributes();//获取系统通知响铃声音的配置
                channel.getGroup();//获取通知取到组
                channel.setBypassDnd(true);//设置可绕过  请勿打扰模式
                channel.setVibrationPattern(new long[]{100, 100, 200});//设置震动模式
                channel.shouldShowLights();//是否会有灯光
                channel.setSound(null, null);

                getNotificationManager(service).createNotificationChannel(channel);
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(service).setContent(remoteViews)
                    .setSmallIcon(R.mipmap.ic_notification)
                    .setContentIntent(click).setOnlyAlertOnce(true)
                    .setChannelId("ellaBookNotification_id")
                    .setWhen(service.mNotificationPostTime);
            if (CommonUtils.isJellyBeanMR1()) {
                builder.setShowWhen(false);
            }
            mNotification = builder.build();
        } else {
            mNotification.contentView = remoteViews;
        }

        return mNotification;
    }

    private NotificationManager mNotificationManager;

    private NotificationManager getNotificationManager(MediaService service) {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mNotificationManager;
    }
}
