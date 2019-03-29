package com.tech2020.packge.bo;

import android.util.Log;

public class MySthread extends Thread{
    @Override
    public void run() {
        for(int i=0; i<5; i++){
            try {
                if(i>=4) {
                    Log.d("myserviceone", "hello" + i);
                    i=0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
