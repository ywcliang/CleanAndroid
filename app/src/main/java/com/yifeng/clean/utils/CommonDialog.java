package com.yifeng.clean.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import static com.yifeng.clean.GameHelper.s_GameActivity;


/**
 * Created by ywc on 2017/5/8.
 */

public class CommonDialog extends DialogFragment {

    public static void popCommonDialog(String title, String message){

        if (s_GameActivity != null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(s_GameActivity);

            builder.setMessage(message)
                    .setTitle(title);

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}
