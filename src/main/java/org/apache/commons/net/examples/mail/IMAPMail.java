/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.net.examples.mail;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.imap.IMAPClient;

/**
 * This is an example program demonstrating how to use the IMAP[S]Client class. This program connects to a IMAP[S] server, lists its capabilities and shows the
 * status of the Inbox.
 * <p>
 * Usage: IMAPMail imap[s]://user:password@server/
 * <p>
 * For example<br>
 * IMAPMail imaps://user:password@imap.mail.yahoo.com/<br>
 * or<br>
 * IMAPMail imaps://user:password@imap.googlemail.com/
 */
public final class IMAPMail {

    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: IMAPMail imap[s]://user:password@server/");
            System.err.println("Connects to server; lists capabilities and shows Inbox status");
            System.exit(1);
        }

        final URI uri = URI.create(args[0]);

        // Connect and login
        final IMAPClient imap = IMAPUtils.imapLogin(uri, 10000, null);

        // suppress login details
        imap.addProtocolCommandListener(new PrintCommandListener(System.out, true));

        try {
            imap.setSoTimeout(6000);

            imap.capability();

            imap.select("inbox");

            imap.examine("inbox");

            imap.status("inbox", new String[] { "MESSAGES" });

            imap.list("", "*"); // Show the folders

        } catch (final IOException e) {
            System.out.println(imap.getReplyString());
            e.printStackTrace();
            System.exit(10);
        } finally {
            imap.logout();
            imap.disconnect();
        }
    }
}

