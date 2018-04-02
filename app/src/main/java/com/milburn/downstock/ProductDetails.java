package com.milburn.downstock;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class ProductDetails {
    private List<BasicItem> basicList = new ArrayList<>();
    private List<DetailedItem> products = new ArrayList<>();

    private boolean showSwiped = false;

    public static class BasicItem {
        private final String sku;
        private final int pageNum;
        private boolean multiPlano = false;

        public BasicItem(String sku, int pageNum) {
            this.sku = sku;
            this.pageNum = pageNum;
        }

        public String getSku() {
            return sku;
        }

        public int getPageNum() {
            return pageNum;
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

        private Bitmap imageBit;
        private boolean multiPlano = false;
        private int pageNum = 0;
        private boolean found = false;
        private boolean deltabusted = false;
        private boolean selected = false;
        private boolean inStock = false;
        private boolean lowStock = false;

        public void setStock(ItemStoreInfo info) {
            inStock = info.isInStock();
            lowStock = info.isLowStock();
        }

        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }

        public void setImageBit(Bitmap imageBit) {
            this.imageBit = imageBit;
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
            return imageBit;
        }

        public boolean isMultiPlano() {
            return multiPlano;
        }

        public int getPageNum() {
            return pageNum;
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

    public String getAllBasicSkus() {
        String combinedSkus = "";
        for (BasicItem item : getBasicItems()) {
            combinedSkus = combinedSkus.concat(combinedSkus.contentEquals("") ? item.getSku() : "," + item.getSku());
        }
        return combinedSkus;
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

    public BasicItem getBasicItem(String sku) {
        for (BasicItem item : getBasicItems()) {
            if (item.getSku().contentEquals(sku)) {
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
            if (getBasicItem(item.getSku()) != null) {
                getBasicItem(item.getSku()).setMultiPlano(true);
            } else {
                getBasicItems().add(item);
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

    public List<BasicItem> removeBasicItem(BasicItem item) {
        if (item != null) {
            getBasicItems().remove(item);
            return getBasicItems();
        }
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

    public List<DetailedItem> addDetailedItem(int position, DetailedItem item) {
        if (item != null && position >= 0) {
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
}