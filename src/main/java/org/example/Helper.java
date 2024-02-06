package org.example;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Helper {
    public static IQ convertXmlToIQ(String xml) throws XmlPullParserException, IOException, SmackParsingException {
        return PacketParserUtils.parseStanza(xml);
    }


    public static String getMacAddress() {
        try {
            // Get all network interfaces
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Check if the network interface is not a loopback or virtual interface
                if (!networkInterface.isLoopback() && !networkInterface.isVirtual()) {
                    // Get the hardware address (MAC address) of the network interface
                    byte[] macAddressBytes = networkInterface.getHardwareAddress();

                    if (macAddressBytes != null) {
                        // Convert the byte array to a readable format
                        StringBuilder macAddress = new StringBuilder();
                        for (byte b : macAddressBytes) {
                            macAddress.append(String.format("%02X:", b));
                        }

                        // Remove the trailing colon
                        macAddress.deleteCharAt(macAddress.length() - 1);

                        return macAddress.toString();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // Return a default value or handle the case when MAC address is not found
        return "Unknown MAC Address";
    }
}
