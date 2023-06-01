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

package com.intergral.deep;

public class Person
{
    private final Person parent;
    private final String name;


    public Person( final String name )
    {
        this( null, name );
    }


    public Person( final Person parent, final String name )
    {
        this.parent = parent;
        this.name = name;
    }


    public Person getParent()
    {
        return parent;
    }


    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "Person{" +
                "parent=" + parent +
                ", name='" + name + '\'' +
                '}';
    }
}