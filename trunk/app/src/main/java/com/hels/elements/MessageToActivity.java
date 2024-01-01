package com.hels.elements;

import android.os.Handler;
import android.os.Message;

/**
 * Created by DF on 9/4/2017.
 */

class MessageToActivity {

    static void sendMessageToActivity(Handler activityHandler, int what, Object data) {
        Message m = activityHandler.obtainMessage();
        m.what = what;
        m.obj = data;
        activityHandler.sendMessage(m);
    }

}
