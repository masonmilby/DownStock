package com.milburn.downstock;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.milburn.downstock.ProductDetails.ItemStoreInfo;
import com.milburn.downstock.ProductDetails.DetailedItem;
import com.milburn.downstock.ProductDetails.BasicItem;

public class BBYApi extends AsyncTask<Object, Void, Object> {

    public AsyncResponse delegate;
    private MainActivity context;
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();
    private int maxAttempts = 3;

    public BBYApi(MainActivity con, AsyncResponse asyncResponse) {
        context = con;
        delegate = asyncResponse;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(con);
    }

    public interface AsyncResponse {
        void processFinish(Object result);
    }

    @Override
    protected Object doInBackground(Object... params) {
        context.showProgress(false,false,0);
        Object object = params[0];

        if (object instanceof ProductDetails) {
            ProductDetails productDetails = (ProductDetails)object;
            if (productDetails.sizeDetailedItems() > 0) {
                return getUpdatedStoreInfo(productDetails);
            } else {
                return getDetailedItems(productDetails);
            }
        } else if (object instanceof BasicItem) {
            return getDetailedItem((BasicItem)object);
        }
        return object;
    }

    private Bitmap getBitmap(String url) {
        try {
            InputStream inputStream = (InputStream) new URL(url).getContent();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ItemStoreInfo getStoreInfo(DetailedItem item) {
        String availUrl = "https://api.bestbuy.com/v1/products/" + item.getSku() + "/stores.json?storeId="+ sharedPreferences.getString("store_id_pref", "0") + "&apiKey=" + context.getString(R.string.bbyapi);
        String availResult = null;
        int count = 0;
        while (count <= maxAttempts && availResult == null) {
            count++;
            try {
                availResult = Jsoup.connect(availUrl).ignoreContentType(true).execute().body();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (availResult != null) {
            return gson.fromJson(availResult, ItemStoreInfo.class);
        }
        return new ItemStoreInfo();
    }

    private ProductDetails getUpdatedStoreInfo(ProductDetails productDetails) {
        for (DetailedItem item : productDetails.getDetailedItems()) {
            item.setStock(getStoreInfo(item));
        }
        return productDetails;
    }

    private DetailedItem getDetailedItem(BasicItem basicItem) {
        String itemUrl = "https://api.bestbuy.com/v1/products/" + basicItem.getSku() + ".json?format=json&show=sku,upc,name,salePrice,image,url,modelNumber&apiKey=" + context.getString(R.string.bbyapi);
        String itemResult = null;
        int count = 0;
        while (count <= maxAttempts && itemResult == null) {
            count++;
            try {
                itemResult = Jsoup.connect(itemUrl).ignoreContentType(true).execute().body();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (itemResult != null) {
            DetailedItem detailedItem = gson.fromJson(itemResult, DetailedItem.class);
            detailedItem.setImageBit(getBitmap(detailedItem.getImage()));
            detailedItem.setPageNum(basicItem.getPageNum());
            detailedItem.setMultiPlano(basicItem.isMutiPlano());
            detailedItem.setStock(getStoreInfo(detailedItem));

            return detailedItem;
        }
        return null;
    }

    private ProductDetails getDetailedItems(ProductDetails productDetails) {
        int numPages = (int) Math.ceil(productDetails.sizeBasicItems() / 100.0);

        for (int i = 1; i <= numPages; i++) {
            String itemUrl = "https://api.bestbuy.com/v1/products(sku%20in(" + productDetails.getAllBasicSkus() + "))?format=json&show=sku,upc,name,salePrice,image,url,modelNumber&pageSize=100&page=" + i + "&apiKey=" + context.getString(R.string.bbyapi);
            String itemResult = null;
            int count = 0;
            while (count <= maxAttempts && itemResult == null) {
                count++;
                try {
                    itemResult = Jsoup.connect(itemUrl).ignoreContentType(true).execute().body();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (itemResult != null) {
                ProductDetails tempProductDetails = gson.fromJson(itemResult, ProductDetails.class);
                productDetails.addDetailedItems(tempProductDetails.getDetailedItems());
            }
        }

        for (DetailedItem item : productDetails.getDetailedItems()) {
            context.showProgress(true, true, 100/productDetails.sizeDetailedItems());
            item.setImageBit(getBitmap(item.getImage()));
            item.setPageNum(productDetails.getBasicItem(item.getSku()).getPageNum());
            item.setMultiPlano(productDetails.getBasicItem(item.getSku()).isMutiPlano());
            item.setStock(getStoreInfo(item));
        }
        context.showProgress(false, false, 0);
        return productDetails;
    }

    @Override
    protected void onPostExecute(Object result) {
        delegate.processFinish(result);
    }
}
