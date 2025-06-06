/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.net.ftp.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.commons.net.io.Util;

/**
 * Sample class to download LIST and MLSD listings from list of ftp sites.
 */
public class DownloadListings extends FTPClient {

    // Also used by MLDSComparison
    static final String DOWNLOAD_DIR = "target/ftptest";

    public static void main(final String[] args) throws Exception {
        String host; // = "ftp.funet.fi";
        final int port = 21;
        String path; // = "/";

        new File(DOWNLOAD_DIR).mkdirs();
        final DownloadListings self = new DownloadListings();
        final OutputStream outputStream = new FileOutputStream(new File(DOWNLOAD_DIR, "session.log"));
        self.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(outputStream), true));

        final Reader reader = new FileReader("mirrors.list");
        final BufferedReader bufReader = new BufferedReader(reader);
        String line;
        while ((line = bufReader.readLine()) != null) {
            if (line.startsWith("ftp")) {
                final String[] parts = line.split("\\s+");
                final String target = parts[2];
                host = target.substring("ftp://".length());
                final int slash = host.indexOf('/');
                path = host.substring(slash);
                host = host.substring(0, slash);
                System.out.println(host + " " + path);
                if (self.open(host, port)) {
                    try {
                        self.info();
                        self.download(path, FTPCmd.LIST, new File(DOWNLOAD_DIR, host + "_list.txt"));
                        self.download(path, FTPCmd.MLSD, new File(DOWNLOAD_DIR, host + "_mlsd.txt"));
                    } catch (final Exception e) {
                        e.printStackTrace();
                    } finally {
                        self.disconnect();
                    }
                    self.removeProtocolCommandListener(self.listener);
                    self.out.close();
                }
            }
        }
        outputStream.close();
        bufReader.close();
    }

    private PrintCommandListener listener;

    private PrintWriter out;

    private void download(final String path, final FTPCmd command, final File fileName) throws Exception {
        final Socket socket;
        if ((socket = _openDataConnection_(command, getListArguments(path))) == null) {
            System.out.println(getReplyString());
            return;
        }
        final InputStream inputStream = socket.getInputStream();
        final OutputStream outputStream = new FileOutputStream(fileName);
        Util.copyStream(inputStream, outputStream);
        inputStream.close();
        socket.close();
        outputStream.close();

        if (!completePendingCommand()) {
            System.out.println(getReplyString());
        }
    }

    private void info() throws IOException {
        syst();
        help();
        feat();
        removeProtocolCommandListener(listener);
    }

    private boolean open(final String host, final int port) throws Exception {
        System.out.println("Connecting to " + host);
        out = new PrintWriter(new FileWriter(new File(DOWNLOAD_DIR, host + "_info.txt")));
        listener = new PrintCommandListener(out);
        addProtocolCommandListener(listener);
        setConnectTimeout(30000);
        try {
            connect(host, port);
        } catch (final Exception e) {
            System.out.println(e);
            return false;
        }
        enterLocalPassiveMode(); // this is reset by connect
        System.out.println("Logging in to " + host);
        return login("anonymous", "user@localhost");
    }
}
