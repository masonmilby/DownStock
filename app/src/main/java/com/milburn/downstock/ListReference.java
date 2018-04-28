package com.milburn.downstock;

import android.util.ArrayMap;

import java.util.Map;
import java.util.Objects;

public class ListReference {
    private String name;
    private String userId;

    public ListReference(String name, String userId) {
        this.name = name;
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String createString() {
        return getName() + ":" + getUserId();
    }

    public Map<String, String> createMap(ProductDetails productDetails) {
        Map<String, String> tempMap = new ArrayMap<>();
        tempMap.put(getName(), productDetails.toJson());
        return tempMap;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListReference that = (ListReference) o;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getUserId(), that.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getUserId());
    }
}
