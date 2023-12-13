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

package com.intergral.deep.tests.it.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intergral.deep.proto.common.v1.KeyValue;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;

public class Cf2018Test extends ACFTest {

  public Cf2018Test() {
    super("adobecoldfusion/coldfusion:latest-2018");
  }

  @Override
  protected void checkPluignData(final Snapshot snapshot) {
    final KeyValue cfVersion = findAttribute(snapshot, "cf_version");
    assertEquals("2018", cfVersion.getValue().getStringValue());
    final KeyValue appName = findAttribute(snapshot, "app_name");
    assertEquals("cf-test-app", appName.getValue().getStringValue());
  }

}
