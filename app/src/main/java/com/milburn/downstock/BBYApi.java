package com.milburn.downstock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import org.jsoup.Jsoup;

import com.milburn.downstock.ProductDetails.ItemStoreInfo;
import com.milburn.downstock.ProductDetails.DetailedItem;
import com.milburn.downstock.ProductDetails.BasicItem;

public class BBYApi extends AsyncTask<Object, Integer, Object> {

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
        void processFinish(Object result);
    }

    @Override
    protected Object doInBackground(Object... params) {
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
            detailedItem.setPageId(basicItem.getPageId());
            detailedItem.setMultiPlano(basicItem.isMutiPlano());
            detailedItem.setStock(getStoreInfo(detailedItem));

            return detailedItem;
        }
        return null;
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
            item.setMultiPlano(productDetails.getBasicItem(item.getSku()).isMutiPlano());
            item.setStock(getStoreInfo(item));
        }
        return productDetails;
    }

    @Override
    protected void onPostExecute(Object result) {
        delegate.processFinish(result);
    }
}
