package com.milburn.downstock;

import android.content.Context;
import android.util.ArrayMap;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MapListReferences {
    private Map<String, List<String>> finalMapReferences = new ArrayMap<>();
    private HashSet<ListReference> listReferences = new HashSet<>();

    public MapListReferences(List<String> stringArray) {
        if (stringArray != null) {
            for (String string : stringArray) {
                String[] splitString = string.split(":");
                addListReference(new ListReference(splitString[0], splitString[1]));
            }
        }
    }

    public Map<String, List<String>> getFinalMapReferences() {
        finalMapReferences.clear();
        finalMapReferences.put("References", getStringArray());
        return finalMapReferences;
    }

    private List<String> getStringArray() {
        List<String> finalListArray = new ArrayList<>();
        for (ListReference reference : getListReferences()) {
            finalListArray.add(reference.createString());
        }
        return finalListArray;
    }

    public HashSet<ListReference> getListReferences() {
        return listReferences;
    }

    public ArrayAdapter<ListReference> createSpinnerAdapter(Context context) {
        ArrayAdapter<ListReference> adapter = new ArrayAdapter<>(context, R.layout.custom_spinner_item, new ArrayList<>(getListReferences()));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    public void addListReference(ListReference listReference) {
        getListReferences().add(listReference);
    }

    public void removeListReference(ListReference listReference) {
        getListReferences().remove(listReference);
    }

    public boolean isEmpty() {
        return getListReferences().isEmpty();
    }
}
