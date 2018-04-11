package com.milburn.downstock;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.util.SparseArray;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.milburn.downstock.ProductDetails.BasicItem;

public class OcrDetectorProcessor {

    private TextRecognizer textRecognizer;
    private Context context;
    private ProductDetails currentProductDetails;
    private DetectedInterface delegate;

    public interface DetectedInterface {
        void FinishedProcessing(List<BasicItem> basicList, ProductDetails products, int responseType);
    }

    public OcrDetectorProcessor(Context con, ProductDetails productDetails, DetectedInterface del) {
        context = con;
        currentProductDetails = productDetails;
        delegate = del;
        textRecognizer = new TextRecognizer.Builder(context).build();
    }

    public boolean isRecognizerReady() {
        if (!textRecognizer.isOperational()) {
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Snackbar.make(((CameraActivity)(context)).coordinatorLayout, "Storage too low", Snackbar.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

    public ProductDetails getProductDetailsFromUris(PhotoUriList uriList) {
        ProductDetails productDetails = new ProductDetails();
        for (int pageNum = 0; pageNum < uriList.size(); pageNum++) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uriList.getUri(pageNum));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Frame frame = new Frame.Builder()
                    .setBitmap(bitmap)
                    .build();

            List<BasicItem> basicList = recognizeSkus(textRecognizer.detect(frame), pageNum);
            for (BasicItem item : basicList) {
                productDetails.addBasicItem(item);
            }
            productDetails.getUriList().addUri(uriList.getUri(pageNum));
        }
        return productDetails;
    }

    private List<BasicItem> recognizeSkus(SparseArray<TextBlock> items, int pageNum) {
        List<BasicItem> basicList = new ArrayList<>();
        Pattern p = Pattern.compile("\\d{7}");
        Matcher m;

        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                m = p.matcher(item.getValue());
                while (m.find()) {
                    System.out.println(m.group());
                    if (!isAlreadyRecognized(m.group())) {
                        basicList.add(new BasicItem(m.group(), pageNum));
                    }
                }
            }
        }
        return basicList;
    }

    public void recognizeFrame(io.fotoapparat.preview.Frame frame) {
        List<BasicItem> basicList = recognizeSkus(textRecognizer.detect(convertToGVFrame(frame)), -2);
        if (basicList.size() != 0) {
            delegate.FinishedProcessing(basicList, null, 1);
        } else {
            delegate.FinishedProcessing(null, null, 0);
        }
    }

    private boolean isAlreadyRecognized(String sku) {
        return currentProductDetails.getBasicItem(sku) != null;
    }

    private Frame convertToGVFrame(io.fotoapparat.preview.Frame frame) {
        int width = frame.getSize().width;
        int height = frame.getSize().height;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        YuvImage yuvImage = new YuvImage(frame.getImage(), ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, bos);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.size());

        return new Frame.Builder().setBitmap(bitmap).build();
    }
}