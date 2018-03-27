package com.milburn.downstock;

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
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private List<ProductDetails> itemDataset = new ArrayList<>();
    private MainActivity context;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private View view;
        private TextView item_name;
        private TextView item_sku;
        private TextView item_upc;
        private TextView item_price;
        private TextView item_model;
        private ImageView item_image;
        private TextView multiple_plano;
        private TextView page_num;
        private ImageView item_selected;
        private TextView item_status;
        private TextView item_stock;

        public ViewHolder(View v) {
            super(v);
            view = v;
            item_name = v.findViewById(R.id.item_name);
            item_sku = v.findViewById(R.id.item_sku);
            item_upc = v.findViewById(R.id.item_upc);
            //item_price = v.findViewById(R.id.item_price);
            item_model = v.findViewById(R.id.item_model);
            item_image = v.findViewById(R.id.item_image);
            multiple_plano = v.findViewById(R.id.multiple_plano);
            page_num = v.findViewById(R.id.page_num);
            item_selected = v.findViewById(R.id.item_selected);
            item_status = v.findViewById(R.id.item_status);
            item_stock = v.findViewById(R.id.item_stock);
        }
    }

    public RecyclerAdapter(List<ProductDetails> data, MainActivity con) {
        itemDataset = data;
        context = con;
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
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.selectedPosition = position;
                if (!context.selectionState) {
                    view.showContextMenu();
                } else {
                    context.selectItem(position);
                }
            }
        });

        holder.view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                MenuInflater inflater = context.getMenuInflater();
                inflater.inflate(R.menu.context_menu, menu);

                menu.setHeaderTitle("Item options");

                if (itemDataset.get(position).found) {
                    menu.add(Menu.NONE, 0, Menu.NONE, "Remove from 'found'");
                } else if (itemDataset.get(position).deltabusted) {
                    menu.add(Menu.NONE, 0, Menu.NONE, "Remove from 'deltabusted'");
                }
            }
        });

        holder.view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                context.selectItem(position);
                return true;
            }
        });

        holder.item_name.setText(itemDataset.get(position).name);
        holder.item_sku.setText(itemDataset.get(position).sku);
        holder.item_upc.setText(itemDataset.get(position).upc);
        //holder.item_price.setText(itemDataset.get(position).salePrice);
        holder.item_model.setText(itemDataset.get(position).modelNumber);
        holder.item_image.setImageBitmap(itemDataset.get(position).imageBit);
        holder.page_num.setText("Page " + (itemDataset.get(position).pageNum+1));

        if (itemDataset.get(position).inStock && itemDataset.get(position).stores.get(0).lowStock) {
            holder.item_stock.setText("Few in stock");
        } else if (itemDataset.get(position).inStock) {
            holder.item_stock.setText("In stock");
        } else {
            holder.item_stock.setText("Not in stock");
        }

        holder.multiple_plano.setVisibility(itemDataset.get(position).multiple_plano ? View.VISIBLE : View.GONE);
        holder.item_selected.setVisibility(itemDataset.get(position).selected ? View.VISIBLE : View.GONE);

        if (itemDataset.get(position).deltabusted) {
            holder.item_status.setText("|  Deltabusted");
            holder.item_status.setVisibility(View.VISIBLE);
        } else if (itemDataset.get(position).found) {
            holder.item_status.setText("|  Found");
            holder.item_status.setVisibility(View.VISIBLE);
        } else {
            holder.item_status.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return itemDataset.size();
    }
}
