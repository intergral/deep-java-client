/*
 *     Copyright (C) 2023  Intergral GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.intergral.deep.agent.tracepoint.inst.jsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap.SmapUtils;
import com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap.SourceMap;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

class JSPUtilsTest {

  @Test
  void isJspClass() {
    assertTrue(JSPUtils.isJspClass("_jsp", Settings.coerc("org.apache.jsp,jsp", List.class),
        "org.apache.jsp.jdbc.views.companies_jsp"));
    assertTrue(JSPUtils.isJspClass("_jsp", Settings.coerc("org.apache.jsp,jsp", List.class),
        "org.apache.jsp.views.companies_jsp"));
    assertTrue(JSPUtils.isJspClass("_jsp", Settings.coerc("org.apache.jsp,jsp", List.class),
        "org.apache.jsp.companies_jsp"));
    assertTrue(JSPUtils.isJspClass("_jsp", Settings.coerc("org.apache.jsp,jsp", List.class),
        "jsp.companies_jsp"));

    assertFalse(JSPUtils.isJspClass("_jsp", Settings.coerc("org.apache.jsp,jsp", List.class),
        "companies_jsp"));
  }


  @Test
  void sourceMap() throws Exception {
    final File file = new File("src/test/resources");
    final URL resourceUrl = file.toURI().toURL();
    final URL[] classUrls = {resourceUrl};
    final URLClassLoader ucl = new URLClassLoader(classUrls);
    final Class c = ucl.loadClass("org.apache.jsp.jdbc.views.companies_jsp");

    final SourceMap sourceMap = JSPUtils.getSourceMap(c);

    assertNotNull(sourceMap);
    final List<String> filenames = sourceMap.getFilenames();
    assertTrue(filenames.containsAll(Arrays.asList("companies.jsp", "header.jsp", "setup.jsp", "footer.jsp")));
  }


  @Test
  void sourceMap_bytes() throws Exception {
    final InputStream resourceAsStream = getClass().getResourceAsStream("/org/apache/jsp/jdbc/views/companies_jsp.class");
    final byte[] bytes = new byte[resourceAsStream.available()];
    resourceAsStream.read(bytes);

    final SourceMap sourceMap = JSPUtils.getSourceMap(bytes);

    assertNotNull(sourceMap);
    final List<String> filenames = sourceMap.getFilenames();
    assertTrue(filenames.containsAll(Arrays.asList("companies.jsp", "header.jsp", "setup.jsp", "footer.jsp")));
  }


  @Test
  void sourceMap_bytes_src() throws Exception {
    final InputStream resourceAsStream = getClass().getResourceAsStream("/org/apache/jsp/jdbc/views/companies_jsp.class");
    final byte[] bytes = new byte[resourceAsStream.available()];
    resourceAsStream.read(bytes);

    final String src = SmapUtils.scanSource(new ClassReader(bytes));

    assertNotNull(src);
    assertEquals("companies_jsp.java", src);
  }
}