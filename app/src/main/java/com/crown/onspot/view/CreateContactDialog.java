package com.crown.onspot.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.crown.onspot.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CreateContactDialog extends DialogFragment {
    public static final String KEY_PHONE_NO = "PHONE_NO";
    public static final String KEY_NAME = "NAME";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_create_contact, null);
        OnContactDialogActionClicked clickedHandler = (OnContactDialogActionClicked) getActivity();

        TextInputLayout nameTIL = root.findViewById(R.id.til_cl_address_line);
        TextInputLayout phoneNoTIL = root.findViewById(R.id.til_cl_how_to_reach);

        TextInputEditText nameTIET = root.findViewById(R.id.tiet_cl_address_line);
        TextInputEditText phoneTIET = root.findViewById(R.id.tiet_cl_how_to_reach);

        Bundle argument = getArguments();
        if (argument != null) {
            nameTIET.setText(argument.getString(KEY_NAME));
            phoneTIET.setText(argument.getString(KEY_PHONE_NO).replace("+91", ""));
        }

        builder.setView(root)
                .setPositiveButton(R.string.action_ok, (dialog, id) -> {
                    String name = nameTIL.getEditText().getText().toString().trim();
                    String phone = phoneNoTIL.getEditText().getText().toString().trim();

                    clickedHandler.onContactDialogPositiveActionClicked(name, phone);
                })
                .setNegativeButton(R.string.action_cancel, (dialog, id) -> CreateContactDialog.this.getDialog().cancel());
        return builder.create();
    }

    public interface OnContactDialogActionClicked {
        void onContactDialogPositiveActionClicked(String name, String phoneNo);
    }
}