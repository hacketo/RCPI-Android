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
        send(key, null, -1);
    }

    /**
     * Pack and send message to the server
     * @param key
     * @param data
     */
    public void send(int key, String data){
        send(key, data, -1);
    }

    /**
     * Pack and send message to the server
     * @param key
     * @param data
     */
    public void send(int key, String data, int code){
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
            if (code != -1){
                packer.packInt(code);
            }
            send(packer.toByteArray());
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Send message to the server
     * @param message
     */
    private void send(final byte[] message){
        thread = new SendTask(this, message);

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

    static class SendTask extends AsyncTask<Void, Void, Void>{
        private byte[] message;
        private UDPClient client;
        public SendTask(UDPClient client, byte[] message){
            this.message = message;
            this.client = client;
        }

        @Override
        protected Void doInBackground(Void... params){
            DatagramSocket ds = null;

            try{
                ds = new DatagramSocket();
                ds.setReuseAddress(true);
                ds.setSoTimeout(1000);
                DatagramPacket dp;
                dp = new DatagramPacket(message, message.length, client.cfg.address, client.cfg.port);
                ds.setBroadcast(true);
                ds.send(dp);
            }
            catch (Exception e){
                e.printStackTrace();
                client.broadcast(new Intent(MainActivity.ERROR_ACTION));
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
    }
}
