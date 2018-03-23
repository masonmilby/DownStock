package com.milburn.downstock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.google.gson.Gson;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BBYApi extends AsyncTask<List<String[]>, Void, List<ProductDetails>> {

    public AsyncResponse delegate = null;
    private Context context;

    public BBYApi(Context con, AsyncResponse asyncResponse) {
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
            if (!tempSkuSet.add(item[0])) {
                for (ProductDetails prod : productList) {
                    if (prod.sku.equals(item[0])) {
                        prod.multiple_plano = true;
                    }
                }
            } else {
                String url = "https://api.bestbuy.com/v1/products/" + item[0] + ".json?format=json&show=sku,upc,name,salePrice,image,url,modelNumber&apiKey=" + context.getString(R.string.bbyapi);
                String result = null;
                try {
                    result = Jsoup.connect(url).ignoreContentType(true).execute().body();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (result != null) {
                    ProductDetails prod = gson.fromJson(result, ProductDetails.class);
                    prod.imageBit = getBitmap(prod.image);
                    prod.pageNum = Integer.parseInt(item[1]);
                    productList.add(prod);
                }
            }
        }

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
