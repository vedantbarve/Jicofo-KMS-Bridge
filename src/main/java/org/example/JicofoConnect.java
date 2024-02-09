package org.example;

import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import org.jitsi.xmpp.extensions.jitsimeet.ConferenceIq;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.bosh.BOSHConfiguration;
import org.jivesoftware.smack.bosh.XMPPBOSHConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
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
    private static final String focusJID = "focus@auth.vedant-the-intern.pune.cdac.in";
    private final EntityBareJid roomJID;
    private XMPPBOSHConnection connectionBOSH;


    public JicofoConnect(String domain, String host, int port, String roomJID) throws IOException {
        this.domain = domain;
        this.host = host;
        this.port = port;
        this.roomJID = JidCreate.entityBareFrom(roomJID);
    }

    void start() throws MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, MultiUserChatException.MucNotJoinedException, InterruptedException, MultiUserChatException.NotAMucServiceException {
        connectBOSH();
        inviteFocus();
//        sendRawConferenceIQ1();
//        sendRawConferenceIQ2();
//        initiateServiceDiscovery();
//        joinMUC();
    }

    private void connectBOSH() {
        try {
            BOSHConfiguration config = BOSHConfiguration.builder()
                    .performSaslAnonymousAuthentication()
                    .setHost(this.host)
                    .setXmppDomain(this.domain)
                    .setPort(this.port)
                    .setFile("/http-bind")
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .build();
            this.connectionBOSH = new XMPPBOSHConnection(config);
            this.connectionBOSH.connect().login();
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
            this.connectionBOSH.createStanzaCollectorAndSend(focusInviteIQ);
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }

    void joinConference(){

    }

    private void sendRawConferenceIQ1() {
        try {
            IQ iq = Helper.convertXmlToIQ("<iq id=\"GFH9-4:sendIQ\" to=\"focus.vedant-the-intern.pune.cdac.in\" type=\"set\" xmlns=\"jabber:client\" ><conference machine-uid=\"00:2B:67:45:6B:DC\" xmlns=\"http://jitsi.org/protocol/focus\" room=\"was@conference.vedant-the-intern.pune.cdac.in\"><property name=\"rtcstatsEnabled\" value=\"false\"/><property name=\"visitors-version\" value=\"1\"/></conference></iq>");
            IQ response = this.connectionBOSH.sendIqRequestAndWaitForResponse(iq);
            if (response != null) {
                LOGGER.info(STR."Response : \{response}");
            }
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }

    private void initiateServiceDiscovery() {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(this.connectionBOSH);
        String[] featuresList = {
                "http://jabber.org/protocol/disco#info",
                "http://jabber.org/protocol/disco#items",
                "urn:xmpp:jingle:apps:dtls:0",
                "urn:ietf:rfc:3264",
                "urn:xmpp:jingle:apps:rtp:audio",
                "urn:xmpp:jingle:apps:rtp:video",
                "urn:ietf:rfc:5888",
                "urn:ietf:rfc:5761",
                "urn:ietf:rfc:4588",
                "http://jitsi.org/tcc"
        };

        for (String s : featuresList) {
            sdm.addFeature(s);
        }

    }

//    private void joinMUC() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, MultiUserChatException.MucNotJoinedException, InterruptedException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.NotAMucServiceException {
//        try {
//            if (muc != null) {
//                LOGGER.info("Leaving an MUC we already occupy.");
//                muc.leave();
//            }
//            MultiUserChat muc = MultiUserChatManager.getInstanceFor(this.connectionBOSH).getMultiUserChat(roomJID);
//            MultiUserChat.MucCreateConfigFormHandle response =  muc.createOrJoin(Resourcepart.from("was@conference.vedant-the-intern.pune.cdac.in"));
//            if(response == null){
//                LOGGER.info("Joined MUC");
//            }else{
//                LOGGER.info("MUC Created");
//            }
//        } catch (Exception e) {
//            LOGGER.warning(e.toString());
//        }
//    }


    private void sendRawConferenceIQ2() {
        try {
            IQ iq = Helper.convertXmlToIQ("<iq id=\"GF5H9-4:sendIQ\" to=\"focus@auth.vedant-the-intern.pune.cdac.in\" type=\"set\" xmlns=\"jabber:client\" ><conference machine-uid=\"00:2B:67:45:6B:DC\" xmlns=\"http://jitsi.org/protocol/focus\" room=\"asdf@conference.vedant-the-intern.pune.cdac.in\"><property name=\"rtcstatsEnabled\" value=\"false\"/><property name=\"visitors-version\" value=\"1\"/></conference></iq>");
            IQ response = this.connectionBOSH.sendIqRequestAndWaitForResponse(iq);
            if (response != null) {
                LOGGER.info(STR."Response : \{response}");
            }
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }


}
