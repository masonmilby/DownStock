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

import java.util.List;

import com.milburn.downstock.ProductDetails.DetailedItem;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private ProductDetails productDetails;
    private List<DetailedItem> detailedList;
    private RecyclerFragment context;

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

        private TextView div_multi;
        private TextView div_status;

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

            div_multi = v.findViewById(R.id.div_multi);
            div_status = v.findViewById(R.id.div_status);
        }
    }

    public RecyclerAdapter(ProductDetails data, RecyclerFragment con) {
        setHasStableIds(true);
        productDetails = data;
        context = con;
        refreshData();
    }

    public void refreshData() {
        detailedList = productDetails.getShownItems();
        notifyDataSetChanged();
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
                context.setSelectedPosition(position);
                if (!context.isSelectionState()) {
                    view.showContextMenu();
                } else {
                    context.selectItem(position);
                }
            }
        });

        holder.view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                MenuInflater inflater = context.getActivityMenuInflater();
                inflater.inflate(R.menu.context_menu, menu);

                menu.setHeaderTitle("Item options");

                if (detailedList.get(position).isFound()) {
                    menu.add(Menu.NONE, 0, Menu.NONE, "Remove from 'found'");
                } else if (detailedList.get(position).isDeltabusted()) {
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

        holder.item_name.setText(detailedList.get(position).getName());
        holder.item_sku.setText(detailedList.get(position).getSku());
        holder.item_upc.setText(detailedList.get(position).getUpc());
        //holder.item_price.setText(detailedList.get(position).getSalePrice());
        holder.item_model.setText(detailedList.get(position).getModelNumber());
        holder.item_image.setImageBitmap(detailedList.get(position).getImageBit());
        holder.page_num.setText("Page " + (detailedList.get(position).getPageNum()+1));

        if (detailedList.get(position).isInStock() && detailedList.get(position).isLowStock()) {
            holder.item_stock.setText("Few in stock");
            holder.item_stock.setTextColor(context.getResources().getColor(R.color.colorTextOkay));
        } else if (detailedList.get(position).isInStock()) {
            holder.item_stock.setText("In stock");
            holder.item_stock.setTextColor(context.getResources().getColor(R.color.colorTextGood));
        } else {
            holder.item_stock.setText("Not in stock");
            holder.item_stock.setTextColor(context.getResources().getColor(R.color.colorTextBad));
        }

        holder.multiple_plano.setVisibility(detailedList.get(position).isMultiPlano() ? View.VISIBLE : View.GONE);
        holder.div_multi.setVisibility(detailedList.get(position).isMultiPlano() ? View.VISIBLE : View.GONE);

        holder.item_selected.setVisibility(detailedList.get(position).isSelected() ? View.VISIBLE : View.GONE);

        if (detailedList.get(position).isDeltabusted()) {
            holder.item_status.setText("Deltabusted");
            holder.item_status.setTextColor(context.getResources().getColor(R.color.colorTextBad));
            holder.item_status.setVisibility(View.VISIBLE);
            holder.div_status.setVisibility(View.VISIBLE);
        } else if (detailedList.get(position).isFound()) {
            holder.item_status.setText("Found");
            holder.item_status.setTextColor(context.getResources().getColor(R.color.colorTextGood));
            holder.item_status.setVisibility(View.VISIBLE);
            holder.div_status.setVisibility(View.VISIBLE);
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
}
