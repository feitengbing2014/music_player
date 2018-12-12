package cn.tony.music.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.ImageView;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

import cn.tony.music.R;
import cn.tony.music.service.MusicPlayer;

import static cn.tony.music.service.MusicPlayer.getAlbumPath;

/**
 * Ella Group
 *
 * <p> 获取封面背景
 * <p>
 * Author by Tony, on 2018/10/26.
 */

public class BlurredAlbumArtAsyn extends AsyncTask<Void, Void, Drawable> {


    private final String TAG = "BlurredAlbumArtAsyn";

    private WeakReference<Context> weakReference;
    private WeakReference<ImageView> imageViewWeakReference;
    private long lastAlbum;


    private long albumid;
    private BitmapFactory.Options mNewOpts;
    private Bitmap mBitmap;


    public BlurredAlbumArtAsyn(Context context, long lastAlbum, ImageView mBackAlbum) {
        this.lastAlbum = lastAlbum;
        albumid = MusicPlayer.getCurrentAlbumId();
        weakReference = new WeakReference<>(context);
        imageViewWeakReference = new WeakReference<>(mBackAlbum);

    }


    @Override
    protected Drawable doInBackground(Void... loadedImage) {
        lastAlbum = albumid;
        Drawable drawable = null;
        mBitmap = null;
        if (mNewOpts == null) {
            mNewOpts = new BitmapFactory.Options();
            mNewOpts.inSampleSize = 6;
            mNewOpts.inPreferredConfig = Bitmap.Config.RGB_565;
        }
        if (!MusicPlayer.isTrackLocal()) {
            Logger.info(TAG, "music is net");
            if (getAlbumPath() == null) {
                Logger.info(TAG, "getalbumpath is null");
                mBitmap = BitmapFactory.decodeResource(weakReference.get().getResources(), R.mipmap.placeholder_disk_210);
                drawable = ImageUtils.createBlurredImageFromBitmap(mBitmap, weakReference.get(), 3);
                return drawable;
            }
            ImageRequest imageRequest = ImageRequestBuilder
                    .newBuilderWithSource(Uri.parse(getAlbumPath()))
                    .setProgressiveRenderingEnabled(true)
                    .build();

            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            DataSource<CloseableReference<CloseableImage>>
                    dataSource = imagePipeline.fetchDecodedImage(imageRequest, weakReference.get());

            dataSource.subscribe(new BaseBitmapDataSubscriber() {
                                     @Override
                                     public void onNewResultImpl(@Nullable Bitmap bitmap) {
                                         // You can use the bitmap in only limited ways
                                         // No need to do any cleanup.
                                         if (bitmap != null) {
                                             mBitmap = bitmap;
                                             Logger.info(TAG, "getalbumpath bitmap success");
                                         }
                                     }

                                     @Override
                                     public void onFailureImpl(DataSource dataSource) {
                                         // No cleanup required here.
                                         Logger.info(TAG, "getalbumpath bitmap failed");
                                         mBitmap = BitmapFactory.decodeResource(weakReference.get().getResources(), R.mipmap.placeholder_disk_210);

                                     }
                                 },
                    CallerThreadExecutor.getInstance());
            if (mBitmap != null) {
                drawable = ImageUtils.createBlurredImageFromBitmap(mBitmap, weakReference.get(), 3);
            }

        } else {
            try {
                mBitmap = null;
                Bitmap bitmap;
                String albumPath = getAlbumPath();
                if (TextUtils.isEmpty(albumPath)) {
                    bitmap = BitmapFactory.decodeResource(weakReference.get().getResources(), R.mipmap.placeholder_disk_210, mNewOpts);
                } else {
                    Uri art = Uri.parse(albumPath);
                    Logger.info(TAG, "album is local ");
                    if (art != null) {
                        ParcelFileDescriptor fd = null;
                        try {
                            fd = weakReference.get().getContentResolver().openFileDescriptor(art, "r");
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        if (fd != null) {
                            bitmap = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, mNewOpts);
                        } else {
                            bitmap = BitmapFactory.decodeResource(weakReference.get().getResources(), R.mipmap.placeholder_disk_210, mNewOpts);
                        }
                    } else {
                        bitmap = BitmapFactory.decodeResource(weakReference.get().getResources(), R.mipmap.placeholder_disk_210, mNewOpts);
                    }
                }

                if (bitmap != null) {
                    drawable = ImageUtils.createBlurredImageFromBitmap(bitmap, weakReference.get(), 3);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return drawable;
    }

    @Override
    protected void onPostExecute(Drawable result) {
        if (albumid != MusicPlayer.getCurrentAlbumId()) {
            this.cancel(true);
            return;
        }
        setDrawable(result, imageViewWeakReference.get());

    }


    private void setDrawable(Drawable result, ImageView mBackAlbum) {
        if (result != null) {
            if (mBackAlbum.getDrawable() != null) {
                final TransitionDrawable td =
                        new TransitionDrawable(new Drawable[]{mBackAlbum.getDrawable(), result});
                mBackAlbum.setImageDrawable(td);
                //去除过度绘制
                td.setCrossFadeEnabled(true);
                td.startTransition(200);

            } else {
                mBackAlbum.setImageDrawable(result);
            }
        }
    }
}
