package org.example;
import org.jitsi.xmpp.extensions.jitsimeet.ConferenceIq;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.bosh.BOSHConfiguration;
import org.jivesoftware.smack.bosh.XMPPBOSHConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import java.util.Enumeration;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.logging.Logger;

import static org.jivesoftware.smack.util.PacketParserUtils.parseStanza;

public class JicofoConnect {

    private static final Logger LOGGER = Logger.getLogger(JicofoConnect.class.getName());
    private final String userName;
    private final String password;
    private final String domain;
    private final String host;
    private final int port;
    private static final String focusJID = "focus@auth.vedant-the-intern.pune.cdac.in";
    private final EntityBareJid roomJID;
    private XMPPBOSHConnection connectionBOSH;

    public JicofoConnect(String userName, String password, String domain, String host, int port, EntityBareJid roomJID) throws SmackException, IOException, XMPPException, InterruptedException {
        this.userName = userName;
        this.password = password;
        this.domain = domain;
        this.host = host;
        this.port = port;
        this.roomJID = roomJID;
    }

    void connectBOSH()  {
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

            LOGGER.info(String.valueOf(this.connectionBOSH.isAnonymous()));
        } catch (Exception e){
            LOGGER.warning("Connection unsuccessful");
        }
    }

    void sendRawConferenceIQ1() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmlPullParserException, IOException, SmackParsingException {
        try {
            IQ iq = Helper.convertXmlToIQ("<iq id=\"GF5H9-4\" to=\"focus.vedant-the-intern.pune.cdac.in\" type=\"set\" xmlns=\"jabber:client\" ><conference machine-uid=\"00:2B:67:45:6B:DC\" xmlns=\"http://jitsi.org/protocol/focus\" room=\"asdf@conference.vedant-the-intern.pune.cdac.in\"><property name=\"rtcstatsEnabled\" value=\"false\"/><property name=\"visitors-version\" value=\"1\"/></conference></iq>");
            IQ response = this.connectionBOSH.sendIqRequestAndWaitForResponse(iq);
            if(response != null){
                LOGGER.info(STR."Response : \{response}");
            }
        }catch (Exception e){
            LOGGER.warning(e.toString());
        }
    }

    void sendRawConferenceIQ2() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmlPullParserException, IOException, SmackParsingException {
        try {
            IQ iq = Helper.convertXmlToIQ("<iq id=\"GF5H9-4:sendIQ\" to=\"focus@auth.vedant-the-intern.pune.cdac.in\" type=\"set\" xmlns=\"jabber:client\" ><conference machine-uid=\"00:2B:67:45:6B:DC\" xmlns=\"http://jitsi.org/protocol/focus\" room=\"asdf@conference.vedant-the-intern.pune.cdac.in\"><property name=\"rtcstatsEnabled\" value=\"false\"/><property name=\"visitors-version\" value=\"1\"/></conference></iq>");
            IQ response = this.connectionBOSH.sendIqRequestAndWaitForResponse(iq);
            if(response != null){
                LOGGER.info(STR."Response : \{response}");
            }
        }catch (Exception e){
            LOGGER.warning(e.toString());
        }
    }

    void generateConferenceIQ() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException {
        try {
            ConferenceIq iq = new ConferenceIq();
            iq.setTo(JidCreate.entityBareFrom("focus.vedant-the-intern.pune.cdac.in"));
            iq.setType(IQ.Type.set);

            iq.setMachineUID(Helper.getMacAddress());
            iq.setRoom(this.roomJID);

            iq.addProperty("rtcstatsEnabled","false");
            iq.addProperty("visitors-version","1");
            LOGGER.info(iq.toXML().toString());
            IQ response = this.connectionBOSH.sendIqRequestAndWaitForResponse(iq);
            LOGGER.info(response.toString());
        }catch (Exception e){
            LOGGER.warning(e.toString());
        }
    }




}
