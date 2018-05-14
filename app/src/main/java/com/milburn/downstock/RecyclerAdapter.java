package com.milburn.downstock;

import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;
import com.milburn.downstock.ProductDetails.DetailedItem;
import com.milburn.downstock.ProductDetails.BasicItem;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private ListReference selectedReference;
    private ProductDetails productDetails = new ProductDetails();
    private List<DetailedItem> detailedList = new ArrayList<>();
    private Set<DetailedItem> selectedSet = new HashSet<>();
    private List<DetailedItem> removedSet = new ArrayList<>();

    private DetailedItem removedItem = null;
    private int removedPosition = 0;

    private RecyclerFragment context;

    private boolean showSwiped = false;
    private boolean pendingActions = false;

    private Manager manager;
    private ListenerRegistration listenerRegistration;

    private Gson gson = new Gson();

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private View view;
        private TextView item_name;
        private TextView item_sku;
        private TextView item_upc;
        private TextView item_price;
        private TextView item_model;
        private ImageView item_image;
        private TextView multiple_plano;
        private ImageView item_selected;
        private TextView item_status;
        private TextView item_stock;

        private View div_bottom;
        private TextView div_status;

        public ViewHolder(View v) {
            super(v);
            view = v;
            item_name = v.findViewById(R.id.item_name);
            item_sku = v.findViewById(R.id.item_sku);
            item_upc = v.findViewById(R.id.item_upc);
            item_price = v.findViewById(R.id.item_price);
            item_model = v.findViewById(R.id.item_model);
            item_image = v.findViewById(R.id.item_image);
            multiple_plano = v.findViewById(R.id.multiple_plano);
            item_selected = v.findViewById(R.id.item_selected);
            item_status = v.findViewById(R.id.item_status);
            item_stock = v.findViewById(R.id.item_stock);

            div_bottom = v.findViewById(R.id.div_bottom);
            div_status = v.findViewById(R.id.div_status);
        }
    }

    public RecyclerAdapter(RecyclerFragment con) {
        setHasStableIds(true);
        context = con;
        manager = new Manager(con.getContext());
    }

    private RecyclerFragment getRecyclerFragment() {
        return context;
    }

    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cardview, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final DetailedItem item = detailedList.get(position);
        
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRecyclerFragment().setSelectedPosition(position);
                if (!getRecyclerFragment().isSelectionState()) {
                    view.showContextMenu();
                } else {
                    selectItem(position);
                }
            }
        });

        holder.view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                MenuInflater inflater = getRecyclerFragment().getActivity().getMenuInflater();
                inflater.inflate(R.menu.context_menu, menu);

                menu.setHeaderTitle("Item options");

                final String pageId = item.getPageId();
                manager.getPageExists(pageId, new Manager.OnGetPageExists() {
                    @Override
                    public void finished(boolean exists) {
                        if (!pageId.equals("-1") && exists) {
                            menu.getItem(1).setVisible(true);
                        }
                    }
                });

                if (item.isFound()) {
                    menu.add(Menu.NONE, 0, Menu.NONE, "Remove from 'found'");
                } else if (item.isDeltabusted()) {
                    menu.add(Menu.NONE, 0, Menu.NONE, "Remove from 'deltabusted'");
                }
            }
        });

        holder.view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                selectItem(position);
                return true;
            }
        });

        holder.item_name.setText(item.getName());
        holder.item_sku.setText(item.getSku());
        holder.item_upc.setText(item.getUpc());
        holder.item_price.setText("$" + item.getSalePrice());
        holder.item_model.setText(item.getModelNumber());

        Glide.with(context).asBitmap().load(item.getImageUrl()).into(holder.item_image);

        if (item.isInStock() && item.isLowStock()) {
            holder.item_stock.setText("Few in stock");
            holder.item_stock.setTextColor(context.getResources().getColor(R.color.colorTextOkay));
        } else if (item.isInStock()) {
            holder.item_stock.setText("In stock");
            holder.item_stock.setTextColor(context.getResources().getColor(R.color.colorTextGood));
        } else {
            holder.item_stock.setText("Not in stock");
            holder.item_stock.setTextColor(context.getResources().getColor(R.color.colorTextBad));
        }

        boolean showDiv = item.isDeltabusted() | item.isFound() | item.isMultiPlano();
        holder.div_bottom.setVisibility(showDiv ? View.VISIBLE : View.GONE);
        holder.multiple_plano.setVisibility(item.isMultiPlano() ? View.VISIBLE : View.GONE);

        holder.item_selected.setVisibility(isSelected(item) ? View.VISIBLE : View.GONE);

        if (item.isDeltabusted()) {
            holder.item_status.setText("Deltabusted");
            holder.item_status.setTextColor(context.getResources().getColor(R.color.colorTextBad));
            holder.item_status.setVisibility(View.VISIBLE);
            holder.div_status.setVisibility(item.isMultiPlano() ? View.VISIBLE : View.GONE);
        } else if (item.isFound()) {
            holder.item_status.setText("Found");
            holder.item_status.setTextColor(context.getResources().getColor(R.color.colorTextGood));
            holder.item_status.setVisibility(View.VISIBLE);
            holder.div_status.setVisibility(item.isMultiPlano() ? View.VISIBLE : View.GONE);
        } else {
            holder.item_status.setVisibility(View.GONE);
            holder.div_status.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return detailedList.size();
    }

    @Override
    public long getItemId(int position) {
        return detailedList.get(position).hashCode();
    }

    public ProductDetails getProductDetails() {
        return productDetails;
    }

    public ListReference getSelectedReference() {
        return selectedReference;
    }

    private void refreshData() {
        detailedList = showSwiped ? getProductDetails().getDetailedItems(true, false) : getProductDetails().getDetailedItems( false, true);
        notifyDataSetChanged();
        getRecyclerFragment().updateSelectionState();
        getRecyclerFragment().updateSwiped();
        if (getProductDetails().sizeSwipedItems() == 0) {
            setShowSwiped(false);
        }

        if (!pendingActions && selectedReference != null) {
            manager.saveList(getProductDetails(), selectedReference, null);
        }
    }

    public void refreshData(ListReference reference) {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        removedSet.clear();
        selectedSet.clear();
        removedItem = null;
        removedPosition = 0;

        selectedReference = reference;
        listenerRegistration = manager.getListDocReference(reference).addSnapshotListener(documentSnapshotEventListener);
        getRecyclerFragment().setRefreshing(true);
    }

    private EventListener<DocumentSnapshot> documentSnapshotEventListener = new EventListener<DocumentSnapshot>() {
        @Override
        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
            getRecyclerFragment().setRefreshing(false);
            if (documentSnapshot != null && documentSnapshot.exists()) {
                String prodJson = (String)documentSnapshot.get(selectedReference.getName());
                if (prodJson != null && !getProductDetails().toJson().contentEquals(prodJson)) {
                    productDetails = gson.fromJson(prodJson, ProductDetails.class);
                    if (productDetails != null ) {
                        refreshData();
                    }
                } else if (prodJson == null) {
                    listEmpty();
                }
            } else {
                listEmpty();
            }
        }
    };

    private void listEmpty() {
        manager.deleteReference(selectedReference, new Manager.OnDeletedReference() {
            @Override
            public void finished() {
                getRecyclerFragment().showSnack("'" + selectedReference.getName() + "' no longer exists", Snackbar.LENGTH_LONG);
                getRecyclerFragment().updateSelectionState();
            }
        });
    }

    public void setShowSwiped(boolean showSwiped) {
        if (showSwiped != this.showSwiped) {
            this.showSwiped = showSwiped;
            refreshData();
        }
    }

    public boolean isShowSwiped() {
        return showSwiped;
    }

    public void removeFromSwiped(int position) {
        getShownItem(position).resetSwiped();
        refreshData();
    }

    public DetailedItem getShownItem(int position) {
        if (detailedList.size() > 0) {
            return getProductDetails().getDetailedItem(detailedList.get(position).getSku());
        }
        return new DetailedItem();
    }

    public int getShownItemIndex(DetailedItem item) {
        return detailedList.indexOf(item);
    }

    public int sizeShownItems() {
        return detailedList.size();
    }

    public void setSelected(boolean selected, DetailedItem item) {
        if (selected) {
            selectedSet.add(item);
        } else {
            selectedSet.remove(item);
        }
        refreshData();
    }

    public void selectItem(int position) {
        setSelected(!isSelected(position), getShownItem(position));
    }

    public void selectAll(boolean deselect) {
        if (deselect) {
            getSelectedSet().clear();
        } else {
            getSelectedSet().addAll(detailedList);
        }
        refreshData();
    }

    public Set<DetailedItem> getSelectedSet() {
        return selectedSet;
    }

    public int selectedSetSize() {
        return selectedSet.size();
    }

    public boolean isSelected(DetailedItem item) {
        return selectedSet.contains(item);
    }

    public boolean isSelected(int position) {
        return isSelected(getShownItem(position));
    }

    public void insertItem(int position, DetailedItem item) {
        setSelected(false, item);
        BasicItem basicItem = new BasicItem(item.getSku(), item.getUpc(), item.getPageId());
        basicItem.setMultiPlano(item.isMultiPlano());
        getProductDetails().addBasicItem(basicItem);
        getProductDetails().addDetailedItem(position, item);
        refreshData();
    }

    public void insertItems(List<DetailedItem> items) {
        pendingActions = true;
        for (DetailedItem item : items) {
            insertItem(0, item);
        }
        pendingActions = false;
        refreshData();
    }

    public void removeItem(int position) {
        DetailedItem item = getShownItem(position);
        removedItem = item;
        removedPosition = position;
        removedSet.add(item);

        getProductDetails().removeBasicItem(item);
        getProductDetails().removeDetailedItem(item.getSku());

        refreshData();
    }

    public void removeSelectedItems() {
        pendingActions = true;
        for (DetailedItem item : selectedSet) {
            removeItem(getShownItemIndex(item));
        }
        selectAll(true);
        pendingActions = false;
        refreshData();
    }

    public void undoRemoveAll() {
        insertItems(removedSet);
    }

    public void undoRemove() {
        insertItem(removedPosition, removedItem);
    }
}
