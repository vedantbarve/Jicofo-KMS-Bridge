package org.example;

import org.jitsi.xmpp.extensions.DefaultPacketExtensionProvider;
import org.jitsi.xmpp.extensions.jingle.JingleIQ;
import org.jitsi.xmpp.extensions.jingle.JingleIQProvider;
import org.jitsi.xmpp.extensions.jitsimeet.ConferenceIqProvider;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.jingle.provider.JingleProvider;

import java.io.IOException;


@SuppressWarnings("unchecked")
public class Main {
    public static void main(String[] args) throws IOException, SmackException, XMPPException, InterruptedException, XmlPullParserException, SmackParsingException {
        System.setProperty("smack.debugEnabled", "true");
        ProviderManager.addIQProvider(
                JingleIQ.ELEMENT,
                JingleIQ.NAMESPACE,
                new JingleIQProvider()
        );
        new ConferenceIqProvider();

        JicofoConnect jicofoConnect = new JicofoConnect(
                "vedant-the-intern.pune.cdac.in",
                "vedant-the-intern.pune.cdac.in",
                5222,
                "pop13@conference.vedant-the-intern.pune.cdac.in"
        );
        jicofoConnect.start();
    }
}



