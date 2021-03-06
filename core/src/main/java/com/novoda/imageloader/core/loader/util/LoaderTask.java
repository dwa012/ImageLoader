/**
 * Copyright 2012 Novoda Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.novoda.imageloader.core.loader.util;

import java.io.File;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.novoda.imageloader.core.LoaderContext;
import com.novoda.imageloader.core.exception.ImageNotFoundException;
import com.novoda.imageloader.core.model.ImageWrapper;

public class LoaderTask extends AsyncTask<String, Void, Bitmap> {

    private WeakReference<ImageView> imageViewReference;
    private LoaderContext loaderContext;
    private String url;
    private boolean saveScaledImage;
    private boolean useCacheOnly;
    private int width;
    private int height;
    private int notFoundResourceId;

    public LoaderTask(ImageView imageView, LoaderContext loaderContext) {
        this.imageViewReference = new WeakReference<ImageView>(imageView);
        this.loaderContext = loaderContext;
    }

    @Override
    protected Bitmap doInBackground(String... arg0) {
        if (imageViewReference == null) {
            return null;
        }
        ImageView imageView = imageViewReference.get();
        if (imageView == null) {
            return null;
        }
        ImageWrapper imageWrapper = setAndValidateTagInformation(imageView);
        if (imageWrapper == null) {
            return null;
        }
        if (hasImageViewUrlChanged(imageView)) {
            return null;
        }
        Bitmap b = loaderContext.getCache().get(url, width, height);
        if (b != null) {
            return b;
        }
        File imageFile = getImageFile(imageWrapper);
        if (!imageFile.exists()) {
            if (useCacheOnly) {
                return null;
            }
            try {
                loaderContext.getNetworkManager().retrieveImage(url, imageFile);
            } catch (ImageNotFoundException inf) {
                return getNotFoundImage(imageWrapper.getContext());
            }
        }
        if (hasImageViewUrlChanged(imageView)) {
            return null;
        }
        return getImageFromFile(imageFile);
    }

    private Bitmap getImageFromFile(File imageFile) {
        Bitmap b = loaderContext.getBitmapUtil().decodeFileAndScale(imageFile, width, height);
        if(b == null) {
            return b;
        }
        if (saveScaledImage) {
            saveScaledImage(imageFile, b);
        }
        loaderContext.getCache().put(url, b);
        return b;
    }

    private ImageWrapper setAndValidateTagInformation(ImageView imageView) {
        ImageWrapper imageWrapper = new ImageWrapper(imageView);
        url = imageWrapper.getUrl();
        if (url == null || url.length() <= 0) {
            return null;
        }
        width = imageWrapper.getWidth();
        height = imageWrapper.getHeight();
        notFoundResourceId = imageWrapper.getNotFoundResourceId();
        useCacheOnly = imageWrapper.isUseCacheOnly();
        return imageWrapper;
    }

    private void saveScaledImage(File imageFile, Bitmap b) {
        loaderContext.getFileManager().saveBitmap(imageFile.getAbsolutePath(), b, width, height);
    }

    private File getImageFile(ImageWrapper imageWrapper) {
        File imageFile = null;
        if (imageWrapper.isSaveThumbnail()) {
            imageFile = loaderContext.getFileManager().getFile(url, width, height);
        }
        if (imageFile == null || !imageFile.exists()) {
            imageFile = loaderContext.getFileManager().getFile(url);
            if (imageWrapper.isSaveThumbnail()) {
                saveScaledImage = true;
            }
        }
        return imageFile;
    }

    private boolean hasImageViewUrlChanged(ImageView imageView) {
        return !url.equals(new ImageWrapper(imageView).getCurrentUrl());
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        if (isCancelled()) {
            bitmap = null;
            return;
        }
        if (imageViewReference == null) {
            return;
        }
        ImageView imageView = imageViewReference.get();
        if (imageView == null) {
            return;
        }
        if (hasImageViewUrlChanged(imageView)) {
            return;
        }
        imageView.setImageBitmap(bitmap);
    }

    private Bitmap getNotFoundImage(Context c) {
        String key = "resource" + notFoundResourceId + width + height;
        Bitmap b = loaderContext.getResBitmapCache().get(key, width, height);
        if (b != null) {
            return b;
        }
        b = loaderContext.getBitmapUtil().scaleResourceBitmap(c, width, height, notFoundResourceId);
        loaderContext.getResBitmapCache().put(key, b);
        return b;
    }

}
