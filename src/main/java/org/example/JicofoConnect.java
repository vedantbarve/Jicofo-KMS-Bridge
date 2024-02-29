package org.example;

import org.jitsi.xmpp.extensions.jingle.JingleIQ;
import org.jitsi.xmpp.extensions.jitsimeet.ConferenceIq;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

public class JicofoConnect {
    private static final Logger LOGGER = Logger.getLogger(JicofoConnect.class.getName());
    private final String domain;
    private final String host;
    private final int port;
    private final EntityBareJid roomJID;
    private XMPPTCPConnection connectionTCP;
    private static String sid;

    public JicofoConnect(String domain, String host, int port, String roomJID) throws IOException {
        this.domain = domain;
        this.host = host;
        this.port = port;
        this.roomJID = JidCreate.entityBareFrom(roomJID);
    }

    void start() {
        connectTCP();
        inviteFocus();
        joinMUC();
    }

    private void connectTCP() {
        try {
            XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                    .performSaslAnonymousAuthentication()
                    .setHost(this.host)
                    .setXmppDomain(this.domain)
                    .setPort(this.port)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .build();
            this.connectionTCP = new XMPPTCPConnection(config);
            this.connectionTCP.connect();
            ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(this.connectionTCP);
            if (discoManager != null) {
                discoManager.addFeature("http://jabber.org/protocol/disco#info");
                discoManager.addFeature("urn:xmpp:jingle:apps:rtp:video");
                discoManager.addFeature("urn:xmpp:jingle:apps:rtp:audio");
                discoManager.addFeature("urn:xmpp:jingle:transports:ice-udp:1");
                discoManager.addFeature("urn:xmpp:jingle:transports:dtls-sctp:1");
                discoManager.addFeature("urn:ietf:params:xml:ns:xmpp-stanzas");
                discoManager.addFeature("urn:ietf:rfc:5888");
                discoManager.addFeature("urn:ietf:rfc:5761");
                discoManager.addFeature("urn:ietf:rfc:4588");
                discoManager.addFeature("http://jitsi.org/tcc");
            }
            this.connectionTCP.login();
            this.connectionTCP.addStanzaListener(
                    stanza -> {
                        LOGGER.info(stanza.toXML().toString());
                    },
                    stanza -> stanza instanceof JingleIQ
            );

            this.connectionTCP.registerIQRequestHandler(
                    new AbstractIqRequestHandler(JingleIQ.ELEMENT,JingleIQ.NAMESPACE, IQ.Type.set, IQRequestHandler.Mode.sync) {
                        @Override
                        public IQ handleIQRequest(IQ iqRequest) {
                            LOGGER.info("In registerIQRequestHandler :");
                            LOGGER.info(iqRequest.toXML().toString());
                            return null;
                        }
                    }
            );
        } catch (Exception e) {
            LOGGER.warning("Connection unsuccessful");
        }


    }

    private void inviteFocus() {
        try {
            ConferenceIq focusInviteIQ = new ConferenceIq();
            focusInviteIQ.setType(IQ.Type.set);
            focusInviteIQ.setTo(JidCreate.domainBareFrom("focus.vedant-the-intern.pune.cdac.in"));
            focusInviteIQ.setRoom(this.roomJID);
            this.connectionTCP.createStanzaCollectorAndSend(focusInviteIQ);

        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }

    private void joinMUC() {
        try {
            MultiUserChat muc = MultiUserChatManager.getInstanceFor(this.connectionTCP).getMultiUserChat(this.roomJID);
            muc.createOrJoin(Resourcepart.fromOrNull(RandomStringGenerator.generateRandomString()));
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }
}

class RandomStringGenerator {

    public static String generateRandomString() {
        // Define the characters that can be used in the random string
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        // Create a Random object
        Random random = new Random();

        // Create a StringBuilder to build the random string
        StringBuilder randomString = new StringBuilder();

        // Generate 10 random characters and append to the StringBuilder
        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            randomString.append(randomChar);
        }

        // Convert StringBuilder to String and return
        return randomString.toString();
    }
}



