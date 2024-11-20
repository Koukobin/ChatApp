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
package github.koukobin.ermis.server.main.java.server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Ilias Koukovinis
 *
 */
public class SimpleDecoder extends Decoder {

	private final int maxLength;

	public SimpleDecoder(int maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public boolean handleMessage(ChannelHandlerContext ctx, int length, ByteBuf in) {
		
		if (maxLength < length) {
			sendMessageExceedsMaximumMessageLength(ctx, maxLength);
			return false;
		}
		
		return true;
	}
	
}
