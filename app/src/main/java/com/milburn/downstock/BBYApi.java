package com.milburn.downstock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import org.jsoup.Jsoup;

import com.milburn.downstock.ProductDetails.ItemStoreInfo;
import com.milburn.downstock.ProductDetails.DetailedItem;

public class BBYApi extends AsyncTask<ProductDetails, Integer, ProductDetails> {

    public AsyncResponse delegate;
    private Context context;
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();
    private int maxAttempts = 3;

    public BBYApi(Context con, AsyncResponse asyncResponse) {
        context = con;
        delegate = asyncResponse;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(con);
    }

    public interface AsyncResponse {
        void processFinish(ProductDetails result);
    }

    @Override
    protected ProductDetails doInBackground(ProductDetails... params) {
        if (params[0].sizeDetailedItems() > 0) {
            return getUpdatedStoreInfo(params[0]);
        } else {
            return getDetailedItems(params[0]);
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

    private ProductDetails getDetailedItems(ProductDetails productDetails) {
        int numPages = (int) Math.ceil(productDetails.sizeBasicItems() / 100.0);
        String[] itemIds = productDetails.getAllBasicIds();

        for (int i = 1; i <= numPages; i++) {
            String itemsUrl = "https://api.bestbuy.com/v1/products(sku%20in(" + itemIds[0] + ")|upc%20in(" + itemIds[1] + "))?format=json&show=sku,upc,name,salePrice,image,url,modelNumber&pageSize=100&page=" + i + "&apiKey=" + context.getString(R.string.bbyapi);
            String itemsResult = null;
            int count = 0;
            while (count <= maxAttempts && itemsResult == null) {
                count++;
                try {
                    itemsResult = Jsoup.connect(itemsUrl).ignoreContentType(true).execute().body();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (itemsResult != null) {
                ProductDetails tempProductDetails = gson.fromJson(itemsResult, ProductDetails.class);
                productDetails.addDetailedItems(tempProductDetails.getDetailedItems());
            }
        }

        for (DetailedItem item : productDetails.getDetailedItems()) {
            item.setPageId(productDetails.getBasicItem(item).getPageId());
            item.setMultiPlano(productDetails.getBasicItem(item).isMutiPlano());
            item.setStock(getStoreInfo(item));
        }
        return productDetails;
    }

    @Override
    protected void onPostExecute(ProductDetails result) {
        delegate.processFinish(result);
    }
}
