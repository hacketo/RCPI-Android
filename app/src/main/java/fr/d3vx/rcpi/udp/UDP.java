package fr.d3vx.rcpi.udp;

import android.util.Log;

import fr.d3vx.rcpi.MainActivity;

/**
 * Created by hacketo on 27/05/18.
 */

public class UDP {

    private UDPServer server;
    private UDPClient client;

    private static UDP instance = null;

    private UDP(MainActivity act, Config config){
        server = new UDPServer(act, config);
        client = new UDPClient(act, config);
    }

    public void send(int key){
        client.send(key);
    }
    public void send(int key, String data){client.send(key, data);}
    public void send(final byte[] message){client.send(message); }

    public void start(){
        server.start();
    }

    void cl(){
        server.close();
        client.close();
    }

    public static UDP getInstance(MainActivity act, Config config){
        if (instance == null){
            instance = new UDP(act, config);
        }
        return instance;
    }

    public static void close(){
        if (instance != null){
            instance.cl();
            Log.d("UDP","closing UDP service");
        }
    }



}
