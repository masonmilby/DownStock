package com.milburn.downstock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.milburn.downstock.ProductDetails.BasicItem;

public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    TextRecognizer textRecognizer;
    Context context;

    public OcrDetectorProcessor(Context con) {
        context = con;
        textRecognizer = new TextRecognizer.Builder(context).build();
    }

    public boolean isRecognizerReady() {
        if (!textRecognizer.isOperational()) {
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                //Snackbar.make(context.findViewById(R.id.fragment_container), "Storage too low", Snackbar.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public void createCameraSource(SurfaceHolder holder, int width, int height) {
        CameraSource cameraSource = new CameraSource.Builder(context, textRecognizer)
                .setAutoFocusEnabled(true)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(60.0f)
                .setRequestedPreviewSize(1920, 1080)
                .build();

        try {
            cameraSource.start(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        textRecognizer.setProcessor(this);
    }

    public ProductDetails recognizeSkus(PhotoUriList uriList) {
        ProductDetails productDetails = new ProductDetails();
        int pageNum = -1;
        while (pageNum < uriList.size()-1) {
            pageNum++;
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uriList.getUri(pageNum));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Frame frame = new Frame.Builder()
                    .setBitmap(bitmap)
                    .build();

            SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);

            Pattern p = Pattern.compile("\\d{7}");

            String temp = "";
            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                temp = temp + " " + textBlock.getValue();
            }

            Matcher m = p.matcher(temp);
            while (m.find()) {
                productDetails.addBasicItem(new BasicItem(m.group(), pageNum));
            }
        }
        return productDetails;
    }

    @Override
    public void release() {
        //
    }

    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        SparseArray<TextBlock> items = detections.getDetectedItems();
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                Log.d("Processor", "Text detected! " + item.getValue());
            }
        }
    }
}
