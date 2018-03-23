package com.milburn.downstock;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
        private CheckBox item_selected;

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
                view.showContextMenu();
            }
        });

        holder.view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                menu.setHeaderTitle("Item options");
                menu.add(Menu.NONE, v.getId(), 0, "Open item URL");
                menu.add(Menu.NONE, v.getId(), 1, "View page");

                if (itemDataset.get(position).found) {
                    menu.add(Menu.NONE, v.getId(), 2, "Remove from 'found'");
                } else if (itemDataset.get(position).deltabusted) {
                    menu.add(Menu.NONE, v.getId(), 2, "Remove from 'deltabusted'");
                }
            }
        });

        holder.view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                view.setSelected(!view.isSelected());
                holder.item_selected.setVisibility(holder.view.isSelected() ? View.VISIBLE : View.GONE);
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

        holder.multiple_plano.setVisibility(itemDataset.get(position).multiple_plano ? View.VISIBLE : View.GONE);
        holder.item_selected.setVisibility(holder.view.isSelected() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return itemDataset.size();
    }
}
