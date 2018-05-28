package fr.d3vx.rcpi.udp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

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

    public void send(int key){
        send(key, null);
    }

    public void send(int key, String data){
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

        if (cfg.debug){
            Log.d("UDPClient", "Sending key:"+key+" ; "+data);
        }

        try {
            packer.packArrayHeader(data != null ? 3 : 2);
            packer.packInt(4);
            packer.packInt(key);
            if (data != null){
                packer.packString(data);
            }
            send(packer.toByteArray());
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private void send(final byte[] message){
        thread = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params){
                DatagramSocket ds = null;

                try{
                    ds = new DatagramSocket();
                    ds.setReuseAddress(true);
                    ds.setSoTimeout(1000);
                    DatagramPacket dp;
                    dp = new DatagramPacket(message, message.length, cfg.address, cfg.port);
                    ds.setBroadcast(true);
                    ds.send(dp);
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
            protected void onPostExecute(Void result){
                super.onPostExecute(result);
            }
        };

        if (Build.VERSION.SDK_INT >= 11) {
            thread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else{
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
