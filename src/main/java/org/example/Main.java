package org.example;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class Main {
    public static void main(String[] args) throws XmppStringprepException {
        Jid user1 = JidCreate.entityBareFrom("user1@chat.example.com");
        Jid user2 = JidCreate.entityBareFrom("user2@chat.example.com");
        PeriodicMessage message1 = new PeriodicMessage("user1", "apollo11", "chat.example.com", "vedant-the-intern.pune.cdac.in", 5280, 2000, user2);
        PeriodicMessage message2 = new PeriodicMessage("user2", "planet2806@", "chat.example.com", "vedant-the-intern.pune.cdac.in", 5280, 2000, user1);

        Thread t1 = new Thread(message1);
        Thread t2 = new Thread(message2);

        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}