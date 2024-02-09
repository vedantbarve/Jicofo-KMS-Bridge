package org.example;

import org.jitsi.xmpp.extensions.jitsimeet.ConferenceIqProvider;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.xml.XmlPullParserException;

import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException, SmackException, XMPPException, InterruptedException, XmlPullParserException, SmackParsingException {
        System.setProperty("smack.debugEnabled", "true");
        new ConferenceIqProvider();
        JicofoConnect jicofoConnect = new JicofoConnect(
                "vedant-the-intern.pune.cdac.in",
                "vedant-the-intern.pune.cdac.in",
                80,
                "xyz@conference.vedant-the-intern.pune.cdac.in"
        );
        jicofoConnect.start();
    }
}



