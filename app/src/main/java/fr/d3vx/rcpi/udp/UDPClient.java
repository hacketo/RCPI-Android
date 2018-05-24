package fr.d3vx.rcpi.udp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import fr.d3vx.rcpi.MainActivity;

/**
 * Created by hacketo on 26/07/17.
 */

public class UDPClient {

    private AsyncTask<Void, Void, Void> thread;

    private Config cfg;
    private WeakReference<MainActivity> act;

    public UDPClient(MainActivity activity, Config config){
        cfg = config;
        act = new WeakReference<MainActivity>(activity);
    }

    public void send(final String message){
        thread = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params){
                DatagramSocket ds = null;

                try{
                    ds = new DatagramSocket();
                    ds.setReuseAddress(true);
                    ds.setSoTimeout(1000);
                    DatagramPacket dp;
                    dp = new DatagramPacket(message.getBytes(), message.getBytes().length, cfg.address, cfg.port);
                    ds.setBroadcast(true);
                    ds.send(dp);
                }
                catch (Exception e){
                    e.printStackTrace();
                    act.get().getApplicationContext().sendBroadcast(new Intent(MainActivity.ERROR_ACTION));
                }
                finally{
                    if (ds != null){
                        ds.close();
                    }
                }
                return null;
            }
            protected void onPostExecute(Void result){
                super.onPostExecute(result);
            }
        };

        if (Build.VERSION.SDK_INT >= 11) thread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else thread.execute();
    }

    public void close(){
        thread.cancel(true);
    }
}
