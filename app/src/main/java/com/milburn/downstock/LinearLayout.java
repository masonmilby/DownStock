package com.milburn.downstock;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

public class LinearLayout extends LinearLayoutManager{
    public LinearLayout(Context context) {
        super(context);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return true;
    }
}
