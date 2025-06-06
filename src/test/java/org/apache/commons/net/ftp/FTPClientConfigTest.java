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
package org.apache.commons.net.ftp;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import junit.framework.TestCase;

public class FTPClientConfigTest extends TestCase {

    private static final String A = "A";

    private static final String B = "B";
    private static final String C = "C";
    private static final String D = "D";
    private static final String E = "E";
    private static final String F = "F";
    private static final String badDelim = "jan,feb,mar,apr,may,jun,jul,aug.sep,oct,nov,dec";

    private static final String tooLong = "jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|jan";

    private static final String tooShort = "jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov";
    private static final String fakeLang = "abc|def|ghi|jkl|mno|pqr|stu|vwx|yza|bcd|efg|hij";

    /*
     * Class under test for void FTPClientConfig(String)
     */
    public void testFTPClientConfigString() {
        final FTPClientConfig config = new FTPClientConfig(FTPClientConfig.SYST_VMS);
        assertEquals(FTPClientConfig.SYST_VMS, config.getServerSystemKey());
        assertNull(config.getDefaultDateFormatStr());
        assertNull(config.getRecentDateFormatStr());
        assertNull(config.getShortMonthNames());
        assertNull(config.getServerTimeZoneId());
        assertNull(config.getServerLanguageCode());
    }

    /*
     * Class under test for void FTPClientConfig(String, String, String, String, String, String)
     */
    public void testFTPClientConfigStringStringStringStringStringString() {
        final FTPClientConfig conf = new FTPClientConfig(A, B, C, D, E, F);

        assertEquals("A", conf.getServerSystemKey());
        assertEquals("B", conf.getDefaultDateFormatStr());
        assertEquals("C", conf.getRecentDateFormatStr());
        assertEquals("E", conf.getShortMonthNames());
        assertEquals("F", conf.getServerTimeZoneId());
        assertEquals("D", conf.getServerLanguageCode());
    }

    public void testGetDateFormatSymbols() {

        assertThrows(IllegalArgumentException.class, () -> FTPClientConfig.getDateFormatSymbols(badDelim), "bad delimiter");
        assertThrows(IllegalArgumentException.class, () -> FTPClientConfig.getDateFormatSymbols(tooLong), "more than 12 months");
        assertThrows(IllegalArgumentException.class, () -> FTPClientConfig.getDateFormatSymbols(tooShort), "fewer than 12 months");
        DateFormatSymbols dfs2 = null;
        try {
            dfs2 = FTPClientConfig.getDateFormatSymbols(fakeLang);
        } catch (final Exception e) {
            fail("rejected valid short month string");
        }
        final SimpleDateFormat sdf1 = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
        final SimpleDateFormat sdf2 = new SimpleDateFormat("MMM dd, yyyy", dfs2);

        Date d1 = null;
        Date d2 = null;
        try {
            d1 = sdf1.parse("dec 31, 2004");
        } catch (final ParseException px) {
            fail("failed.to.parse.std");
        }
        try {
            d2 = sdf2.parse("hij 31, 2004");
        } catch (final ParseException px) {
            fail("failed.to.parse.weird");
        }

        assertEquals("different.parser.same.date", d1, d2);

        assertThrows(ParseException.class, () -> sdf1.parse("hij 31, 2004"));
        assertThrows(ParseException.class, () -> sdf2.parse("dec 31, 2004"));
    }

    public void testGetServerLanguageCode() {
    }

    public void testLookupDateFormatSymbols() {
        DateFormatSymbols dfs1 = null;
        DateFormatSymbols dfs2 = null;
        DateFormatSymbols dfs3 = null;
        DateFormatSymbols dfs4 = null;

        try {
            dfs1 = FTPClientConfig.lookupDateFormatSymbols("fr");
        } catch (final IllegalArgumentException e) {
            fail("french");
        }

        try {
            dfs2 = FTPClientConfig.lookupDateFormatSymbols("sq");
        } catch (final IllegalArgumentException e) {
            fail("albanian");
        }

        try {
            dfs3 = FTPClientConfig.lookupDateFormatSymbols("ru");
        } catch (final IllegalArgumentException e) {
            fail("unusupported.default.to.en");
        }
        try {
            dfs4 = FTPClientConfig.lookupDateFormatSymbols(fakeLang);
        } catch (final IllegalArgumentException e) {
            fail("not.language.code.but.defaults");
        }

        assertEquals(dfs3, dfs4);

        final SimpleDateFormat sdf1 = new SimpleDateFormat("d MMM yyyy", dfs1);
        final SimpleDateFormat sdf2 = new SimpleDateFormat("MMM dd, yyyy", dfs2);
        final SimpleDateFormat sdf3 = new SimpleDateFormat("MMM dd, yyyy", dfs3);
        Date d1 = null;
        Date d2 = null;
        Date d3 = null;
        try {
            d1 = sdf1.parse("31 d\u00e9c 2004");
        } catch (final ParseException px) {
            fail("failed.to.parse.french");
        }
        try {
            d2 = sdf2.parse("dhj 31, 2004");
        } catch (final ParseException px) {
            fail("failed.to.parse.albanian");
        }
        try {
            d3 = sdf3.parse("DEC 31, 2004");
        } catch (final ParseException px) {
            fail("failed.to.parse.'russian'");
        }
        assertEquals("different.parser.same.date", d1, d2);
        assertEquals("different.parser.same.date", d1, d3);

    }

    public void testSetShortMonthNames() {
    }

}
