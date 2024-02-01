package org.example;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.bosh.BOSHConfiguration;
import org.jivesoftware.smack.bosh.XMPPBOSHConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.Jid;
import org.jxmpp.stringprep.XmppStringprepException;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

public class JicofoConnect {

    private static final Logger LOGGER = Logger.getLogger(JicofoConnect.class.getName());
    private final String userName;
    private final String password;
    private final String domain;
    private final String host;
    private final int port;
    private Jid jid;
    private XMPPBOSHConnection connection;

    public JicofoConnect(String userName, String password, String domain, String host, int port, Jid jid) throws SmackException, IOException, XMPPException, InterruptedException {
        this.userName = userName;
        this.password = password;
        this.domain = domain;
        this.host = host;
        this.port = port;
        this.jid = jid;
        connect();
    }

    void connect()  {
        try {
            LOGGER.info("Reached Here - 0 ");
//            XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
//                    .setUsernameAndPassword(this.userName,this.password)
//                    .setHost(this.host)
//                    .setXmppDomain(this.domain)
//                    .setPort(this.port)
//                    .build();
            BOSHConfiguration config = BOSHConfiguration.builder()
//                    .setUsernameAndPassword(this.userName,this.password)
                    .setHost(this.host)
                    .setXmppDomain(this.domain)
                    .setPort(this.port)
                    .setFile("/http-bind")
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .performSaslAnonymousAuthentication()
                    .build();
            LOGGER.info("Reached Here - 1 ");
            this.connection = new XMPPBOSHConnection(config);
            LOGGER.info("Reached Here - 2 ");
            this.connection.connect();
            LOGGER.info("Reached Here - 3 ");
            this.connection.login();
            LOGGER.info("Reached Here - 4 ");

            LOGGER.info("Connection successful");
        } catch (Exception e){
            LOGGER.warning("Connection unsuccessful");
        }
    }

    private void sendConferenceRequest(String roomJid) {
        IQ response;
    }


}
