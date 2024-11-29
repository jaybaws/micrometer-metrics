package org.jaybaws.metrics.bw.util;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class HostUtils {

    public static String getIPAddress() {
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Throwable t) {
            return "127.0.0.1";
        }
    }

}