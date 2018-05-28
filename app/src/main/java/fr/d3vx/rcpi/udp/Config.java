package fr.d3vx.rcpi.udp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by hacketo on 26/07/17.
 */

public class Config {

    public int port;
    public String uri;
    public InetAddress address;
    public boolean debug;

    private static Config instance = null;

    private Config(){}

    public static Config getInstance(Context c, boolean force){
        if (instance == null || force){
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);

            String ip = "192.168.0.7";
            int port = 9878;

            //SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            String prefIp = sharedPref.getString("ip", ip);
            String pp = sharedPref.getString("port", ""+port);
            int prefPort = Integer.valueOf(pp);
            boolean prefDebug = sharedPref.getBoolean("debug", false);

            if (instance == null){
                instance = new Config();
            }

            instance.port = prefPort;
            try {
                instance.address = InetAddress.getByName(prefIp);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            instance.debug = prefDebug;
            instance.uri = prefIp+":"+prefPort;
        }
        return instance;
    }
}
