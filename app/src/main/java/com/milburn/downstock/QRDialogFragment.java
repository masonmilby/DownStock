package com.milburn.downstock;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.ImageView;

import net.glxn.qrgen.android.QRCode;

public class QRDialogFragment extends DialogFragment {

    public interface OnSaveListName {
        void saveName(String name);
    }

    private OnSaveListName listener;
    private Manager manager;
    private Bundle bundle;

    private TextInputLayout nameLayout;
    private ImageView qrImage;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        manager = new Manager(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        bundle = getArguments();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        if (bundle.getBoolean("is_new")) {
            builder.setView(R.layout.fragment_qr_name)
                    .setTitle("Set list name")
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.saveName(nameLayout.getEditText().getText().toString());
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        } else {
            builder.setView(R.layout.fragment_qr_image);
        }

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        nameLayout = getDialog().findViewById(R.id.list_name);
        qrImage = getDialog().findViewById(R.id.image_qr);

        ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        if (!bundle.getBoolean("is_new")) {
            Bitmap qrBit = QRCode.from(bundle.getString("list_reference")).withSize(1000, 1000).bitmap();
            qrImage.setImageBitmap(qrBit);
        } else {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            nameLayout.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(final CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(final Editable s) {
                    if (s.toString().length() > 0) {
                        manager.getListExists(s.toString(), new Manager.OnGetListExists() {
                            @Override
                            public void finished(boolean exists, String name) {
                                if (exists && name.equals(nameLayout.getEditText().getText().toString())) {
                                    ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                    nameLayout.setError("List already exists");
                                } else if (!exists && name.equals(nameLayout.getEditText().getText().toString())) {
                                    nameLayout.setError("");
                                    ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                }
                            }
                        });
                    } else {
                        ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                }
            });
        }
    }

    public void setListener(OnSaveListName onSaveListName) {
        listener = onSaveListName;
    }
}
