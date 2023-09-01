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

package coldfusion.runtime;

import coldfusion.tagext.io.OutputTag;
import java.io.IOException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.Tag;

@SuppressWarnings("ALL")
public abstract class CfJspPage {

  public Tag parent;
  public NeoPageContext pageContext = new NeoPageContext();

  protected void bindPageVariables(VariableScope varscope, LocalScope locscope) {

  }

  protected Variable bindPageVariable(String varName, VariableScope varScope, LocalScope locScope) {
    return null;
  }

  protected abstract Object runPage();

  public Tag _initTag(Class clazz, int slot, Tag parent) throws IOException {
    return new OutputTag();
  }

  public void _setCurrentLineNo(int lineNo) {
  }

  public void _whitespace(JspWriter out, String msg) {

  }
}
