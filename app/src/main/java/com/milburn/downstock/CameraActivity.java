package com.milburn.downstock;

import android.Manifest;
import android.animation.LayoutTransition;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.milburn.downstock.ProductDetails.BasicItem;

public class CameraActivity extends AppCompatActivity {
    public CoordinatorLayout coordinatorLayout;
    public Toolbar toolbar;
    public MenuItem showSwipedItems;
    private MenuItem spinnerMenuItem;
    public Spinner spinnerSelect;
    private FragmentManager fragmentManager;
    private Manager manager;
    private Button captureButton;
    private CameraView cameraView;
    private Fotoapparat fotoapparat;
    private Frame latestFrame;
    private OcrDetectorProcessor ocr;
    private LinearLayout bottomSheet;
    public BottomSheetBehavior bottomSheetBehavior;
    private BottomNavigationView bottomNavigationView;

    private Vibrator vibrator;
    private long[] PATTERN_FOUND = new long[]{0, 50};

    private int currentAngle;
    private boolean isPageSelected = false;
    private float lastFloat = 0.0f;

    private OcrDetectorProcessor.DetectedInterface detectedInterface = new OcrDetectorProcessor.DetectedInterface() {
        @Override
        public void FinishedProcessing(List<BasicItem> items, Bitmap bitmap, String pageId, int responseType) {
            switch (responseType) {
                //No new items detected
                case 0:
                    break;

                //New items detected
                case 1:
                    submitItems(items, bitmap, pageId, false);
                    break;

                //Single frame processed
                case 2:
                    setCaptureButtonEnabled(true);
                    submitItems(items, bitmap, pageId,true);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = new Manager(this);

        if (savedInstanceState != null) {
            isPageSelected = savedInstanceState.getBoolean("page_selected");
        }

        if (manager.getStoreId().contentEquals("0")) {
            startActivityForResult(new Intent(this, SettingsActivity.class), 1);
        } else if (checkPermissions()) {
            finishSetup();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        return !(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (checkPermissions()) {
                    finishSetup();
                } else {
                    finish();
                }
                break;
            }

            default:
                break;
        }
    }

    private void finishSetup() {
        setContentView(R.layout.activity_camera);

        MobileAds.initialize(this, this.getString(R.string.admob_key));
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

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
                final int rotation = currentAngle;
                setCaptureButtonEnabled(false);
                fotoapparat.takePicture().toBitmap().whenAvailable(new Function1<BitmapPhoto, Unit>() {
                    @Override
                    public Unit invoke(final BitmapPhoto bitmapPhoto) {
                        com.google.android.gms.vision.Frame frame = new com.google.android.gms.vision.Frame.Builder().setBitmap(bitmapPhoto.bitmap).build();
                        ocr.recognizeFrame(frame, rotation, false, true, ProductDetails.generateUUID());
                        return null;
                    }
                });
            }
        });

        cameraView = findViewById(R.id.camera_view);
        fotoapparat = createFotoapparat();

        bottomSheet = findViewById(R.id.bottom_sheet);
        LayoutTransition transition = new LayoutTransition();
        transition.setAnimateParentHierarchy(false);
        bottomSheet.setLayoutTransition(transition);

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        toolbar.setNavigationIcon(R.drawable.ic_close);
                        toolbar.setTitle("");
                        spinnerMenuItem.setVisible(true);
                        setOcrRunning(false);
                        break;

                    case BottomSheetBehavior.STATE_COLLAPSED:
                        toolbar.setNavigationIcon(null);
                        toolbar.setTitle(R.string.app_name);
                        spinnerMenuItem.setVisible(false);
                        setOcrRunning(true);
                        break;

                    default:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                LinearLayout buttonsContainer = findViewById(R.id.buttons_container);
                buttonsContainer.setVisibility((slideOffset>lastFloat) ? View.GONE : View.VISIBLE);
                lastFloat = slideOffset;
            }
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
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
        ocr = new OcrDetectorProcessor(this, detectedInterface);

        bottomNavigationView.setSelectedItemId(isPageSelected ? R.id.selector_page : R.id.selector_sku_upc);

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

    private void selectNavigation(boolean page) {
        isPageSelected = page;
        setCaptureButtonEnabled(page);
        setOcrRunning(!page);
    }

    private void setOcrRunning(boolean run) {
        if (run && !isPageSelected) {
            ocr.start();
        } else if (!run){
            ocr.stop(false);
        }
    }

    private void setCaptureButtonEnabled(boolean enabled) {
        captureButton.setEnabled(enabled);
        captureButton.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void submitItems(List<BasicItem> basicItems, Bitmap bitmap, String pageId, boolean save) {
        if (basicItems.size() != 0) {
            ProductDetails tempProductDetails = new ProductDetails();
            tempProductDetails.addBasicItems(basicItems);
            vibrateToast(null, Toast.LENGTH_SHORT, PATTERN_FOUND);
            getRecyclerFragment().queryApi(tempProductDetails, false);

            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            if (save) {
                manager.savePage(bitmap, pageId);
            }

        } else {
            Snackbar.make(coordinatorLayout, "No items detected", Snackbar.LENGTH_SHORT).show();
        }
    }

    public RecyclerFragment getRecyclerFragment() {
        return (RecyclerFragment) fragmentManager.findFragmentById(R.id.fragment_recycler);
    }

    public void vibrateToast(final CharSequence text, final int duration, final long[] pattern) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vibrator.vibrate(pattern, -1);
                if (text != null) {
                    Toast.makeText(getApplicationContext(), text, duration).show();
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
        List<Uri> uriList = new ArrayList<>();
        switch (requestCode) {
            case 0:
                if (data != null) {
                    if (data.getClipData() != null) {
                        for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                            uriList.add(data.getClipData().getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        uriList.add(data.getData());
                    }
                    ocr.recognizeSkusFromUris(uriList);
                    break;
                } else {
                    Snackbar.make(coordinatorLayout, "No image selected", Snackbar.LENGTH_SHORT).show();
                    break;
                }

            case 1:
                if (checkPermissions()) {
                    finishSetup();
                } else {
                    requestPermissions();
                }
                break;

            default:
                break;
        }
    }

    private boolean isSelectionState() {
        if (getRecyclerFragment() != null) {
            return getRecyclerFragment().isSelectionState();
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        if (!isSelectionState()) {
            inflater.inflate(R.menu.toolbar_menu, menu);
            showSwipedItems = menu.findItem(R.id.show_swiped);
            spinnerMenuItem = menu.findItem(R.id.list_selector);
            spinnerSelect = (Spinner)spinnerMenuItem.getActionView();

            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                toolbar.setTitle(R.string.app_name);
                spinnerMenuItem.setVisible(false);
            } else {
                toolbar.setTitle("");
                toolbar.setNavigationIcon(R.drawable.ic_close);
                spinnerMenuItem.setVisible(true);
            }
            toolbar.setTitleTextColor(getResources().getColor(R.color.colorWhite));
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            toolbar.getOverflowIcon().setColorFilter(getResources().getColor(R.color.colorWhite), PorterDuff.Mode.MULTIPLY);

            if (getRecyclerFragment() == null) {
                showSwipedItems.setVisible(false);
            } else {
                getRecyclerFragment().updateSwiped();
            }

            spinnerSelect.setOnItemSelectedListener(spinnerSelectListener);
            setupSpinner();

        } else {
            inflater.inflate(R.menu.toolbar_selected_menu, menu);
            toolbar.setTitleTextColor(getResources().getColor(R.color.colorBlack));
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorWhite));
            toolbar.getOverflowIcon().setColorFilter(getResources().getColor(R.color.colorBlack), PorterDuff.Mode.MULTIPLY);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        }
        return true;
    }

    private void setupSpinner() {
        if (manager.getReferences().isEmpty()) {
            manager.createList("Main List", new ProductDetails(), new Manager.OnCreateListCompleted() {
                @Override
                public void finished() {
                    spinnerSelect.setAdapter(createSpinnerAdapter(manager.getReferences()));
                }
            });
        } else {
            spinnerSelect.setAdapter(createSpinnerAdapter(manager.getReferences()));
        }
    }

    private ArrayAdapter<String> createSpinnerAdapter(List<String[]> references) {
        List<String> spinnerArray = new ArrayList<>();
        for (String[] item : references) {
            spinnerArray.add(item[0]);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private AdapterView.OnItemSelectedListener spinnerSelectListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            getRecyclerFragment().getRecyclerAdapter().refreshData(manager.getReferences().get(position));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //
        }
    };

    private boolean backNavPressed() {
        if (isSelectionState()) {
            getRecyclerFragment().getRecyclerAdapter().selectAll(true);
            return true;
        } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                backNavPressed();
                return true;

            case R.id.open_image:
                selectPhotos();
                return true;

            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (!backNavPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("page_selected", isPageSelected);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fotoapparat != null && checkPermissions()) {
            fotoapparat.start();
        } else if (fotoapparat != null) {
            recreate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fotoapparat != null) {
            fotoapparat.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fotoapparat != null && ocr != null) {
            fotoapparat.stop();
            ocr.stop(true);
        }
    }

    private class PreviewFrameProcessor implements FrameProcessor {
        @Override
        public void process(Frame frame) {
            latestFrame = frame;

            if (!ocr.isStopped() && ocr.isReadyForFrame()) {
                ocr.recognizeFrame(ocr.convertToGVFrame(frame), currentAngle, true, true, "-1");
            }
        }
    }
}
