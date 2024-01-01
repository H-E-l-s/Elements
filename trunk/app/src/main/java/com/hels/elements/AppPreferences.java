package com.hels.elements;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;

/**
 * Created by DF on 9/7/2017.
 */

class AppPreferences {

    public static final String TYPE_MISC               = "misc";
    public static final String TYPE_SERVERS            = "servers";
    public static final String TYPE_VERSIONS           = "versions";
    public static final String TYPE_REGION             = "region";
    public static final String TYPE_MODE               = "mode";


    public static final String NAME_ENABLED = "enabled";

    public static final String NAME_SELECTED_SERVER_NAME = "selected_server_name";

    public static final String NAME_CUSTOM_SERVER_IP   = "custom_server_ip";
    public static final String NAME_CUSTOM_SERVER_PORT = "custom_server_port";
    public static final String NAME_ENG_MODE           = "eng_mode";
    public static final String NAME_SELECTED_REGION    = "selected_region";
    public static final String NAME_LOCAL_SERVER_IP   = "local_server_ip";
    public static final String NAME_LOCAL_SERVER_PORT = "local_server_port";
    public static final String NAME_SELECTED_MODE    = "selected_mode";

    public static final String NAME_DONT_SHOW_NEW_APP  = "new_app";
    public static final String NAME_NEW_APP_VER        = "new_app_ver";

    //public static final String NAME_LAST_FILENAME      = "last_file_name";
    //public static final String NAME_LAST_PATH          = "last_file_path";

    public static final String NAME_SGLE_URI           = "selected_sgle_uri";
    public static final String NAME_SGLE_FILENAME      = "selected_sgle_filename";

    public static final String NAME_SELECTED_TX        = "selected_tx_name";

    public static final Integer VAL_ENG_MODE            = 1;
    public static final Integer VAL_PUB_MODE            = 0;

    public static final int PROG_MODE_PROD          = 0;
    public static final int PROG_MODE_UPD           = 1;
    public static final int PROG_MODE_BT            = 2;

//    <string name="sel_server">selected_server</string>
//    <string name="last_app_ver_code">last_app_ver_code</string>
//    <string name="last_app_ver_name">last_app_ver_name</string>
//    <string name="last_tx_list_ver_code">last_tx_list_ver_code</string>
//    <string name="last_rx_list_ver_code">last_rx_list_ver_code</string>
//    <string name="curr_tx_list_ver_code">curr_tx_list_ver_code</string>
//    <string name="curr_rx_list_ver_code">curr_rx_list_ver_code</string>
//    <string name="list_ver_code">_list_ver_code</string>



    static void save(Activity a, String prefType, String prefName, Integer value ) {

        SharedPreferences sharedPref =  a.getSharedPreferences(prefType, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt( prefName, value.intValue());
        editor.commit();
    }

    static void save(Activity a, String prefType, String prefName, String value ) {

        SharedPreferences sharedPref =  a.getSharedPreferences(prefType, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString( prefName, value);
        editor.commit();
    }

    static void save(Activity a, String prefType, String prefName, Boolean value ) {

        SharedPreferences sharedPref =  a.getSharedPreferences(prefType, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean( prefName, value);
        editor.commit();
    }

    static String readString(Activity a, String prefType, String prefName) {
        String s = null;

        SharedPreferences sharedPref = a.getSharedPreferences(prefType,Context.MODE_PRIVATE);
        if(sharedPref.contains(prefName)) {
            s = sharedPref.getString(prefName, null);
        }

        return s;
    }

    static String readString(Context c, String prefType, String prefName) {
        String s = null;

        SharedPreferences sharedPref = c.getSharedPreferences(prefType,Context.MODE_PRIVATE);
        if(sharedPref.contains(prefName)) {
            s = sharedPref.getString(prefName, null);
        }

        return s;
    }


    static Integer readInteger(Activity a, String prefType, String prefName) {
        Integer i = null;

        SharedPreferences sharedPref = a.getSharedPreferences(prefType,Context.MODE_PRIVATE);
        if(sharedPref.contains(prefName)) {
            i = sharedPref.getInt(prefName, 0);
        }

        return i;
    }

    static Boolean readBoolean(Activity a, String prefType, String prefName) {
        Boolean v = null;

        SharedPreferences sharedPref = a.getSharedPreferences(prefType,Context.MODE_PRIVATE);
        if(sharedPref.contains(prefName)) {
            v = sharedPref.getBoolean(prefName, false);
        }

        return v;
    }

    static Boolean readBoolean(Context c, String prefType, String prefName) {
        Boolean v = null;

        SharedPreferences sharedPref = c.getSharedPreferences(prefType,Context.MODE_PRIVATE);
        if(sharedPref.contains(prefName)) {
            v = sharedPref.getBoolean(prefName, false);
        }

        return v;
    }

    static void remove(Activity a, String prefType, String prefName) {
        Integer i = null;

        SharedPreferences sharedPref =  a.getSharedPreferences(prefType, Context.MODE_PRIVATE);
        if(sharedPref.contains(prefName)) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove(prefName);
            editor.commit();
        }

    }

    static Boolean remove(Context c, String prefType) {
//        return c.deleteDatabase(prefType);
        //SharedPreferences.Editor.
        c.getSharedPreferences(prefType,0).edit().clear().commit();

        File sharedPreferenceFile = new File(c.getFilesDir().getParentFile().getAbsolutePath() + File.separator + "shared_prefs" +  File.separator + prefType + ".xml");
        Log.d("PREFS", String.format("%s : %B", sharedPreferenceFile.getAbsolutePath(), sharedPreferenceFile.exists() ) );
        sharedPreferenceFile.delete();
        //File[] listFiles = sharedPreferenceFile.listFiles();
        //for (File file : listFiles) {
//            file.delete();
  //      }
        return true;
    }
}
