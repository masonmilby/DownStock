package com.milburn.downstock;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProductDetails {
    private List<BasicItem> basicList = new ArrayList<>();
    private List<DetailedItem> products = new ArrayList<>();

    private boolean showSwiped = false;

    public static class BasicItem {
        private final String sku;
        private final String upc;
        private final String pageId;
        private boolean multiPlano = false;

        public BasicItem(String sku, String upc, String pageId) {
            this.sku = sku;
            this.upc = upc;
            this.pageId = pageId;
        }

        public String getSku() {
            return sku;
        }

        public String getUpc() {
            return upc;
        }

        public String getId() {
            return getSku().contentEquals("") ? getUpc() : getSku();
        }

        public String getPageId() {
            return pageId;
        }

        public boolean isMutiPlano() {
            return multiPlano;
        }

        public void setMultiPlano(boolean multiPlano) {
            this.multiPlano = multiPlano;
        }
    }

    public static class DetailedItem {
        private String sku;
        private String upc;
        private String name;
        private String salePrice;
        private String image;
        private String url;
        private String modelNumber;

        private String imageBit;
        private boolean multiPlano = false;
        private String pageId = "-1";
        private boolean found = false;
        private boolean deltabusted = false;
        private boolean selected = false;
        private boolean inStock = false;
        private boolean lowStock = false;

        public void setStock(ItemStoreInfo info) {
            inStock = info.isInStock();
            lowStock = info.isLowStock();
        }

        public void setPageId(String pageId) {
            this.pageId = pageId;
        }

        public void setImageBit(Bitmap imageBit) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            imageBit.compress(Bitmap.CompressFormat.PNG, 100, bos);
            byte[] b = bos.toByteArray();

            this.imageBit = Base64.encodeToString(b, Base64.DEFAULT);
        }

        public void setFound(boolean found) {
            this.deltabusted = false;
            this.found = found;
        }

        public void setDeltabusted(boolean deltabusted) {
            this.found = false;
            this.deltabusted = deltabusted;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public void setMultiPlano(boolean multi) {
            this.multiPlano = multi;
        }

        public String getSku() {
            return sku;
        }

        public String getUpc() {
            return upc;
        }

        public String getName() {
            return name;
        }

        public String getSalePrice() {
            return salePrice;
        }

        public String getImage() {
            return image;
        }

        public String getUrl() {
            return url;
        }

        public String getModelNumber() {
            return modelNumber;
        }

        public Bitmap getImageBit() {
            byte[] decodedString = Base64.decode(imageBit, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        }

        public boolean isMultiPlano() {
            return multiPlano;
        }

        public String getPageId() {
            return pageId;
        }

        public boolean isFound() {
            return found;
        }

        public boolean isDeltabusted() {
            return deltabusted;
        }

        public boolean isSelected() {
            return selected;
        }

        public boolean isSwiped() {
            return isDeltabusted() | isFound();
        }

        public void resetSwiped() {
            setDeltabusted(false);
            setFound(false);
        }

        public boolean isInStock() {
            return inStock;
        }

        public boolean isLowStock() {
            return lowStock;
        }


    }

    public static class ItemStoreInfo {
        private List<Stock> stores = new ArrayList<>();

        private static class Stock {
            private boolean lowStock;
        }

        public boolean isInStock() {
            return stores.size() == 1;
        }

        public boolean isLowStock() {
            if (isInStock()) {
                return stores.get(0).lowStock;
            }
            return false;
        }

    }

    public String[] getAllBasicIds() {
        String combinedSkus = "";
        String combinedUpcs = "";

        for (BasicItem item : getBasicItems()) {
            if (item.getId().length() == 7) {
                combinedSkus = combinedSkus.concat(combinedSkus.length() == 0 ? item.getSku() : "," + item.getSku());
            } else {
                combinedUpcs = combinedUpcs.concat(combinedUpcs.length() == 0 ? item.getUpc() : "," + item.getUpc());
            }
        }

        if (combinedSkus.length() == 0) {
            combinedSkus = "1";
        }
        if (combinedUpcs.length() == 0) {
            combinedUpcs = "1";
        }

        return new String[]{combinedSkus, combinedUpcs};
    }

    public List<BasicItem> getBasicItems() {
        return basicList;
    }

    public BasicItem getBasicItem(BasicItem item) {
        if (getBasicItems().contains(item)) {
            return getBasicItems().get(getBasicItems().indexOf(item));
        }
        return null;
    }

    public BasicItem getBasicItem(String id) {
        for (BasicItem item : getBasicItems()) {
            if (item.getSku().contentEquals(id) | item.getUpc().contentEquals(id)) {
                return item;
            }
        }
        return null;
    }

    public BasicItem getBasicItem(int position) {
        if (getBasicItems().size() > position && position >= 0) {
            return getBasicItems().get(position);
        }
        return null;
    }

    public Integer sizeBasicItems() {
        return getBasicItems().size();
    }

    public List<BasicItem> addBasicItem(BasicItem item) {
        if (item != null) {
            if (item.getPageId().equals("-1") && getBasicItem(item.getId()) == null) {
                getBasicItems().add(item);
            } else {
                if (getBasicItem(item.getId()) != null) {
                    getBasicItem(item.getId()).setMultiPlano(true);
                } else {
                    getBasicItems().add(item);
                }
            }
        }
        return getBasicItems();
    }

    public List<BasicItem> addBasicItem(int position, BasicItem item) {
        if (item != null && position >= 0) {
            getBasicItems().add(position, item);
            return getBasicItems();
        }
        return getBasicItems();
    }

    public List<BasicItem> addBasicItems(List<BasicItem> basicList) {
        for (BasicItem item : basicList) {
            addBasicItem(item);
        }
        return getBasicItems();
    }

    public List<BasicItem> removeBasicItem(BasicItem item) {
        if (item != null) {
            getBasicItems().remove(item);
            return getBasicItems();
        }
        return getBasicItems();
    }

    public List<BasicItem> removeBasicItem(String sku) {
        getBasicItems().remove(getBasicItem(sku));
        return getBasicItems();
    }

    public List<BasicItem> removeBasicItem(int position) {
        if (position < sizeBasicItems() && position >= 0) {
            getBasicItems().remove(position);
            return getBasicItems();
        }
        return getBasicItems();
    }

    public void addDetailedItems(List<DetailedItem> itemList) {
        getDetailedItems().addAll(itemList);
    }

    public List<DetailedItem> getDetailedItems() {
        return products;
    }

    public List<DetailedItem> getShownItems() {
        return getDetailedItems(true, isShowSwiped(), !isShowSwiped());
    }

    public DetailedItem getShownItem(int position) {
        return getShownItems().get(position);
    }

    public Integer getShownItemIndex(DetailedItem item) {
        return getShownItems().indexOf(item);
    }

    public void removeShownItem(int position) {
        removeDetailedItem(getShownItem(position));
    }

    public List<DetailedItem> getDetailedItems(boolean getSelected, boolean getSwiped, boolean getOthers) {
        List<DetailedItem> tempList = new ArrayList<>();
        for (DetailedItem item : getDetailedItems()) {
            if (getSelected && item.isSelected()) {
                if (!tempList.contains(item)) {
                    tempList.add(item);
                }
            }
            if (getSwiped && item.isSwiped()) {
                if (!tempList.contains(item)) {
                    tempList.add(item);
                }
            }
            if (getOthers && !item.isSelected() && !item.isSwiped()) {
                if (!tempList.contains(item)) {
                    tempList.add(item);
                }
            }
        }
        return tempList;
    }

    public boolean isShowSwiped() {
        return showSwiped;
    }

    public void setShowSwiped(boolean show) {
        showSwiped = show;
    }

    public Integer sizeShownItems() {
        return getShownItems().size();
    }

    public Integer sizeDetailedItems() {
        return getDetailedItems().size();
    }

    public Integer sizeSwipedItems() {
        return getDetailedItems(false, true, false).size();
    }

    public Integer sizeSelectedItems() {
        return getDetailedItems(true, false, false).size();
    }

    public DetailedItem getDetailedItem(DetailedItem item) {
        if (getDetailedItems().contains(item)) {
            return getDetailedItems().get(getDetailedItems().indexOf(item));
        }
        return null;
    }

    public DetailedItem getDetailedItem(int position) {
        if (getDetailedItems().size() > position && position >= 0) {
            return getDetailedItems().get(position);
        }
        return null;
    }

    public List<DetailedItem> addDetailedItem(DetailedItem item) {
        if (item != null) {
            getDetailedItems().add(item);
            return getDetailedItems();
        }
        return getDetailedItems();
    }

    public DetailedItem getDetailedItem(String id) {
        for (DetailedItem item : getDetailedItems()) {
            if (item.getSku().contentEquals(id) | item.getUpc().contentEquals(id)) {
                return item;
            }
        }
        return null;
    }

    public List<DetailedItem> addDetailedItem(int position, DetailedItem item) {
        if (item != null && position >= 0 && getDetailedItem(item.getSku()) == null) {
            getDetailedItems().add(position, item);
            return getDetailedItems();
        }
        return getDetailedItems();
    }

    public List<DetailedItem> removeDetailedItem(DetailedItem item) {
        if (item != null) {
            getDetailedItems().remove(item);
            return getDetailedItems();
        }
        return getDetailedItems();
    }

    public List<DetailedItem> removeDetailedItem(int position) {
        if (position < sizeDetailedItems() && position >= 0) {
            getDetailedItems().remove(position);
            return getDetailedItems();
        }
        return getDetailedItems();
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this, ProductDetails.class);
    }

    public static String generateUUID() {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }
}