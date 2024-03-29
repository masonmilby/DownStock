package com.milburn.downstock;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;

public class AddProductFragment extends DialogFragment {

    public interface OnSubmitItem {
        void submit(ProductDetails.DetailedItem detailedItem);
    }

    private OnSubmitItem listener;
    private BBYApi bbyApi;
    private TextInputLayout idLayout;
    private ProductDetails productDetails = new ProductDetails();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(R.layout.fragment_custom_id)
                .setTitle("Add item")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) {
                            listener.submit(productDetails.getDetailedItems().get(0));
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        idLayout = getDialog().findViewById(R.id.custom_product_id);

        ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        idLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                productDetails = new ProductDetails();
                String prodId = s.toString();
                if (prodId.length() == 7 || prodId.length() == 12) {
                    if (prodId.length() == 7) {
                        productDetails.addBasicItem(new ProductDetails.BasicItem(prodId, "", "-1"));
                    } else {
                        productDetails.addBasicItem(new ProductDetails.BasicItem("", prodId, "-1"));
                    }

                    bbyApi = new BBYApi(getContext(), new BBYApi.AsyncResponse() {
                        @Override
                        public void processFinish(ProductDetails result) {
                            if (getDialog() != null) {
                                if (result.sizeDetailedItems() == 1) {
                                    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                    idLayout.setError("");
                                } else {
                                    idLayout.setError("Product does not exist");
                                    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                }
                            }
                        }
                    });
                    bbyApi.execute(productDetails);

                } else if (getDialog() != null) {
                    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });
    }

    public void setListener(OnSubmitItem onSubmitItem) {
        listener = onSubmitItem;
    }
}
