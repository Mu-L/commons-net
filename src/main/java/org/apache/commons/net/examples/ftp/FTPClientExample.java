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

package org.apache.commons.net.examples.ftp;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;

import org.apache.commons.net.examples.PrintCommandListeners;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.util.TrustManagerUtils;

/**
 * This is an example program demonstrating how to use the FTPClient class. This program connects to an FTP server and retrieves the specified file. If the -s
 * flag is used, it stores the local file at the FTP server. Just so you can see what's happening, all reply strings are printed. If the -b flag is used, a
 * binary transfer is assumed (default is ASCII). See below for further options.
 */
public final class FTPClientExample {

    /**
     * Command line usage.
     */
    // @formatter:off
    public static final String USAGE = "Expected Parameters: [options] <hostname> <user> <password> [<remote file> [<local file>]]\n"
            + "\nDefault behavior is to download a file and use ASCII transfer mode.\n"
            + "\t-a - use local active mode (default is local passive)\n"
            + "\t-A - anonymous login (omit user and password parameters)\n"
            + "\t-b - use binary transfer mode\n"
            + "\t-c cmd - issue arbitrary command (remote is used as a parameter if provided) \n"
            + "\t-d - list directory details using MLSD (remote is used as the path if provided)\n"
            + "\t-e - use EPSV with IPv4 (default false)\n"
            + "\t-E - encoding to use for control channel\n"
            + "\t-f - issue FEAT command (remote and local files are ignored)\n"
            + "\t-h - list hidden files (applies to -l and -n only)\n"
            + "\t-i - issue SIZE command for a file\n"
            + "\t-k secs - use keep-alive timer (setControlKeepAliveTimeout)\n"
            + "\t-l - list files using LIST (remote is used as the path if provided)\n"
            + "\t     Files are listed twice: first in raw mode, then as the formatted parsed data.\n"
            + "\t     N.B. if the wrong server-type is used, output may be lost. Use -U or -S as necessary.\n"
            + "\t-L - use lenient future dates (server dates may be up to 1 day into future)\n"
            + "\t-m - list file details using MDTM (remote is used as the path if provided)\n"
            + "\t-n - list file names using NLST (remote is used as the path if provided)\n"
            + "\t--OPTS \"opts-cmd command-name command-options\" - Sends the OPTS\n"
            + "\t-p true|false|protocol[,true|false] - use FTPSClient with the specified protocol and/or isImplicit setting\n"
            + "\t-s - store file on server (upload)\n"
            + "\t-S - systemType set server system type (e.g. Unix VMS WINDOWS)\n"
            + "\t-t - list file details using MLST (remote is used as the path if provided)\n"
            + "\t-U - save unparseable responses\n"
            + "\t-w msec - wait time for keep-alive reply (setControlKeepAliveReplyTimeout)\n"
            + "\t-T  all|valid|none - use one of the built-in TrustManager implementations (none = JVM default)\n"
            + "\t-y format - set default date format string\n"
            + "\t-Y format - set recent date format string\n"
            + "\t-Z timezone - set the server time zone for parsing LIST responses\n"
            + "\t-z timezone - set the time zone for displaying MDTM, LIST, MLSD, MLST responses\n"
            + "\t-PrH server[:port] - HTTP Proxy host and optional port[80] \n"
            + "\t-PrU user - HTTP Proxy server user\n"
            + "\t-PrP password - HTTP Proxy server password\n"
            + "\t-# - add hash display during transfers\n";
    // @formatter:on

    private static void configure(final FTPClient ftp, final FTPClientConfig config, final String defaultDateFormat, final String recentDateFormat,
            final boolean saveUnparseable) {
        config.setUnparseableEntries(saveUnparseable);
        if (defaultDateFormat != null) {
            config.setDefaultDateFormatStr(defaultDateFormat);
        }
        if (recentDateFormat != null) {
            config.setRecentDateFormatStr(recentDateFormat);
        }
        ftp.configure(config);
    }

    private static CopyStreamListener createListener() {
        return new CopyStreamListener() {
            private long megsTotal;

            @Override
            public void bytesTransferred(final CopyStreamEvent event) {
                bytesTransferred(event.getTotalBytesTransferred(), event.getBytesTransferred(), event.getStreamSize());
            }

            @Override
            public void bytesTransferred(final long totalBytesTransferred, final int bytesTransferred, final long streamSize) {
                final long megs = totalBytesTransferred / 1000000;
                for (long l = megsTotal; l < megs; l++) {
                    System.err.print("#");
                }
                megsTotal = megs;
            }
        };
    }

    public static void main(final String[] args) throws UnknownHostException {
        boolean storeFile = false;
        boolean binaryTransfer = false;
        boolean error = false;
        boolean listFiles = false;
        boolean listNames = false;
        boolean hidden = false;
        boolean localActive = false;
        boolean useEpsvWithIPv4 = false;
        boolean feat = false;
        boolean printHash = false;
        boolean mlst = false;
        boolean mlsd = false;
        boolean mdtm = false;
        boolean saveUnparseable = false;
        boolean size = false;
        boolean lenient = false;
        long keepAliveTimeoutSeconds = -1;
        int controlKeepAliveReplyTimeoutMillis = -1;
        int minParams = 5; // listings require 3 params
        String protocol = null; // SSL protocol
        String doCommand = null;
        String trustManager = null;
        String proxyHost = null;
        int proxyPort = 80;
        String proxyUser = null;
        String proxyPassword = null;
        String user = null;
        String password = null;
        Charset encoding = null;
        String serverTimeZoneId = null;
        String displayTimeZoneId = null;
        String serverType = null;
        String defaultDateFormat = null;
        String recentDateFormat = null;
        String opts = null;

        int base = 0;
        for (base = 0; base < args.length; base++) {
            if (args[base].equals("-s")) {
                storeFile = true;
            } else if (args[base].equals("-a")) {
                localActive = true;
            } else if (args[base].equals("-A")) {
                user = "anonymous";
                password = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName();
            } else if (args[base].equals("-b")) {
                binaryTransfer = true;
            } else if (args[base].equals("-c")) {
                doCommand = args[++base];
                minParams = 3;
            } else if (args[base].equals("-d")) {
                mlsd = true;
                minParams = 3;
            } else if (args[base].equals("-e")) {
                useEpsvWithIPv4 = true;
            } else if (args[base].equals("-E")) {
                encoding = Charset.forName(args[++base]);
            } else if (args[base].equals("-f")) {
                feat = true;
                minParams = 3;
            } else if (args[base].equals("-h")) {
                hidden = true;
            } else if (args[base].equals("-i")) {
                size = true;
                minParams = 3;
            } else if (args[base].equals("-k")) {
                keepAliveTimeoutSeconds = Long.parseLong(args[++base]);
            } else if (args[base].equals("-l")) {
                listFiles = true;
                minParams = 3;
            } else if (args[base].equals("-m")) {
                mdtm = true;
                minParams = 3;
            } else if (args[base].equals("-L")) {
                lenient = true;
            } else if (args[base].equals("--OPTS")) {
                opts = args[++base];
            } else if (args[base].equals("-n")) {
                listNames = true;
                minParams = 3;
            } else if (args[base].equals("-p")) {
                protocol = args[++base];
            } else if (args[base].equals("-S")) {
                serverType = args[++base];
            } else if (args[base].equals("-t")) {
                mlst = true;
                minParams = 3;
            } else if (args[base].equals("-U")) {
                saveUnparseable = true;
            } else if (args[base].equals("-w")) {
                controlKeepAliveReplyTimeoutMillis = Integer.parseInt(args[++base]);
            } else if (args[base].equals("-T")) {
                trustManager = args[++base];
            } else if (args[base].equals("-y")) {
                defaultDateFormat = args[++base];
            } else if (args[base].equals("-Y")) {
                recentDateFormat = args[++base];
            } else if (args[base].equals("-Z")) {
                serverTimeZoneId = args[++base];
            } else if (args[base].equals("-z")) {
                displayTimeZoneId = args[++base];
            } else if (args[base].equals("-PrH")) {
                proxyHost = args[++base];
                final String[] parts = proxyHost.split(":");
                if (parts.length == 2) {
                    proxyHost = parts[0];
                    proxyPort = Integer.parseInt(parts[1]);
                }
            } else if (args[base].equals("-PrU")) {
                proxyUser = args[++base];
            } else if (args[base].equals("-PrP")) {
                proxyPassword = args[++base];
            } else if (args[base].equals("-#")) {
                printHash = true;
            } else {
                break;
            }
        }

        final int remain = args.length - base;
        if (user != null) {
            minParams -= 2;
        }
        if (remain < minParams) {
            // server, user, pass, remote, local [protocol]
            if (args.length > 0) {
                System.err.println("Actual Parameters: " + Arrays.toString(args));
            }
            System.err.println(USAGE);
            System.exit(1);
        }

        String server = args[base++];
        int port = 0;
        final String[] parts = server.split(":");
        if (parts.length == 2) {
            server = parts[0];
            port = Integer.parseInt(parts[1]);
        }
        if (user == null) {
            user = args[base++];
            password = args[base++];
        }

        String remote = null;
        if (args.length - base > 0) {
            remote = args[base++];
        }

        String local = null;
        if (args.length - base > 0) {
            local = args[base++];
        }

        final FTPClient ftp;
        if (protocol == null) {
            if (proxyHost != null) {
                System.out.println("Using HTTP proxy server: " + proxyHost);
                ftp = new FTPHTTPClient(proxyHost, proxyPort, proxyUser, proxyPassword);
            } else {
                ftp = new FTPClient();
            }
        } else {
            final FTPSClient ftps;
            if (protocol.equals("true")) {
                ftps = new FTPSClient(true);
            } else if (protocol.equals("false")) {
                ftps = new FTPSClient(false);
            } else {
                final String[] prot = protocol.split(",");
                if (prot.length == 1) { // Just protocol
                    ftps = new FTPSClient(protocol);
                } else { // protocol,true|false
                    ftps = new FTPSClient(prot[0], Boolean.parseBoolean(prot[1]));
                }
            }
            ftp = ftps;
            if (trustManager != null) {
                switch (trustManager) {
                case "all":
                    ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
                    break;
                case "valid":
                    ftps.setTrustManager(TrustManagerUtils.getValidateServerCertificateTrustManager());
                    break;
                case "none":
                    ftps.setTrustManager(null);
                    break;
                default:
                    break;
                }
            }
        }

        if (printHash) {
            ftp.setCopyStreamListener(createListener());
        }
        if (keepAliveTimeoutSeconds >= 0) {
            ftp.setControlKeepAliveTimeout(Duration.ofSeconds(keepAliveTimeoutSeconds));
        }
        if (controlKeepAliveReplyTimeoutMillis >= 0) {
            ftp.setControlKeepAliveReplyTimeout(Duration.ofMillis(controlKeepAliveReplyTimeoutMillis));
        }
        if (encoding != null) {
            ftp.setControlEncoding(encoding);
        }
        ftp.setListHiddenFiles(hidden);

        // suppress login details
        ftp.addProtocolCommandListener(PrintCommandListeners.sysOutPrintCommandListener());

        final FTPClientConfig config;
        if (serverType != null) {
            config = new FTPClientConfig(serverType);
        } else {
            config = new FTPClientConfig();
        }
        configure(ftp, config, defaultDateFormat, recentDateFormat, saveUnparseable);

        try {
            final int reply;
            if (port > 0) {
                ftp.connect(server, port);
            } else {
                ftp.connect(server);
            }
            System.out.println("Connected to " + server + " on " + (port > 0 ? port : ftp.getDefaultPort()));

            // After connection attempt, you should check the reply code to verify
            // success.
            reply = ftp.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                System.exit(1);
            }
        } catch (final IOException e) {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (final IOException f) {
                    // do nothing
                }
            }
            System.err.println("Could not connect to server.");
            e.printStackTrace();
            System.exit(1);
        }

        __main: try {
            if (!ftp.login(user, password)) {
                ftp.logout();
                error = true;
                break __main;
            }

            System.out.println("Remote system is " + ftp.getSystemType());
            if (!ftp.getSystemType().contains(config.getServerSystemKey())) {
                configure(ftp, new FTPClientConfig(ftp.getSystemType()), defaultDateFormat, recentDateFormat, saveUnparseable);
            }
            if (opts != null) {
                ftp.opts(opts);
            }

            if (binaryTransfer) {
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
            } else {
                // in theory this should not be necessary as servers should default to ASCII,
                // but they don't all do so - see NET-500
                ftp.setFileType(FTP.ASCII_FILE_TYPE);
            }

            // Use passive mode as default because most of us are
            // behind firewalls these days.
            if (localActive) {
                ftp.enterLocalActiveMode();
            } else {
                ftp.enterLocalPassiveMode();
            }

            ftp.setUseEPSVwithIPv4(useEpsvWithIPv4);

            if (storeFile) {
                try (InputStream input = new FileInputStream(local)) {
                    ftp.storeFile(remote, input);
                }

                if (keepAliveTimeoutSeconds > 0) {
                    showCslStats(ftp);
                }
            } else if (listFiles || mlsd || mdtm || mlst || listNames || size) {
                // Allow multiple list types for single invocation
                if (mlsd) {
                    for (final FTPFile f : ftp.mlistDir(remote)) {
                        System.out.println(f.getRawListing());
                        System.out.println(f.toFormattedString(displayTimeZoneId));
                    }
                }
                if (mdtm) {
                    final FTPFile f = ftp.mdtmFile(remote);
                    if (f != null) {
                        System.out.println(f.getRawListing());
                        System.out.println(f.toFormattedString(displayTimeZoneId));
                    } else {
                        System.out.println("File not found");
                    }
                }
                if (mlst) {
                    final FTPFile f = ftp.mlistFile(remote);
                    if (f != null) {
                        System.out.println(f.toFormattedString(displayTimeZoneId));
                    }
                }
                if (listNames) {
                    for (final String s : ftp.listNames(remote)) {
                        System.out.println(s);
                    }
                }
                if (size) {
                    System.out.println("Size=" + ftp.getSize(remote));
                }
                // Do this last because it changes the client
                if (listFiles) {
                    if (lenient || serverTimeZoneId != null) {
                        config.setLenientFutureDates(lenient);
                        if (serverTimeZoneId != null) {
                            config.setServerTimeZoneId(serverTimeZoneId);
                        }
                        ftp.configure(config);
                    }

                    for (final FTPFile f : ftp.listFiles(remote)) {
                        System.out.println(f.getRawListing());
                        System.out.println(f.toFormattedString(displayTimeZoneId));
                    }
                }
            } else if (feat) {
                // boolean feature check
                if (remote != null) { // See if the command is present
                    if (ftp.hasFeature(remote)) {
                        System.out.println("Has feature: " + remote);
                    } else if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                        System.out.println("FEAT " + remote + " was not detected");
                    } else {
                        System.out.println("Command failed: " + ftp.getReplyString());
                    }

                    // Strings feature check
                    final String[] features = ftp.featureValues(remote);
                    if (features != null) {
                        for (final String f : features) {
                            System.out.println("FEAT " + remote + "=" + f + ".");
                        }
                    } else if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                        System.out.println("FEAT " + remote + " is not present");
                    } else {
                        System.out.println("Command failed: " + ftp.getReplyString());
                    }
                } else if (ftp.features()) {
//                        Command listener has already printed the output
                } else {
                    System.out.println("Failed: " + ftp.getReplyString());
                }
            } else if (doCommand != null) {
                if (ftp.doCommand(doCommand, remote)) {
//                  Command listener has already printed the output
//                    for(String s : ftp.getReplyStrings()) {
//                        System.out.println(s);
//                    }
                } else {
                    System.out.println("Failed: " + ftp.getReplyString());
                }
            } else {
                try (OutputStream output = new FileOutputStream(local)) {
                    ftp.retrieveFile(remote, output);
                }

                if (keepAliveTimeoutSeconds > 0) {
                    showCslStats(ftp);
                }
            }

            ftp.noop(); // check that control connection is working OK

            ftp.logout();
        } catch (final FTPConnectionClosedException e) {
            error = true;
            System.err.println("Server closed connection.");
            e.printStackTrace();
        } catch (final IOException e) {
            error = true;
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (final IOException f) {
                    // do nothing
                }
            }
        }

        System.exit(error ? 1 : 0);
    } // end main

    private static void showCslStats(final FTPClient ftp) {
        @SuppressWarnings("deprecation") // debug code
        final int[] stats = ftp.getCslDebug();
        System.out.println("CslDebug=" + Arrays.toString(stats));

    }
}
