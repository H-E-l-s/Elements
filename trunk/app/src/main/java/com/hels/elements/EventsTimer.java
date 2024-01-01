package com.hels.elements;

public class EventsTimer {

  //  private CountDownTimer timer = null;
    //private boolean ready = false;
    private long endTime = 0;

    void start(long ms) {
       // ready = false;
        endTime = System.currentTimeMillis() + ms;
//        timer = new CountDownTimer(ms, ms) {
//            public void onTick(long millisUntilFinished) {
//            }
//            public void onFinish() {
//                ready = true;
//            }
//        };
//        timer.start();
    }

    public boolean isReady() {
        if( System.currentTimeMillis() >= endTime ) return true;

        return false;
    }
}
