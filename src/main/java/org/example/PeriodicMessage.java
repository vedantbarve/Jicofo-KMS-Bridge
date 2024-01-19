package org.example;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.bosh.BOSHConfiguration;
import org.jivesoftware.smack.bosh.XMPPBOSHConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jxmpp.jid.Jid;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;

public class PeriodicMessage implements Runnable{

    private String userName;
    private String password;
    private String xmppDomain;

    private String host;
    private int port;

    private long sleepDuration;

    private Jid receiver;

    private XMPPBOSHConnection connection;

    public PeriodicMessage(String userName, String password, String xmppDomain, String host, int port, long sleepDuration, Jid receiver) throws XmppStringprepException{
        this.userName = userName;
        this.password = password;
        this.xmppDomain = xmppDomain;
        this.host = host;
        this.port = port;
        this.sleepDuration = sleepDuration;
        this.receiver = receiver;
        createConnection();
    }

    private void createConnection() throws XmppStringprepException {
        try{
        BOSHConfiguration config = BOSHConfiguration.builder()
                .setUsernameAndPassword(this.userName, this.password)
                .setFile("/http-bind")
                .setHost(this.host)
                .setXmppDomain(this.xmppDomain)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setPort(this.port)
                .build();

            this.connection = new XMPPBOSHConnection(config);

            this.connection.connect().login();
            // Add a stanza filter to capture incoming and outgoing messages
            connection.addAsyncStanzaListener(new StanzaListener() {
                @Override
                public void processStanza(Stanza packet) {
                    System.out.println("Incoming message: " + packet.toXML());
                }
            }, new StanzaFilter() {
                @Override
                public boolean accept(Stanza packet) {
                    return true; // Accept all stanzas for logging
                }
            });

        } catch (SmackException | IOException | XMPPException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public void run(){

        while(true){
            try {
                Message message = StanzaBuilder.buildMessage().to(this.receiver).setSubject("Hello").setBody("World").build();
                connection.sendStanza(message);
                Thread.sleep(sleepDuration);
                Thread.yield();
            } catch (InterruptedException | SmackException  e) {
                this.connection.disconnect();
                throw new RuntimeException(e);
            }
        }
    }
}
