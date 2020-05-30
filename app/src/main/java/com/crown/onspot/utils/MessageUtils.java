package com.crown.onspot.utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class MessageUtils {
    public static void showShortToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

    }

    public static void showNoActionShortSnackBar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    public static void showActionIndefiniteSnackBar(View view, String message, String actionMsg, int requestCode, OnSnackBarActionListener onSnackbarActionListener) {
        Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).setAction(actionMsg, view1 -> onSnackbarActionListener.onSnackBarActionClicked(view1, requestCode)).show();
    }

    public void showLongToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public interface OnSnackBarActionListener {
        void onSnackBarActionClicked(View view, int requestCode);
    }
}
