/* Copyright (C) 2023 Ilias Koukovinis <ilias.koukovinis@gmail.com>
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
package github.chatapp.client.main.java.service.client.io_client;

import java.io.IOException;
import java.io.OutputStream;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;

/**
 * @author Ilias Koukovinis
 *
 */
class ByteBufOutputStream implements AutoCloseable {

	private final OutputStream out;
	
	public ByteBufOutputStream(OutputStream out) {
		this.out = out;
	}

	public void write(ByteBuf msg) throws IOException {
		
		int msgLength = msg.readableBytes();

		byte[] msgBytes = new byte[msgLength];
		msg.readBytes(msgBytes);
		
		// The length of the payload is mentioned at its beginning.
		byte[] lengthOfMsgBytes = Ints.toByteArray(msgLength);

		byte[] payload = new byte[Integer.BYTES /* length of paylod */ + msgBytes.length];
		System.arraycopy(lengthOfMsgBytes, 0, payload, 0, lengthOfMsgBytes.length);
		System.arraycopy(msgBytes, 0, payload, Integer.BYTES, msgLength);
		
		out.write(payload);
	}

	@Override
	public void close() throws IOException {
		out.close();
	}
}
