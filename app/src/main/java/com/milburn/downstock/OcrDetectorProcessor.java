package com.milburn.downstock;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.util.SparseArray;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
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
    private BarcodeDetector barcodeDetector;
    private Context context;
    private DetectedInterface delegate;
    private boolean isReadyForFrame = false;
    private boolean isStopped = true;

    public interface DetectedInterface {
        void FinishedProcessing(List<BasicItem> basicList, Bitmap bitmap, String pageId, int responseType);
    }

    public OcrDetectorProcessor(Context con, DetectedInterface del) {
        context = con;
        delegate = del;
    }

    private boolean isRecognizerReady() {
        if (!(textRecognizer.isOperational() && barcodeDetector.isOperational())) {
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Snackbar.make(((CameraActivity)(context)).coordinatorLayout, "Storage too low", Snackbar.LENGTH_LONG).show();
            }
            stop(true);
            return false;
        }
        return true;
    }

    public boolean isReadyForFrame() {
        return isReadyForFrame;
    }

    public boolean isStopped() {
        return isStopped;
    }

    public void start() {
        if (isStopped) {
            textRecognizer = new TextRecognizer.Builder(context).build();
            barcodeDetector = new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.QR_CODE | Barcode.UPC_A).build();
            isReadyForFrame = isRecognizerReady();
            isStopped = false;
        }
    }

    public void stop(boolean release) {
        isStopped = true;
        isReadyForFrame = false;
        if (release && textRecognizer != null && barcodeDetector != null) {
            textRecognizer.release();
            barcodeDetector.release();
        }
    }

    public void recognizeSkusFromUris(List<Uri> uriList) {
        for (int page = 0; page < uriList.size(); page++) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uriList.get(page));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (bitmap != null) {
                Frame frame = new Frame.Builder()
                        .setBitmap(bitmap)
                        .build();

                recognizeFrame(frame, 0, false, false, ProductDetails.generateUUID());
            }
        }
    }

    private List<BasicItem> recognizeSkus(SparseArray<TextBlock> textItems, SparseArray<Barcode> barcodeItems, String pageId) {
        List<BasicItem> basicList = new ArrayList<>();
        Pattern p = Pattern.compile("\\b((\\d{7})|\\d{12})(?!\\d)");
        Matcher m;

        for (int i = 0; i < textItems.size(); ++i) {
            TextBlock item = textItems.valueAt(i);
            if (item != null && item.getValue() != null) {
                m = p.matcher(item.getValue());
                while (m.find()) {
                    if (!isAlreadyRecognized(m.group())) {
                        BasicItem basicItem;
                        if (m.group().length() == 7) {
                            basicItem = new BasicItem(m.group(), "", pageId);
                        } else {
                            basicItem = new BasicItem("", m.group(), pageId);
                        }
                        basicList.add(basicItem);
                    }
                }
            }
        }

        for (int i = 0; i < barcodeItems.size(); i++) {
            String rawData = barcodeItems.valueAt(i).rawValue;
            if (rawData.contains("bby.us")) {
                rawData = rawData.replace("http://bby.us/?c=", "");
                rawData = rawData.substring(7, 14);
                if (!isAlreadyRecognized(rawData)) {
                    basicList.add(new BasicItem(rawData, "", pageId));
                }
            } else if (rawData.length() == 12) {
                if (!isAlreadyRecognized(rawData)) {
                    basicList.add(new BasicItem("", rawData, pageId));
                }
            }
        }

        return basicList;
    }

    public void recognizeFrame(Frame frame, int angle, boolean continuous, boolean rotate, String id) {
        isReadyForFrame = false;
        Frame rotatedFrame = rotateFrame(frame, angle);
        List<BasicItem> basicList = recognizeSkus(textRecognizer.detect(rotate ? rotatedFrame : frame), barcodeDetector.detect(frame), id);
        if (basicList.size() != 0 | !continuous) {
            delegate.FinishedProcessing(basicList, rotate ? rotatedFrame.getBitmap() : frame.getBitmap(), id, continuous ? 1 : 2);
        } else {
            delegate.FinishedProcessing(null, null, null, 0);
        }
        isReadyForFrame = continuous;
    }

    private boolean isAlreadyRecognized(String id) {
        ProductDetails productDetails = ((CameraActivity)context).getRecyclerFragment().getProductDetails();
        return productDetails.getBasicItem(id) != null | productDetails.getDetailedItem(id) != null;
    }

    public Frame convertToGVFrame(io.fotoapparat.preview.Frame frame) {
        int width = frame.getSize().width;
        int height = frame.getSize().height;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        YuvImage yuvImage = new YuvImage(frame.getImage(), ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, bos);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.size());

        return new Frame.Builder().setBitmap(bitmap).build();
    }

    private Frame rotateFrame(Frame frame, int angle) {
        return new Frame.Builder().setBitmap(rotateBitmap(frame.getBitmap(), angle)).build();
    }

    public Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        int newAngle = 90+angle;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.postRotate(newAngle);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
}