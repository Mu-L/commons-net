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
package org.apache.commons.net.telnet;

import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

/**
 * JUnit functional test for TelnetClient. Connects to the weather forecast service rainmaker.wunderground.com and asks for Los Angeles forecast.
 */
public class TelnetClientFunctionalTest extends TestCase {
    protected TelnetClient tc1;

    /**
     * test setUp
     */
    @Override
    protected void setUp() {
        tc1 = new TelnetClient();
    }

    /*
     * Do the functional test: - connect to the weather service - press return on the first menu - send LAX on the second menu - send X to exit
     */
    public void testFunctionalTest() throws Exception {
        boolean testresult = false;
        tc1.connect("rainmaker.wunderground.com", 3000);

        try (InputStream is = tc1.getInputStream(); final OutputStream os = tc1.getOutputStream()) {

            boolean cont = waitForString(is, "Return to continue:", 30000);
            if (cont) {
                os.write("\n".getBytes());
                os.flush();
                cont = waitForString(is, "city code--", 30000);
            }
            if (cont) {
                os.write("LAX\n".getBytes());
                os.flush();
                cont = waitForString(is, "Los Angeles", 30000);
            }
            if (cont) {
                cont = waitForString(is, "X to exit:", 30000);
            }
            if (cont) {
                os.write("X\n".getBytes());
                os.flush();
                tc1.disconnect();
                testresult = true;
            }

            assertTrue(testresult);
        }
    }

    /*
     * Helper method. waits for a string with timeout
     */
    public boolean waitForString(final InputStream is, final String end, final long timeout) throws Exception {
        final byte[] buffer = new byte[32];
        final long starttime = System.currentTimeMillis();

        String readbytes = "";
        while (!readbytes.contains(end) && System.currentTimeMillis() - starttime < timeout) {
            if (is.available() > 0) {
                final int retRead = is.read(buffer);
                readbytes += new String(buffer, 0, retRead);
            } else {
                Thread.sleep(500);
            }
        }

        return readbytes.indexOf(end) >= 0;
    }
}