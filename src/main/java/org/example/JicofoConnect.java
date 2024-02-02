package org.example;
import org.jitsi.xmpp.extensions.jitsimeet.ConferenceIq;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.bosh.BOSHConfiguration;
import org.jivesoftware.smack.bosh.XMPPBOSHConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

import java.io.IOException;
import java.util.logging.Logger;

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

    public static IQ convertXmlToIQ(String xml) throws SmackException, InterruptedException, XmlPullParserException, IOException, SmackParsingException {

        return PacketParserUtils.parseStanza(xml);
    }

    void sendRawConferenceIQ() throws SmackException, InterruptedException, XMPPException.XMPPErrorException, XmlPullParserException, IOException, SmackParsingException {
        IQ customCinferenceIQ = convertXmlToIQ(
                "<iq id=\"be1b7e83-d66a-42d7-bd6b-fb4c5e3a5c3c:sendIQ\" to=\"focus.vedant-the-intern.pune.cdac.in\" type=\"set\" xmlns=\"jabber:client\"><conference machine-uid=\"9b19307f1fda988b42d51324d963fee6\" room=\"apk@conference.vedant-the-intern.pune.cdac.in\" xmlns=\"http://jitsi.org/protocol/focus\"><property name=\"rtcstatsEnabled\" value=\"false\"/><property name=\"visitors-version\" value=\"1\"/></conference></iq>"
        );
        this.connectionBOSH.sendIqRequestAndWaitForResponse(customCinferenceIQ);
    }

    void sendConferenceRequest() {
        try{
            ConferenceIq iq = new ConferenceIq();

            iq.setTo(JidCreate.entityBareFrom(focusJID));
            iq.setFrom(JidCreate.entityFullFrom(this.connectionBOSH.getUser().toString()));

            iq.setFocusJid(focusJID);

            iq.setType(IQ.Type.set);
            iq.setRoom(this.roomJID);

            iq.addProperty("authentication","false");

            LOGGER.info(iq.toXML().toString());
            IQ response = this.connectionBOSH.sendIqRequestAndWaitForResponse(iq);
            if(response != null){
                LOGGER.info(response.toString());
            }
        }catch (Exception e){
            LOGGER.warning(e.toString());
        }
    }



}
