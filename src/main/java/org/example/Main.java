package org.example;

import org.jitsi.xmpp.extensions.jingle.JingleIQ;
import org.jitsi.xmpp.extensions.jingle.JingleIQProvider;
import org.jitsi.xmpp.extensions.jitsimeet.ConferenceIqProvider;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;
import org.jivesoftware.smack.provider.ProviderManager;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        // Enables debug logs for smack
        System.setProperty("smack.debugEnabled", "true"); 

        // Runs the function in the argument if there is ant parsing error
        SmackConfiguration.setDefaultParsingExceptionCallback(ExceptionLoggingCallback());

        // Subscribe the ConferenceIQ and JingleIQ providers
        new ConferenceIqProvider();
        ProviderManager.addIQProvider(
                JingleIQ.ELEMENT,
                JingleIQ.NAMESPACE,
                new JingleIQProvider()
        );

        // Creating a new JicofoConnect object
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



