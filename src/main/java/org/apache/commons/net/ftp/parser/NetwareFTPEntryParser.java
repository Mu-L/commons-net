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

package org.apache.commons.net.ftp.parser;

import java.text.ParseException;

import org.apache.commons.net.ftp.Configurable;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileEntryParser;

/**
 * Implements {@link FTPFileEntryParser} and {@link Configurable} for Netware Systems. Note that some proprietary extensions for Novell-specific operations
 * are not supported. See <a href="http://www.novell.com/documentation/nw65/index.html?page=/documentation/nw65/ftp_enu/data/fbhbgcfa.html">
 * http://www.novell.com/documentation/nw65/index.html?page=/documentation/nw65/ftp_enu/data/fbhbgcfa.html</a> for more details.
 *
 * @see FTPFileEntryParser Usage instructions.
 * @since 1.5
 */
public class NetwareFTPEntryParser extends ConfigurableFTPFileEntryParserImpl {

    /**
     * Default date format is e.g. Feb 22 2006
     */
    private static final String DEFAULT_DATE_FORMAT = "MMM dd yyyy";

    /**
     * Default recent date format is e.g. Feb 22 17:32
     */
    private static final String DEFAULT_RECENT_DATE_FORMAT = "MMM dd HH:mm";

    /**
     * this is the regular expression used by this parser. Example: d [-W---F--] SCION_VOL2 512 Apr 13 23:12 VOL2
     */
    private static final String REGEX = "(d|-){1}\\s+" // Directory/file flag
            + "\\[([-A-Z]+)\\]\\s+" // Attributes RWCEAFMS or -
            + "(\\S+)\\s+" + "(\\d+)\\s+" // Owner and size
            + "(\\S+\\s+\\S+\\s+((\\d+:\\d+)|(\\d{4})))" // Long/short date format
            + "\\s+(.*)"; // Filename (incl. spaces)

    /**
     * The default constructor for a NetwareFTPEntryParser object.
     *
     * @throws IllegalArgumentException Thrown if the regular expression is unparseable. Should not be seen under normal conditions. If it is seen, this is a
     *                                  sign that {@code REGEX} is not a valid regular expression.
     */
    public NetwareFTPEntryParser() {
        this(null);
    }

    /**
     * This constructor allows the creation of an NetwareFTPEntryParser object with something other than the default configuration.
     *
     * @param config The {@link FTPClientConfig configuration} object used to configure this parser.
     * @throws IllegalArgumentException Thrown if the regular expression is unparseable. Should not be seen under normal conditions. If it is seen, this is a
     *                                  sign that {@code REGEX} is not a valid regular expression.
     * @since 1.4
     */
    public NetwareFTPEntryParser(final FTPClientConfig config) {
        super(REGEX);
        configure(config);
    }

    /**
     * Gets a new default configuration to be used when this class is instantiated without a {@link FTPClientConfig FTPClientConfig} parameter being specified.
     *
     * @return the default configuration for this parser.
     */
    @Override
    protected FTPClientConfig getDefaultConfiguration() {
        return new FTPClientConfig(FTPClientConfig.SYST_NETWARE, DEFAULT_DATE_FORMAT, DEFAULT_RECENT_DATE_FORMAT);
    }

    /**
     * Parses a line of an NetwareFTP server file listing and converts it into a usable format in the form of an {@code FTPFile} instance. If the file
     * listing line doesn't describe a file, {@code null} is returned, otherwise a {@code FTPFile} instance representing the files in the
     * directory is returned.
     * <p>
     * Netware file permissions are in the following format: RWCEAFMS, and are explained as follows:
     * <ul>
     * <li><strong>S</strong> - Supervisor; All rights.
     * <li><strong>R</strong> - Read; Right to open and read or execute.
     * <li><strong>W</strong> - Write; Right to open and modify.
     * <li><strong>C</strong> - Create; Right to create; when assigned to a file, allows a deleted file to be recovered.
     * <li><strong>E</strong> - Erase; Right to delete.
     * <li><strong>M</strong> - Modify; Right to rename a file and to change attributes.
     * <li><strong>F</strong> - File Scan; Right to see directory or file listings.
     * <li><strong>A</strong> - Access Control; Right to modify trustee assignments and the Inherited Rights Mask.
     * </ul>
     *
     * See <a href="http://www.novell.com/documentation/nfap10/index.html?page=/documentation/nfap10/nfaubook/data/abxraws.html"> here</a> for more details
     *
     * @param entry A line of text from the file listing
     * @return An FTPFile instance corresponding to the supplied entry
     */
    @Override
    public FTPFile parseFTPEntry(final String entry) {

        final FTPFile f = new FTPFile();
        if (matches(entry)) {
            final String dirString = group(1);
            final String attrib = group(2);
            final String user = group(3);
            final String size = group(4);
            final String datestr = group(5);
            final String name = group(9);

            try {
                f.setTimestamp(super.parseTimestamp(datestr));
            } catch (final ParseException e) {
                // intentionally do nothing
            }

            // is it a DIR or a file
            if (dirString.trim().equals("d")) {
                f.setType(FTPFile.DIRECTORY_TYPE);
            } else {
                // Should be "-"
                f.setType(FTPFile.FILE_TYPE);
            }

            f.setUser(user);

            // set the name
            f.setName(name.trim());

            // set the size
            f.setSize(Long.parseLong(size.trim()));

            // Now set the permissions (or at least a subset thereof - full permissions would probably require
            // subclassing FTPFile and adding extra metainformation there)
            if (attrib.indexOf('R') != -1) {
                f.setPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION, true);
            }
            if (attrib.indexOf('W') != -1) {
                f.setPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION, true);
            }

            return f;
        }
        return null;

    }

}
