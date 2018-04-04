package com.milburn.downstock;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        surfaceView = findViewById(R.id.surface_camera);
        surfaceView.getHolder().addCallback(this);

        BottomSheetBehavior sheet = BottomSheetBehavior.from((LinearLayout)findViewById(R.id.bottom_sheet));
        sheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                //
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        OcrDetectorProcessor ocrDetectorProcessor = new OcrDetectorProcessor(this);
        if (ocrDetectorProcessor.isRecognizerReady()) {
            ocrDetectorProcessor.createCameraSource(surfaceView.getHolder(), width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
