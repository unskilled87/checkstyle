////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2016 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.mockito.BDDMockito;
import org.mockito.Mockito;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.io.Closeables;
import com.puppycrawl.tools.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.BriefUtLogger;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SuppressionFilter.class, CommonUtils.class})
public class SuppressionFilterTest extends BaseCheckTestSupport {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    protected String getPath(String filename) throws IOException {
        return super.getPath("filters" + File.separator + filename);
    }

    @Test
    public void testEqualsAndHashCode() {
        EqualsVerifier
                .forClass(SuppressionFilter.class)
                .usingGetClass()
                .withIgnoredFields("file", "optional", "configuration")
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void testAccept() throws CheckstyleException {
        final String fileName = "src/test/resources/com/puppycrawl/tools/checkstyle/filters/"
                          + "suppressions_none.xml";
        final boolean optional = false;
        final SuppressionFilter filter = createSupressionFilter(fileName, optional);

        final AuditEvent ev = new AuditEvent(this, "ATest.java", null);

        assertTrue(filter.accept(ev));
    }

    @Test
    public void testAcceptOnNullFile() throws CheckstyleException {
        final String fileName = null;
        final boolean optional = false;
        final SuppressionFilter filter = createSupressionFilter(fileName, optional);

        final AuditEvent ev = new AuditEvent(this, "AnyJava.java", null);

        assertTrue(filter.accept(ev));
    }

    @Test
    public void testNonExistanceSuppressionFileWithFalseOptional() {
        final String fileName = "src/test/resources/com/puppycrawl/tools/checkstyle/filters/"
                + "non_existance_suppresion_file.xml";
        try {
            final boolean optional = false;
            createSupressionFilter(fileName, optional);
            fail("Exception is expected");
        }
        catch (CheckstyleException ex) {
            assertEquals("Unable to find: " + fileName, ex.getMessage());
        }
    }

    @Test
    public void testExistanceInvalidSuppressionFileWithTrueOptional() {
        final String fileName = "src/test/resources/com/puppycrawl/tools/checkstyle/filters/"
                + "suppressions_invalid_file.xml";
        try {
            final boolean optional = true;
            createSupressionFilter(fileName, optional);
            fail("Exception is expected");
        }
        catch (CheckstyleException ex) {
            assertEquals("Unable to parse " + fileName + " - invalid files or checks format",
                    ex.getMessage());
        }
    }

    @Test
    public void testExistingSuppressionFileWithTrueOptional() throws Exception {
        final String fileName = "src/test/resources/com/puppycrawl/tools/checkstyle/filters/"
                + "suppressions_none.xml";
        final boolean optional = true;
        final SuppressionFilter filter = createSupressionFilter(fileName, optional);

        final AuditEvent ev = new AuditEvent(this, "AnyFile.java", null);

        assertTrue(filter.accept(ev));
    }

    @Test
    public void testExistingConfigWithTrueOptionalThrowsIoErrorWhileClosing()
            throws Exception {
        final InputStream inputStream = PowerMockito.mock(InputStream.class);
        Mockito.doThrow(IOException.class).when(inputStream).close();

        final URL url = PowerMockito.mock(URL.class);
        BDDMockito.given(url.openStream()).willReturn(inputStream);

        final URI uri = PowerMockito.mock(URI.class);
        BDDMockito.given(uri.toURL()).willReturn(url);

        PowerMockito.mockStatic(CommonUtils.class);

        final String fileName = "src/test/resources/com/puppycrawl/tools/checkstyle/filters/"
                + "suppressions_none.xml";
        BDDMockito.given(CommonUtils.getUriByFilename(fileName)).willReturn(uri);

        final boolean optional = true;
        final SuppressionFilter filter = createSupressionFilter(fileName, optional);
        final AuditEvent ev = new AuditEvent(this, "AnyFile.java", null);
        assertTrue(filter.accept(ev));
    }

    @Test
    public void testNonExistanceSuppressionFileWithTrueOptional() throws Exception {
        final String fileName = "src/test/resources/com/puppycrawl/tools/checkstyle/filters/"
                + "non_existance_suppresion_file.xml";
        final boolean optional = true;
        final SuppressionFilter filter = createSupressionFilter(fileName, optional);

        final AuditEvent ev = new AuditEvent(this, "AnyFile.java", null);

        assertTrue(filter.accept(ev));
    }

    @Test
    public void testNonExistanceSuppressionUrlWithTrueOptional() throws Exception {
        final String fileName =
                "http://checkstyle.sourceforge.net/non_existing_suppression.xml";
        final boolean optional = true;
        final SuppressionFilter filter = createSupressionFilter(fileName, optional);

        final AuditEvent ev = new AuditEvent(this, "AnyFile.java", null);

        assertTrue(filter.accept(ev));
    }

    @Test
    public void testLocalFileExternalResourceContentDoesNotChange() throws Exception {
        final DefaultConfiguration filterConfig = createCheckConfig(SuppressionFilter.class);
        filterConfig.addAttribute("file", getPath("suppressions_none.xml"));

        final DefaultConfiguration checkerConfig = new DefaultConfiguration("checkstyle_checks");
        checkerConfig.addChild(filterConfig);
        final String cacheFile = temporaryFolder.newFile().getPath();
        checkerConfig.addAttribute("cacheFile", cacheFile);

        final Checker checker = new Checker();
        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        checker.addListener(new BriefUtLogger(stream));
        checker.configure(checkerConfig);

        final String filePath = temporaryFolder.newFile("file.java").getPath();
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

        verify(checker, filePath, expected);
        // One more time to use cache.
        verify(checker, filePath, expected);
    }

    @Test
    public void testRemoteFileExternalResourceContentDoesNotChange() throws Exception {
        final String[] urlCandidates = {
            "http://checkstyle.sourceforge.net/files/suppressions_none.xml",
            "https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/site/resources/"
                + "files/suppressions_none.xml",
        };

        String urlForTest = null;
        for (String url : urlCandidates) {
            if (isConnectionAvailableAndStable(url)) {
                urlForTest = url;
                break;
            }
        }

        // Run the test only if connection is available and url is reachable.
        Assume.assumeFalse(urlForTest == null);

        final DefaultConfiguration firstFilterConfig = createCheckConfig(SuppressionFilter.class);
        firstFilterConfig.addAttribute("file", urlForTest);

        final DefaultConfiguration firstCheckerConfig =
            new DefaultConfiguration("checkstyle_checks");
        firstCheckerConfig.addChild(firstFilterConfig);
        final String cacheFile = temporaryFolder.newFile().getPath();
        firstCheckerConfig.addAttribute("cacheFile", cacheFile);

        final Checker checker = new Checker();
        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        checker.configure(firstCheckerConfig);
        checker.addListener(new BriefUtLogger(stream));

        final String pathToEmptyFile = temporaryFolder.newFile("file.java").getPath();
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

        verify(checker, pathToEmptyFile, expected);

        // One more time to use cache.
        final DefaultConfiguration secondFilterConfig = createCheckConfig(SuppressionFilter.class);
        secondFilterConfig.addAttribute("file", urlForTest);

        final DefaultConfiguration secondCheckerConfig =
            new DefaultConfiguration("checkstyle_checks");
        secondCheckerConfig.addAttribute("cacheFile", cacheFile);
        secondCheckerConfig.addChild(secondFilterConfig);

        checker.configure(secondCheckerConfig);

        verify(checker, pathToEmptyFile, expected);
    }

    private static boolean isConnectionAvailableAndStable(String url) throws Exception {
        boolean available = false;

        if (isUrlReachable(url)) {
            final int attemptLimit = 5;
            int attemptCount = 0;

            while (attemptCount <= attemptLimit) {
                final URL addres = new URL(url);
                InputStream stream = null;
                try {
                    stream = addres.openStream();
                    // Attemt to read a byte in order to check wtether file content is available
                    available = stream.read() != -1;
                    break;
                }
                catch (IOException ex) {
                    // for some reason Travis CI failed some times (unstable) on reading the file
                    if (attemptCount < attemptLimit && ex.getMessage().contains("Unable to read")) {
                        attemptCount++;
                        available = false;
                        // wait for bad / disconnection time to pass
                        Thread.sleep(1000);
                    }
                    else {
                        Closeables.closeQuietly(stream);
                        throw ex;
                    }
                }
                finally {
                    Closeables.closeQuietly(stream);
                }
            }
        }
        return available;
    }

    private static boolean isUrlReachable(String url) {
        try {
            final URL verifiableUrl = new URL(url);
            final HttpURLConnection urlConnect = (HttpURLConnection) verifiableUrl.openConnection();
            urlConnect.getContent();
        }
        catch (IOException ex) {
            return false;
        }
        return true;
    }

    private static SuppressionFilter createSupressionFilter(String fileName, boolean optional)
            throws CheckstyleException {
        final SuppressionFilter suppressionFilter = new SuppressionFilter();
        suppressionFilter.setFile(fileName);
        suppressionFilter.setOptional(optional);
        suppressionFilter.finishLocalSetup();
        return suppressionFilter;
    }
}
