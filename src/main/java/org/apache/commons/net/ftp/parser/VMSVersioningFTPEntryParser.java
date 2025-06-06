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

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPClientConfig;

/**
 * Special implementation VMSFTPEntryParser with versioning turned on. This parser removes all duplicates and only leaves the version with the highest version
 * number for each file name.
 * <p>
 * This is a sample of VMS LIST output
 * </p>
 *
 * <pre>
 *  "1-JUN.LIS;1              9/9           2-JUN-1998 07:32:04  [GROUP,OWNER]    (RWED,RWED,RWED,RE)",
 *  "1-JUN.LIS;2              9/9           2-JUN-1998 07:32:04  [GROUP,OWNER]    (RWED,RWED,RWED,RE)",
 *  "DATA.DIR;1               1/9           2-JUN-1998 07:32:04  [GROUP,OWNER]    (RWED,RWED,RWED,RE)",
 * </pre>
 *
 * @see org.apache.commons.net.ftp.FTPFileEntryParser FTPFileEntryParser (for usage instructions)
 */
public class VMSVersioningFTPEntryParser extends VMSFTPEntryParser {

    /**
     * Guard against polynomial regular expression used on uncontrolled data.
     * Don't look for more than 20 digits for the version.
     * Don't look for more than 80 spaces after the version.
     * Don't look for more than 80 characters after the spaces.
     */
    private static final String REGEX = "(.*?);([0-9]{1,20})\\s{0,80}.{0,80}";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    /**
     * Constructor for a VMSFTPEntryParser object.
     *
     * @throws IllegalArgumentException Thrown if the regular expression is unparseable. Should not be seen under normal conditions. If the exception is seen,
     *                                  this is a sign that {@code REGEX} is not a valid regular expression.
     */
    public VMSVersioningFTPEntryParser() {
        this(null);
    }

    /**
     * This constructor allows the creation of a VMSVersioningFTPEntryParser object with something other than the default configuration.
     *
     * @param config The {@link FTPClientConfig configuration} object used to configure this parser.
     * @throws IllegalArgumentException Thrown if the regular expression is unparseable. Should not be seen under normal conditions. If the exception is seen,
     *                                  this is a sign that {@code REGEX} is not a valid regular expression.
     * @since 1.4
     */
    public VMSVersioningFTPEntryParser(final FTPClientConfig config) {
        configure(config);
    }

    @Override
    protected boolean isVersioning() {
        return true;
    }

    /**
     * Implement hook provided for those implementers (such as VMSVersioningFTPEntryParser, and possibly others) which return multiple files with the same name
     * to remove the duplicates.
     *
     * @param original Original list
     * @return Original list purged of duplicates
     */
    @Override
    public List<String> preParse(final List<String> original) {
        final HashMap<String, Integer> existingEntries = new HashMap<>();
        final ListIterator<String> iter = original.listIterator();
        while (iter.hasNext()) {
            final String entry = iter.next().trim();
            final MatchResult result;
            final Matcher matcher = PATTERN.matcher(entry);
            if (matcher.matches()) {
                result = matcher.toMatchResult();
                final String name = result.group(1);
                final String version = result.group(2);
                final Integer nv = Integer.valueOf(version);
                final Integer existing = existingEntries.get(name);
                if (null != existing && nv.intValue() < existing.intValue()) {
                    iter.remove(); // removes older version from original list.
                    continue;
                }
                existingEntries.put(name, nv);
            }

        }
        // we've now removed all entries less than with less than the largest
        // version number for each name that were listed after the largest.
        // we now must remove those with smaller than the largest version number
        // for each name that were found before the largest
        while (iter.hasPrevious()) {
            final String entry = iter.previous().trim();
            MatchResult result = null;
            final Matcher matcher = PATTERN.matcher(entry);
            if (matcher.matches()) {
                result = matcher.toMatchResult();
                final String name = result.group(1);
                final String version = result.group(2);
                final int nv = Integer.parseInt(version);
                final Integer existing = existingEntries.get(name);
                if (null != existing && nv < existing.intValue()) {
                    iter.remove(); // removes older version from original list.
                }
            }

        }
        return original;
    }

}
