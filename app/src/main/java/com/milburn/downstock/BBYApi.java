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

public class BBYApi extends AsyncTask<ProductDetails, Void, ProductDetails> {

    public AsyncResponse delegate;
    private MainActivity context;
    private SharedPreferences sharedPreferences;

    public BBYApi(MainActivity con, AsyncResponse asyncResponse) {
        context = con;
        delegate = asyncResponse;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(con);
    }

    public interface AsyncResponse {
        void processFinish(ProductDetails result);
    }

    @Override
    protected ProductDetails doInBackground(ProductDetails... params) {
        context.showProgress(false,false,0);
        Gson gson = new Gson();
        String itemUrl = "https://api.bestbuy.com/v1/products(sku%20in(" + params[0].getAllBasicSkus() + "))?format=json&show=sku,upc,name,salePrice,image,url,modelNumber&pageSize=100&apiKey=" + context.getString(R.string.bbyapi);
        String availUrl = "https://api.bestbuy.com/v1/products/" + params[0].getBasicItem(0).getSku() + "/stores.json?storeId="+ sharedPreferences.getString("store_id_pref", "0") + "&apiKey=" + context.getString(R.string.bbyapi);
        String itemResult = null;
        String availResult = null;
        try {
            itemResult = Jsoup.connect(itemUrl).ignoreContentType(true).execute().body();
            System.out.println(itemResult);
            availResult = Jsoup.connect(availUrl).ignoreContentType(true).execute().body();
            System.out.println(availResult);
        } catch (IOException e) {
            e.printStackTrace();
            if (e.getMessage().contains("Status=403")) {
                //
            }
        }
        if (itemResult != null && availResult != null) {
            ProductDetails productDetails = gson.fromJson(itemResult, ProductDetails.class);
            ItemStoreInfo itemStoreInfo = gson.fromJson(availResult, ItemStoreInfo.class);

            System.out.println("Size:" + productDetails.sizeDetailedItems());

            for (DetailedItem item : productDetails.getDetailedItems()) {
                System.out.println(item.getSku());
                item.setImageBit(getBitmap(item.getImage()));
                item.setPageNum(params[0].getBasicItem(item.getSku()).getPageNum());
                item.setStock(itemStoreInfo);
            }

            return productDetails;
        }
        return null;
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

    @Override
    protected void onPostExecute(ProductDetails result) {
        delegate.processFinish(result);
    }
}
