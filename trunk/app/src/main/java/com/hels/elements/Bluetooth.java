package com.hels.elements;

import static com.hels.elements.ShowAlertDialog.showFinalAlertDialog;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.UUID;

public class Bluetooth {

    public static class BLECharacteristic {

        public final static int optionRead = 0x01;
        public final static int optionWrite = 0x02;
        public final static int optionNotification = 0x04;

        private BluetoothGattCharacteristic gattCharacteristic;
        private UUID uuid;
        private String description;
        private int id;
        private int options;

        public BLECharacteristic(BluetoothGattCharacteristic gattCharacteristic,  UUID uuid, String description, int options, int id) {
            this.gattCharacteristic = gattCharacteristic;
            this.uuid = uuid;
            this.description = description;
            this.options = options;
            this.id = id;
        }

        public BluetoothGattCharacteristic getGattCharacteristic() { return this.gattCharacteristic; }
        public void setGattCharacteristic(BluetoothGattCharacteristic gattCharacteristic) { this.gattCharacteristic = gattCharacteristic; }
        public UUID getUUID() { return this.uuid; }
        public String getDescription() { return this.description; }
        public int getOptions() { return this.options; }
        public int getId() { return this.id; }

    }

    final static int PERM_REQ_MULTIPLE = 11;
    static public boolean checkPermissions(Activity activity) {

        ArrayList<String> permissionsToAsk = new ArrayList<>();

        ArrayList<String> necessaryPermissions = new ArrayList<>();
        //necessaryPermissions.add(Manifest.permission.REQUEST_INSTALL_PACKAGES);
        //necessaryPermissions.add(Manifest.permission.READ_PHONE_STATE);
        //necessaryPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //necessaryPermissions.add(Manifest.permission.INTERNET);
        necessaryPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        necessaryPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        necessaryPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        necessaryPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
        necessaryPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        necessaryPermissions.add(Manifest.permission.BLUETOOTH);



        for(int i = 0; i < necessaryPermissions.size(); i++) {
            if(ContextCompat.checkSelfPermission(activity,
                    necessaryPermissions.get(i)) != PackageManager.PERMISSION_GRANTED) {

                permissionsToAsk.add(necessaryPermissions.get(i));
            }
        }

        if(permissionsToAsk.size() > 0) {
            ActivityCompat.requestPermissions(activity,
                    permissionsToAsk.toArray(new String[permissionsToAsk.size()]), PERM_REQ_MULTIPLE);
        }

        if(permissionsToAsk.size() > 0 ) return false;
        else return true;

    }

    public static void processRequest(Activity activity, int requestCode, String permissions[], int[] grantResults) {
        int denied = 0;
        switch(requestCode) {
            case PERM_REQ_MULTIPLE: {
                //Logger.logToFile(getApplicationContext(), "perms.txt", String.format("num: %d", grantResults.length));
                if(grantResults.length > 0) {
                    String permName = "";
                    for(int i = 0; i < grantResults.length; i++) {

                        if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            //Logger.logToFile(getApplicationContext(), "perms.txt", String.format("%s denied", permissions[i]));
                            try {
                                permName += "\r\n" + permissions[i].substring(permissions[i].lastIndexOf(".") + 1).replace("_", " ");
                            } catch(Exception e) {
                            }
                            denied++;
                        }
                        //else Logger.logToFile(getApplicationContext(), "perms.txt", String.format("%s allowed", permissions[i]));
                    }
                    if(denied == 1)
                        showFinalAlertDialog(activity, "Permission denied", String.format("Please allow permission:\r\n%s", permName));
                    else if(denied > 1)
                        showFinalAlertDialog(activity, "Permissions denied", String.format("Please allow  permissions:\r\n%s", permName));
                }
            }
        }
    }
}
