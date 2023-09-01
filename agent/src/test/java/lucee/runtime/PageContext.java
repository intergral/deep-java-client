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

package lucee.runtime;

import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.scope.Undefined;

@SuppressWarnings("ALL")
public class PageContext {

  private Object server = new Scope("server");
  private Object variables = new Scope("variables");
  private Object local = new Scope("local");
  private Object cookie = new Scope("cookie");
  private Object session = new Scope("session");
  private Object application = new Scope("application");
  private Object cgiR = new Scope("cgiR");
  private Object request = new Scope("request");
  private Object _form = new Scope("_form");
  private Object _url = new Scope("_url");
  private Object client = new Scope("client");
  private Object threads = new Scope("threads");

  public void write(String str) {

  }

  public Undefined us() {
    return new Undefined() {
      @Override
      public Object get(final Key k) {
        return new Object();
      }

      @Override
      public Object set(final Key ket, final Object obj) {
        return obj;
      }
    };
  }

  public void outputStart() {
  }

  public void outputEnd() {
  }

  public static class Scope {

    private final String threads;


    public Scope(final String threads) {
      this.threads = threads;
    }

    public boolean isInitalized() {
      return false;
    }
  }
}
