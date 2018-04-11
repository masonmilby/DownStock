package com.milburn.downstock;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.error.CameraErrorListener;
import io.fotoapparat.exception.camera.CameraException;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;
import io.fotoapparat.selector.FocusModeSelectorsKt;
import io.fotoapparat.selector.ResolutionSelectorsKt;
import io.fotoapparat.view.CameraView;

import static io.fotoapparat.log.LoggersKt.logcat;
import static io.fotoapparat.selector.LensPositionSelectorsKt.back;

import com.milburn.downstock.ProductDetails.BasicItem;

public class CameraActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private ProductDetails productDetails = new ProductDetails();

    private Button captureButton;
    private Button selectButton;
    private CameraView cameraView;
    private Fotoapparat fotoapparat;
    private Frame latestFrame;
    private OcrDetectorProcessor ocr;
    private boolean isReadyForFrame;

    public CoordinatorLayout coordinatorLayout;

    private LinearLayout bottomSheet;
    private BottomSheetBehavior bottomSheetBehavior;
    private BottomNavigationView bottomNavigationView;

    private Vibrator vibrator;
    private long[] PATTERN_FOUND = new long[]{0, 50};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        coordinatorLayout = findViewById(R.id.container_coordinator);

        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        captureButton = findViewById(R.id.button_capture);
        selectButton = findViewById(R.id.button_select);
        selectButton.setOnClickListener(imageSelectListener);

        cameraView = findViewById(R.id.camera_view);
        fotoapparat = createFotoapparat();

        isReadyForFrame = false;
        ocr = new OcrDetectorProcessor(this, productDetails, detectedInterface);

        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from((LinearLayout) findViewById(R.id.bottom_sheet));

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.selector_page);

        fragmentManager = getSupportFragmentManager();

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                //
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //
            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.selector_page:
                        captureButton.setEnabled(true);
                        captureButton.setAlpha(1.0f);
                        break;

                    default:
                        captureButton.setEnabled(false);
                        captureButton.setAlpha(0.5f);
                        break;
                }
                return true;
            }
        });

        getRecyclerFragment().setupRecycler(productDetails);

        fotoapparat.start();
        startOcr();
    }

    private Fotoapparat createFotoapparat() {
        return Fotoapparat
                .with(this)
                .into(cameraView)
                .focusMode(FocusModeSelectorsKt.continuousFocusVideo())
                .previewScaleType(ScaleType.CenterCrop)
                .previewResolution(ResolutionSelectorsKt.highestResolution())
                .lensPosition(back())
                .frameProcessor(new PreviewFrameProcessor())
                .logger(logcat())
                .cameraErrorCallback(new CameraErrorListener() {
                    @Override
                    public void onError(@NotNull CameraException e) {
                        Toast.makeText(CameraActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                    }
                })
                .build();
    }

    private void startOcr() {
        if (ocr.isRecognizerReady()) {
            isReadyForFrame = true;
        }
    }

    private class PreviewFrameProcessor implements FrameProcessor {
        @Override
        public void process(Frame frame) {
            latestFrame = frame;

            if (isReadyForFrame) {
                ocr.recognizeFrame(frame);
            }
        }
    }

    private OcrDetectorProcessor.DetectedInterface detectedInterface = new OcrDetectorProcessor.DetectedInterface() {
        @Override
        public void FinishedProcessing(List<BasicItem> items, ProductDetails products, int responseType) {
            switch (responseType) {
                //No new items detected
                case 0:
                    break;

                //New items detected
                case 1:
                    for (BasicItem item : items) {
                        Log.i("New item: ", item.getSku());
                        productDetails.addBasicItem(item);
                        vibrateToast(item.getSku(), Toast.LENGTH_SHORT, PATTERN_FOUND);
                        getRecyclerFragment().queryApi(item, false);
                    }
                    break;

                //Single frame processed
                case 2:
                    break;
            }
            isReadyForFrame = true;
        }
    };

    private RecyclerFragment getRecyclerFragment() {
        return (RecyclerFragment) fragmentManager.findFragmentById(R.id.fragment_recycler);
    }

    public void vibrateToast(final CharSequence text, int duration, final long[] pattern) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vibrator.vibrate(pattern, -1);
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View.OnClickListener imageSelectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, 0);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        PhotoUriList uriList = new PhotoUriList();
        switch (requestCode) {
            case 0:
                if (data != null) {
                    if (data.getClipData() != null) {
                        for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                            uriList.addUri(data.getClipData().getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null){
                        uriList.addUri(data.getData());
                    }
                    ProductDetails productDetails = ocr.getProductDetailsFromUris(uriList);
                    if (productDetails.sizeBasicItems() > 0) {
                        getRecyclerFragment().queryApi(productDetails, false);
                    } else {
                        Snackbar.make(coordinatorLayout, "No products detected", Snackbar.LENGTH_LONG).show();
                    }
                    break;
                } else {
                    Snackbar.make(coordinatorLayout, "No image selected", Snackbar.LENGTH_LONG).show();
                    break;
                }

                default:
                    break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fotoapparat.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        fotoapparat.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fotoapparat.stop();
    }
}
