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

package com.intergral.deep.agent;

import java.util.concurrent.ThreadLocalRandom;

public class IDUtils
{
    static final int BYTE_BASE16 = 2;
    private static final String ALPHABET = "0123456789abcdef";
    private static final char[] ENCODING = buildEncodingArray();

    public static String randomId()
    {
        final ThreadLocalRandom current = ThreadLocalRandom.current();
        final long longId = current.nextLong();
        char[] result = new char[16];
        longToBase16String( longId, result, 0 );
        return new String( result, 0, 16 );
    }

    private static char[] buildEncodingArray()
    {
        char[] encoding = new char[512];
        for( int i = 0; i < 256; ++i )
        {
            encoding[i] = ALPHABET.charAt( i >>> 4 );
            encoding[i | 0x100] = ALPHABET.charAt( i & 0xF );
        }
        return encoding;
    }

    /**
     * Appends the base16 encoding of the specified {@code value} to the {@code dest}.
     *
     * @param value      the value to be converted.
     * @param dest       the destination char array.
     * @param destOffset the starting offset in the destination char array.
     */
    public static void longToBase16String( long value, char[] dest, int destOffset )
    {
        byteToBase16( (byte) (value >> 56 & 0xFFL), dest, destOffset );
        byteToBase16( (byte) (value >> 48 & 0xFFL), dest, destOffset + BYTE_BASE16 );
        byteToBase16( (byte) (value >> 40 & 0xFFL), dest, destOffset + 2 * BYTE_BASE16 );
        byteToBase16( (byte) (value >> 32 & 0xFFL), dest, destOffset + 3 * BYTE_BASE16 );
        byteToBase16( (byte) (value >> 24 & 0xFFL), dest, destOffset + 4 * BYTE_BASE16 );
        byteToBase16( (byte) (value >> 16 & 0xFFL), dest, destOffset + 5 * BYTE_BASE16 );
        byteToBase16( (byte) (value >> 8 & 0xFFL), dest, destOffset + 6 * BYTE_BASE16 );
        byteToBase16( (byte) (value & 0xFFL), dest, destOffset + 7 * BYTE_BASE16 );
    }

    /**
     * Encodes the specified byte, and returns the encoded {@code String}.
     *
     * @param value      the value to be converted.
     * @param dest       the destination char array.
     * @param destOffset the starting offset in the destination char array.
     */
    public static void byteToBase16( byte value, char[] dest, int destOffset )
    {
        int b = value & 0xFF;
        dest[destOffset] = ENCODING[b];
        dest[destOffset + 1] = ENCODING[b | 0x100];
    }
}
