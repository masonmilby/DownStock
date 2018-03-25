package com.milburn.downstock;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BBYApi extends AsyncTask<List<String[]>, Void, List<ProductDetails>> {

    public AsyncResponse delegate = null;
    private MainActivity context;

    public BBYApi(MainActivity con, AsyncResponse asyncResponse) {
        context = con;
        delegate = asyncResponse;
    }

    public interface AsyncResponse {
        void processFinish(List<ProductDetails> result);
    }

    @Override
    protected List<ProductDetails> doInBackground(List<String[]>... params) {
        Gson gson = new Gson();
        List<ProductDetails> productList = new ArrayList<>();
        Set<String> tempSkuSet = new HashSet<>();
        for (String[] item : params[0]) {
            context.showProgress(true,true,100/params[0].size());
            if (!tempSkuSet.add(item[0])) {
                for (ProductDetails prod : productList) {
                    if (prod.sku.equals(item[0])) {
                        prod.multiple_plano = true;
                    }
                }
            } else {
                String itemUrl = "https://api.bestbuy.com/v1/products/" + item[0] + ".json?format=json&show=sku,upc,name,salePrice,image,url,modelNumber&apiKey=" + context.getString(R.string.bbyapi);
                String availUrl = "https://api.bestbuy.com/v1/products/" + item[0] + "/stores.json?storeId=161&apiKey=" + context.getString(R.string.bbyapi);
                String itemResult = null;
                String availResult = null;
                try {
                    itemResult = Jsoup.connect(itemUrl).ignoreContentType(true).execute().body();
                    availResult = Jsoup.connect(availUrl).ignoreContentType(true).execute().body();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (itemResult != null) {
                    ProductDetails prod;
                    if (availResult != null) {
                        Map itemGsonMap = gson.fromJson(itemResult, Map.class);
                        Map availGsonMap = gson.fromJson(availResult, Map.class);
                        itemGsonMap.putAll(availGsonMap);

                        prod = gson.fromJson(gson.toJson(itemGsonMap), ProductDetails.class);
                    } else {
                        prod = gson.fromJson(itemResult, ProductDetails.class);
                    }

                    prod.imageBit = getBitmap(prod.image);
                    prod.pageNum = Integer.parseInt(item[1]);
                    if (prod.stores.size() == 0) {
                        prod.inStock = false;
                    }
                    prod.sku = prod.sku.replace(".0","");

                    productList.add(prod);
                }
            }
        }
        context.showProgress(false, false, 0);

        return productList;
    }

    public Bitmap getBitmap(String url) {
        try {
            InputStream inputStream = (InputStream) new URL(url).getContent();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<ProductDetails> result) {
        delegate.processFinish(result);
    }
}
