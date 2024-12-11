/* Copyright (C) 2024 Ilias Koukovinis <ilias.koukovinis@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package github.koukobin.ermis.common.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.netty.buffer.ByteBuf;

/**
 * @author Ilias Koukovinis
 *
 */
public final class CompressionDetector {
	
	/**
	 *  Signature of Zstandard
	 *  
	 */
	private static final int ZSTD_MAGIC_NUMBER = 0xFD2FB528; // Zstandard magic number

	private CompressionDetector() {}
	
    /**
	 * Determines if the data in the ByteBuf is Zstandard-compressed. This method
	 * does not modify the ByteBuf's read index or content.
	 *
	 * @param data the ByteBuf to check
	 * @return true if the data is Zstandard-compressed, false otherwise
	 */
	public static boolean isZstdCompressed(ByteBuf data) {
		if (data == null || data.readableBytes() < 4) {
			return false; // Cannot be Zstandard if less than 4 bytes
		}

		byte[] array = new byte[Integer.BYTES];
		data.getBytes(data.readerIndex(), array); // Preserve ByteBuf state
		return isZstdCompressed(array);
	}

    /**
	 * Determines if the byte array represents Zstandard-compressed data.
	 *
	 * @param data the byte array to check
	 * @return true if the data is Zstandard-compressed, false otherwise
	 */
	public static boolean isZstdCompressed(byte[] data) {
		if (data == null || data.length < 4) {
			return false; // Cannot be Zstandard if less than 4 bytes
		}

		// Read the first 4 bytes to check the magic number
		int magicNumber = ByteBuffer.wrap(data, 0, Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).getInt();
		return isZstdCompressed(magicNumber);
	}

	/**
	 * Compares the given magic number to the Zstandard magic number.
	 *
	 * @param magicNumber the magic number to check
	 * @return true if the magic number matches Zstandard, false otherwise
	 */
    private static boolean isZstdCompressed(int magicNumber) {
        return magicNumber == ZSTD_MAGIC_NUMBER; 
    }
}

