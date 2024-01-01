package com.hels.elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

/**
 * Created by DF on 10/18/2017.
 */

class ShowAlertDialog {

    static CheckBox dontShowAgain;

//    public static void showNewAppVerDialog(final Activity activity, String title, String message) {
//
//        AlertDialog.Builder builder=new AlertDialog.Builder(activity);
//        LayoutInflater adbInflater = LayoutInflater.from(activity);
//        View view = adbInflater.inflate(R.layout.new_version_dlg, null);
//        dontShowAgain = (CheckBox)view.findViewById(R.id.chkb_dont_show);
//        builder.setView(view);
//        builder.setTitle(title);
//        builder.setMessage(message);
//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//
//                if (dontShowAgain.isChecked()) {
//                    AppPreferences.save(activity, AppPreferences.TYPE_MISC, AppPreferences.NAME_DONT_SHOW_NEW_APP, true);
//                }
//                else
//                    AppPreferences.save(activity, AppPreferences.TYPE_MISC, AppPreferences.NAME_DONT_SHOW_NEW_APP, false);
//
//                return;
//            } });
//
//        builder.show();
//
//    }

//    public static void showAlertDialog(final Activity activity, String title, String message) {
//
//        AlertDialog.Builder builder=new AlertDialog.Builder(activity);
//        LayoutInflater adbInflater = LayoutInflater.from(activity);
//        View view = adbInflater.inflate(R.layout.alert_dlg, null);
//        builder.setView(view);
//        builder.setTitle(title);
//        builder.setMessage(message);
//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                return;
//            } });
//
//        builder.show();
//
//    }

    public static void showFinalAlertDialog(final Activity activity, String title, String message) {

        AlertDialog.Builder builder=new AlertDialog.Builder(activity);
        LayoutInflater adbInflater = LayoutInflater.from(activity);
        View view = adbInflater.inflate(R.layout.alert_dlg, null);
        builder.setView(view);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            } });

        builder.show();

    }

    public static void showAlertDialog(final Activity activity, String title, String message) {

        AlertDialog.Builder builder=new AlertDialog.Builder(activity, R.style.AlertDialog);
        //AlertDialog.Builder builder=new AlertDialog.Builder(activity);
        LayoutInflater adbInflater = LayoutInflater.from(activity);
        //AlertDialog alertDialog = builder.create();
        View view = adbInflater.inflate(R.layout.alert_dlg, null);
        builder.setView(view);
        //builder.setTitle(title);
        //builder.setMessage(message);
        builder.setCancelable(false);

//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                activity.finish();
//            } });

        builder.show();


//        Dialog dialog = new Dialog(activity, android.R.style.Theme_Dialog);
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        dialog.setContentView(R.layout.alert_dlg);
//        dialog.setCanceledOnTouchOutside(true);
//
//        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        //dialog.setContentView(layoutResId);
//        View v = dialog.getWindow().getDecorView();
//        v.setBackgroundResource(android.R.color.transparent);
//
//        dialog.setCanceledOnTouchOutside(false);
//        //dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
//        dialog.show();
    }


}
