package com.milburn.downstock;

import android.graphics.Bitmap;

public class ProductDetails {
    public String sku;
    public String upc;
    public String name;
    public String salePrice;
    public String image;
    public String url;
    public String modelNumber;

    public Bitmap imageBit;
    public boolean multiple_plano = false;
    public int pageNum = 0;
    public boolean found = false;
    public boolean deltabusted = false;
}
