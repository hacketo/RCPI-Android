package fr.d3vx.rcpi.udp;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by hacketo on 26/07/17.
 */

public class Config {

    public int port;
    public InetAddress address;

    private static Config instance = null;

    private Config(){}

    public static Config getInstance(int port, String address){
        if (instance == null){
            instance = new Config();
            instance.port = port;
            try {
                instance.address = InetAddress.getByName(address);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }
}
