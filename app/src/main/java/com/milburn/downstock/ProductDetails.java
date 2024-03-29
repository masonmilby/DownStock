package com.milburn.downstock;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class ProductDetails {
    private List<BasicItem> basicList = new ArrayList<>();
    private List<DetailedItem> products = new ArrayList<>();

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

        private boolean multiPlano = false;
        private String pageId = "-1";
        private boolean found = false;
        private boolean deltabusted = false;
        private boolean inStock = false;
        private boolean lowStock = false;

        public void setStock(ItemStoreInfo info) {
            inStock = info.isInStock();
            lowStock = info.isLowStock();
        }

        public void setPageId(String pageId) {
            this.pageId = pageId;
        }

        public void setFound(boolean found) {
            this.deltabusted = false;
            this.found = found;
        }

        public void setDeltabusted(boolean deltabusted) {
            this.found = false;
            this.deltabusted = deltabusted;
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

        public String getImageUrl() {
            return image;
        }

        public String getUrl() {
            return url;
        }

        public String getModelNumber() {
            return modelNumber;
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

    public BasicItem getBasicItem(DetailedItem item) {
        for (BasicItem basicItem : getBasicItems()) {
            if (basicItem.getSku().contentEquals(item.getSku()) | basicItem.getUpc().contentEquals(item.getUpc())) {
                return basicItem;
            }
        }
        return null;
    }

    public BasicItem getBasicItem(String id) {
        for (BasicItem basicItem : getBasicItems()) {
            if (basicItem.getSku().contentEquals(id) | basicItem.getUpc().contentEquals(id)) {
                return basicItem;
            }
        }
        return null;
    }

    public Integer sizeBasicItems() {
        return getBasicItems().size();
    }

    public void addBasicItem(BasicItem item) {
        if (item != null) {
            switch (item.getPageId()) {
                case "-1":
                    if (getBasicItem(item.getId()) == null) {
                        getBasicItems().add(item);
                    }
                    break;

                default:
                    if (getBasicItem(item.getId()) == null) {
                        getBasicItems().add(item);
                    } else {
                        getBasicItem(item.getId()).setMultiPlano(true);
                    }
                    break;
            }
        }
    }

    public void addBasicItems(List<BasicItem> basics) {
        for (BasicItem item : basics) {
            addBasicItem(item);
        }
    }

    public List<BasicItem> removeBasicItem(BasicItem item) {
        getBasicItems().remove(getBasicItem(item.getId()));
        return getBasicItems();
    }

    public List<BasicItem> removeBasicItem(DetailedItem item) {
        getBasicItems().remove(getBasicItem(item));
        return getBasicItems();
    }

    public void addDetailedItems(List<DetailedItem> itemList) {
        getDetailedItems().addAll(itemList);
    }

    public List<DetailedItem> getDetailedItems() {
        return products;
    }

    public List<DetailedItem> getDetailedItems(boolean getSwiped, boolean getOthers) {
        List<DetailedItem> tempList = new ArrayList<>();
        for (DetailedItem item : getDetailedItems()) {
            if (getSwiped && item.isSwiped()) {
                if (!tempList.contains(item)) {
                    tempList.add(item);
                }
            }
            if (getOthers && !item.isSwiped()) {
                if (!tempList.contains(item)) {
                    tempList.add(item);
                }
            }
        }
        return tempList;
    }

    public Integer sizeDetailedItems() {
        return getDetailedItems().size();
    }

    public Integer sizeSwipedItems() {
        return getDetailedItems(true, false).size();
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

    public List<DetailedItem> removeDetailedItem(String id) {
        getDetailedItems().remove(getDetailedItem(id));
        return getDetailedItems();
    }

    public HashSet<String> getAllPageIds() {
        HashSet<String> idList = new HashSet<>();
        for (DetailedItem item : getDetailedItems()) {
            idList.add(item.getPageId());
        }
        return idList;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this, ProductDetails.class);
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}