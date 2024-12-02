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
package github.koukobin.ermis.server.main.java.server.util;

import github.koukobin.ermis.common.message_types.ServerMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Utility class that facilitates communication between the server and the client
 * 
 * @author Ilias Koukovinis
 *
 */
public final class MessageByteBufCreator {

	private MessageByteBufCreator() {}
	
	public static void sendMessageInfo(ChannelHandlerContext ctx, String text) {
		ByteBuf payload = ctx.alloc().ioBuffer();
		payload.writeInt(ServerMessageType.SERVER_MESSAGE_INFO.id);
		payload.writeBytes(text.getBytes());
		ctx.channel().writeAndFlush(payload);
	}
	
}
