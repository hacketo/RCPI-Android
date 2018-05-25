package fr.d3vx.rcpi.udp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.Buffer;

import fr.d3vx.rcpi.MainActivity;

/**
 * Created by hacketo on 26/07/17.
 */

public class UDPServer {

    private boolean enabled;

    private AsyncTask<Void, Void, Void> thread;

    private Config cfg;

    private WeakReference<MainActivity> act;

    public UDPServer(MainActivity activity, Config config){
        cfg = config;
        act = new WeakReference<MainActivity>(activity);
    }

    public void start(){
        enabled = true;

        thread = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params){
                byte[] lMsg = new byte[12096];
                DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
                DatagramSocket ds = null;

                try{
                    ds = new DatagramSocket(cfg.port);
                    while(enabled){
                        ds.receive(dp);
                        Intent i = new Intent();
                        i.setAction(MainActivity.SERVER_MSG_ACTION);
                        i.putExtra(MainActivity.SERVER_MSG_KEY, lMsg);
                        broadcast(i);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                    broadcast(new Intent(MainActivity.ERROR_ACTION));
                }
                finally{
                    if (ds != null){
                        ds.close();
                    }
                }

                return null;
            }
        };

        if (Build.VERSION.SDK_INT >= 11) {
            thread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else {
            thread.execute();
        }
    }

    private void broadcast(Intent i){
        if (act.get() != null) {
            act.get().getApplicationContext().sendBroadcast(i);
        }
    }

    public void close(){
        thread.cancel(true);
    }

}
