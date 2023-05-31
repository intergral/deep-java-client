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

package com.intergral.deep.agent.tracepoint.inst;


import java.util.Set;

public class SetClassScanner implements IClassScanner
{
    private final Set<String> classNames;


    public SetClassScanner( final Set<String> classNames )
    {
        this.classNames = classNames;
    }


    @Override
    public boolean scanClass( final Class<?> allLoadedClass )
    {
        return this.classNames.contains( InstUtils.internalClass( allLoadedClass ) ) ||
                this.classNames.contains( allLoadedClass.getSimpleName() ) ||
                allLoadedClass.getName().contains( "$" ) &&
                        this.classNames.contains( InstUtils.internalClassStripInner( allLoadedClass ) );
    }
}
