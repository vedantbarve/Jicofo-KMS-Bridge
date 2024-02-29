package org.example;


import org.jitsi.xmpp.extensions.colibri2.IqProviderUtils;
import org.jitsi.xmpp.extensions.jingle.JingleIQ;
import org.example.JingleIQProvider;
import org.jitsi.xmpp.extensions.jitsimeet.ConferenceIqProvider;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;
import org.jivesoftware.smack.provider.ProviderManager;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
//        System.setProperty("smack.debugEnabled", "true");
        IqProviderUtils.registerProviders();
        SmackConfiguration.setStanzaCollectorSize(2500000);
//        SmackConfiguration.setDefaultParsingExceptionCallback(ExceptionLoggingCallback());
        new ConferenceIqProvider();
        ProviderManager.addIQProvider(
                JingleIQ.ELEMENT,
                JingleIQ.NAMESPACE,
                new JingleIQProvider()
        );

        JicofoConnect jicofoConnect = new JicofoConnect(
                "vedant-the-intern.pune.cdac.in",
                "vedant-the-intern.pune.cdac.in",
                5222,
                "pop@conference.vedant-the-intern.pune.cdac.in"
        );
        jicofoConnect.start();

        while(true){
            Thread.yield();
        }
    }

    private static ParsingExceptionCallback ExceptionLoggingCallback() {
        return null;
    }

}



