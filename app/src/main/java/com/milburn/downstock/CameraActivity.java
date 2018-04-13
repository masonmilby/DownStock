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
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.error.CameraErrorListener;
import io.fotoapparat.exception.camera.CameraException;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;
import io.fotoapparat.result.BitmapPhoto;
import io.fotoapparat.selector.FocusModeSelectorsKt;
import io.fotoapparat.selector.ResolutionSelectorsKt;
import io.fotoapparat.view.CameraView;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static io.fotoapparat.log.LoggersKt.logcat;
import static io.fotoapparat.selector.LensPositionSelectorsKt.back;

import com.milburn.downstock.ProductDetails.BasicItem;

public class CameraActivity extends AppCompatActivity {
    private FragmentManager fragmentManager;
    private ProductDetails productDetails = new ProductDetails();

    private Button captureButton;
    private CameraView cameraView;
    private Fotoapparat fotoapparat;
    private Frame latestFrame;
    private OcrDetectorProcessor ocr;

    public CoordinatorLayout coordinatorLayout;
    public Toolbar toolbar;

    private LinearLayout bottomSheet;
    private BottomSheetBehavior bottomSheetBehavior;
    private BottomNavigationView bottomNavigationView;

    private Vibrator vibrator;
    private long[] PATTERN_FOUND = new long[]{0, 50};

    private int currentAngle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                currentAngle = orientation;
            }
        };
        orientationEventListener.enable();

        coordinatorLayout = findViewById(R.id.container_coordinator);

        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        captureButton = findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File dirs = new File(getExternalFilesDir(null).getAbsolutePath() + "/pages");
                dirs.mkdirs();
                File photo = new File(dirs.getAbsolutePath() + "/page0.png");
                fotoapparat.takePicture().toBitmap().whenAvailable(new Function1<BitmapPhoto, Unit>() {
                    @Override
                    public Unit invoke(BitmapPhoto bitmapPhoto) {
                        //TODO Post rotate and save
                        return null;
                    }
                });
            }
        });

        cameraView = findViewById(R.id.camera_view);
        fotoapparat = createFotoapparat();

        ocr = new OcrDetectorProcessor(this, productDetails, detectedInterface);

        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.selector_sku_upc);
        selectNavigation(false);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.selector_page:
                        selectNavigation(true);
                        break;

                    default:
                        selectNavigation(false);
                        break;
                }
                return true;
            }
        });

        fragmentManager = getSupportFragmentManager();

        productDetails = getRecyclerFragment().getProductDetails();

        ocr.start();
        fotoapparat.start();
    }

    private Fotoapparat createFotoapparat() {
        return Fotoapparat
                .with(this)
                .into(cameraView)
                .focusMode(FocusModeSelectorsKt.continuousFocusVideo())
                .previewScaleType(ScaleType.CenterCrop)
                .previewResolution(ResolutionSelectorsKt.highestResolution())
                .photoResolution(ResolutionSelectorsKt.highestResolution())
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

    private class PreviewFrameProcessor implements FrameProcessor {
        @Override
        public void process(Frame frame) {
            latestFrame = frame;

            if (ocr.isReadyForFrame()) {
                ocr.recognizeFrame(frame, currentAngle);
            }
        }
    }

    private void selectNavigation(boolean page) {
        if (page) {
            ocr.stop();
            captureButton.setEnabled(true);
            captureButton.setAlpha(1.0f);
        } else {
            ocr.start();
            captureButton.setEnabled(false);
            captureButton.setAlpha(0.5f);
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
                    ProductDetails tempProductDetails = new ProductDetails();
                    tempProductDetails.addBasicItems(items);
                    vibrateToast(null, Toast.LENGTH_SHORT, PATTERN_FOUND);
                    getRecyclerFragment().queryApi(tempProductDetails, false);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    break;

                //Single frame processed
                case 2:
                    break;
            }
        }
    };

    public RecyclerFragment getRecyclerFragment() {
        return (RecyclerFragment) fragmentManager.findFragmentById(R.id.fragment_recycler);
    }

    public void vibrateToast(final CharSequence text, int duration, final long[] pattern) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vibrator.vibrate(pattern, -1);
                if (text != null) {
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void selectPhotos() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        PhotoUriList uriList = productDetails.getUriList();
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
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.camera_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            case R.id.open_image:
                selectPhotos();
                break;

            case R.id.auto_rejection:
                break;

            default:
                break;
        }
        return true;
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
        ocr.stop();
    }
}
