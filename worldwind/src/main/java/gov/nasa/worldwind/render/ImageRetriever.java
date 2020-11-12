/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.util.Logger;
import gov.nasa.worldwind.util.Retriever;
import gov.nasa.worldwind.util.WWUtil;

public class ImageRetriever extends Retriever<ImageSource, ImageOptions, Bitmap> {

    protected final Context context;

    public ImageRetriever(int maxSimultaneousRetrievals, Context context) {
        super(maxSimultaneousRetrievals);
        this.context = context;
    }

    @Override
    protected void retrieveAsync(ImageSource imageSource, ImageOptions imageOptions,
                                 Callback<ImageSource, ImageOptions, Bitmap> callback) {
        try {
            Bitmap bitmap = this.decodeImage(imageSource, imageOptions);

            if (bitmap != null) {
                callback.retrievalSucceeded(this, imageSource, imageOptions, bitmap);
            } else {
                callback.retrievalFailed(this, imageSource, null); // failed but no exception
            }
        } catch (Throwable logged) {
            callback.retrievalFailed(this, imageSource, logged); // failed with exception
        }
    }

    // TODO can we explicitly recycle bitmaps from image sources other than direct Bitmap references?
    // TODO does explicit recycling help?
    protected Bitmap decodeImage(ImageSource imageSource, ImageOptions imageOptions) throws IOException {
        if (imageSource.isBitmap()) {
            return imageSource.asBitmap();
        }

        if (imageSource.isBitmapFactory()) {
            return imageSource.asBitmapFactory().createBitmap();
        }

        if (imageSource.isResource()) {
            return this.decodeResource(imageSource.asResource(), imageOptions);
        }

        if (imageSource.isFilePath()) {
            return this.decodeFilePath(imageSource.asFilePath(), imageOptions);
        }

        if (imageSource.isUrl()) {
            return this.decodeUrl(imageSource.asUrl(), imageOptions, imageSource.transformer);
        }

        return this.decodeUnrecognized(imageSource);
    }

    protected Bitmap decodeResource(int id, ImageOptions imageOptions) {
        BitmapFactory.Options factoryOptions = this.bitmapFactoryOptions(imageOptions);
        return BitmapFactory.decodeResource(this.context.getResources(), id, factoryOptions);
    }

    protected Bitmap decodeFilePath(String pathName, ImageOptions imageOptions) {
        BitmapFactory.Options factoryOptions = this.bitmapFactoryOptions(imageOptions);
        return BitmapFactory.decodeFile(pathName, factoryOptions);
    }

    protected Bitmap decodeUrl(String urlString, ImageOptions imageOptions, ImageSource.Transformer transformer) throws IOException {
        // TODO retry absent resources, they are currently handled but suppressed entirely after the first failure
        // TODO configurable connect and read timeouts

        // Attempt to retrieve remote image from a file cache
        Bitmap cached = retrieveFromCache(urlString, imageOptions);
        if (cached != null) {
            return cached;
        }

        InputStream stream = null;
        try {
            URLConnection conn = new URL(urlString).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(30000);

            stream = new BufferedInputStream(conn.getInputStream());

            BitmapFactory.Options factoryOptions = this.bitmapFactoryOptions(imageOptions);
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, factoryOptions);

            // Apply bitmap transformation if required
            if (transformer != null && bitmap != null) {
                bitmap = transformer.transform(bitmap);
            }

            // Store remote image in a file cache
            if (bitmap != null) {
                storeToCache(urlString, bitmap);
            }

            return bitmap;
        } finally {
            WWUtil.closeSilently(stream);
        }
    }

    protected Bitmap decodeUnrecognized(ImageSource imageSource) {
        Logger.log(Logger.WARN, "Unrecognized image source \'" + imageSource + "\'");
        return null;
    }

    protected BitmapFactory.Options bitmapFactoryOptions(ImageOptions imageOptions) {
        BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
        factoryOptions.inScaled = false; // suppress default image scaling; load the image in its native dimensions

        if (imageOptions != null) {
            switch (imageOptions.imageConfig) {
                case WorldWind.RGBA_8888:
                    factoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    break;
                case WorldWind.RGB_565:
                    factoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                    break;
            }
        }

        return factoryOptions;
    }

    protected Bitmap retrieveFromCache(String urlString, ImageOptions imageOptions) {
        File file = getCachedFile(urlString);
        return file != null ? decodeFilePath(file.getAbsolutePath(), imageOptions) : null;
    }

    protected void storeToCache(String urlString, Bitmap bitmap) {
        File file = getCachedFile(urlString);
        if (file != null) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                Logger.logMessage(Logger.ERROR, "ImageRetriever", "storeToCache", "Cannot write bitmap to cache file");
            }
        }
    }

    private File getCachedFile(String urlString) {
        byte[] hash = WWUtil.calculateHash(urlString.getBytes());
        return new File(this.context.getCacheDir(), WWUtil.byteToHex(hash));
    }
}
