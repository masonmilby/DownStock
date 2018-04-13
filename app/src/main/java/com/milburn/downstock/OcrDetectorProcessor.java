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
    private ProductDetails currentProductDetails;
    private DetectedInterface delegate;
    private boolean isReadyForFrame = false;

    public interface DetectedInterface {
        void FinishedProcessing(List<BasicItem> basicList, ProductDetails products, int responseType);
    }

    public OcrDetectorProcessor(Context con, ProductDetails productDetails, DetectedInterface del) {
        context = con;
        currentProductDetails = productDetails;
        delegate = del;
    }

    private boolean isRecognizerReady() {
        if (!(textRecognizer.isOperational() && barcodeDetector.isOperational())) {
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Snackbar.make(((CameraActivity)(context)).coordinatorLayout, "Storage too low", Snackbar.LENGTH_LONG).show();
            }
            stop();
            return false;
        }
        return true;
    }

    public boolean isReadyForFrame() {
        return isReadyForFrame;
    }

    public boolean start() {
        textRecognizer = new TextRecognizer.Builder(context).build();
        barcodeDetector = new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.QR_CODE | Barcode.UPC_A).build();
        isReadyForFrame = isRecognizerReady();
        return isReadyForFrame;
    }

    public void stop() {
        isReadyForFrame = false;
        textRecognizer.release();
        barcodeDetector.release();
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

            List<BasicItem> basicList = recognizeSkus(textRecognizer.detect(frame), barcodeDetector.detect(frame), pageNum);
            for (BasicItem item : basicList) {
                productDetails.addBasicItem(item);
            }
            productDetails.getUriList().addUri(uriList.getUri(pageNum));
        }
        return productDetails;
    }

    private List<BasicItem> recognizeSkus(SparseArray<TextBlock> textItems, SparseArray<Barcode> barcodeItems, int pageNum) {
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
                            basicItem = new BasicItem(m.group(), "", pageNum);
                        } else {
                            basicItem = new BasicItem("", m.group(), pageNum);
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
                    basicList.add(new BasicItem(rawData, "", pageNum));
                }
            }
        }

        return basicList;
    }

    public void recognizeFrame(io.fotoapparat.preview.Frame frame, int angle) {
        isReadyForFrame = false;
        List<BasicItem> basicList = recognizeSkus(textRecognizer.detect(rotateFrame(convertToGVFrame(frame), angle)), barcodeDetector.detect(convertToGVFrame(frame)), -1);
        if (basicList.size() != 0) {
            delegate.FinishedProcessing(basicList, null, 1);
        } else {
            delegate.FinishedProcessing(null, null, 0);
        }
        isReadyForFrame = true;
    }

    private boolean isAlreadyRecognized(String id) {
        currentProductDetails = ((CameraActivity)context).getRecyclerFragment().getProductDetails();
        return currentProductDetails.getBasicItem(id) != null | currentProductDetails.getDetailedItem(id) != null;
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

    private Frame rotateFrame(Frame frame, int angle) {
        int newAngle = 270-angle;
        int width = frame.getBitmap().getWidth();
        int height = frame.getBitmap().getHeight();

        if ((angle > 315 || angle < 45) || (angle > 135 && angle < 225)) {
            newAngle = 90-angle;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(newAngle);
        return new Frame.Builder()
                .setBitmap(Bitmap.createBitmap(frame.getBitmap(), 0, 0, width, height, matrix, true))
                .build();
    }
}