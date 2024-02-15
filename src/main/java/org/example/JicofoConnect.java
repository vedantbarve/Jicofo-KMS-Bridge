package org.example;

import org.jitsi.xmpp.extensions.jitsimeet.ConferenceIq;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;
import java.util.logging.Logger;

public class JicofoConnect {
    private static final Logger LOGGER = Logger.getLogger(JicofoConnect.class.getName());
    private final String domain;
    private final String host;
    private final int port;
    private final EntityBareJid roomJID;
    private XMPPTCPConnection connectionTCP;

    public JicofoConnect(String domain, String host, int port, String roomJID) throws IOException {
        this.domain = domain;
        this.host = host;
        this.port = port;
        this.roomJID = JidCreate.entityBareFrom(roomJID);
    }

    void start() {
        connectBOSH();
        inviteFocus();
        joinMUC();
    }

    private void connectBOSH() {
        try {
            XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                    .performSaslAnonymousAuthentication()
                    .setHost(this.host)
                    .setXmppDomain(this.domain)
                    .setPort(this.port)
//
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
                discoManager.addFeature("urn:ietf:rfc:5888");
                discoManager.addFeature("urn:ietf:rfc:5761");
                discoManager.addFeature("urn:ietf:rfc:4588");
                discoManager.addFeature("http://jitsi.org/tcc");

            }
            this.connectionTCP.login();
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
            MultiUserChat.MucCreateConfigFormHandle response = muc.createOrJoin(Resourcepart.fromOrNull("Jicofo-KMS-Bridge"));
            if (response == null) {
                LOGGER.info("Joining meet");
            } else {
                LOGGER.info("Creating meet");
            }

        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }

}

