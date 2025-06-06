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

package org.apache.commons.net.examples.nntp;

import java.io.IOException;

import org.apache.commons.net.nntp.NNTPClient;
import org.apache.commons.net.nntp.NewsgroupInfo;

/**
 * This is a trivial example using the NNTP package to approximate the Unix newsgroups command. It merely connects to the specified news server and issues
 * fetches the list of newsgroups stored by the server. On servers that store a lot of newsgroups, this command can take a very long time (listing upwards of
 * 30,000 groups).
 */

public final class ListNewsgroups {

    public static void main(final String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: newsgroups newsserver [pattern]");
            return;
        }

        final NNTPClient client = new NNTPClient();
        final String pattern = args.length >= 2 ? args[1] : "";

        try {
            client.connect(args[0]);

            int j = 0;
            try {
                for (final String s : client.iterateNewsgroupListing(pattern)) {
                    j++;
                    System.out.println(s);
                }
            } catch (final IOException e1) {
                e1.printStackTrace();
            }
            System.out.println(j);

            j = 0;
            for (final NewsgroupInfo n : client.iterateNewsgroups(pattern)) {
                j++;
                System.out.println(n.getNewsgroup());
            }
            System.out.println(j);
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
            } catch (final IOException e) {
                System.err.println("Error disconnecting from server.");
                e.printStackTrace();
                System.exit(1);
            }
        }

    }

}
