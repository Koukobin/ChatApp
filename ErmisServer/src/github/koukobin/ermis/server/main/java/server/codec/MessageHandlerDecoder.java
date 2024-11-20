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

import java.nio.charset.Charset;

import github.koukobin.ermis.common.message_types.ClientCommandType;
import github.koukobin.ermis.common.message_types.ClientMessageType;
import github.koukobin.ermis.common.message_types.ContentType;
import github.koukobin.ermis.common.message_types.ServerMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Ilias Koukovinis
 *
 */
public class MessageHandlerDecoder extends Decoder {

	private final int maxMessageTextLength;
	private final int maxMessageFileLength;
	
	public MessageHandlerDecoder(int maxMessageTextLength, int maxMessageFileLength) {
		this.maxMessageTextLength = maxMessageTextLength;
		this.maxMessageFileLength = maxMessageFileLength;
	}

	@Override
	public boolean handleMessage(ChannelHandlerContext ctx, int length, ByteBuf in) {
			
		boolean isSuccesfull = false;
		
		ClientMessageType messageType;
		
		try {
			messageType = ClientMessageType.fromId(in.readInt());
		} catch (IndexOutOfBoundsException iobe) {
			ByteBuf payload = ctx.alloc().ioBuffer();
			payload.writeInt(ServerMessageType.SERVER_MESSAGE_INFO.id);
			payload.writeBytes("Message type not known!".getBytes());
			ctx.channel().writeAndFlush(payload);
			return isSuccesfull;
		}
		
		final int maxLength;
		
		switch (messageType) {
		case CLIENT_CONTENT -> {
			
			ContentType contentType;

			try {
				contentType = ContentType.fromId(in.readInt());
			} catch (IndexOutOfBoundsException iobe) {
				ByteBuf payload = ctx.alloc().ioBuffer();
				payload.writeInt(ServerMessageType.SERVER_MESSAGE_INFO.id);
				payload.writeBytes("Content type not known!".getBytes());
				ctx.channel().writeAndFlush(payload);
				return isSuccesfull;
			}
			
			switch (contentType) {
			case FILE:
				maxLength = maxMessageFileLength;
				break;
			case TEXT:
				maxLength = maxMessageTextLength;
				break;
			default:
				ByteBuf payload = ctx.alloc().ioBuffer();
				payload.writeInt(ServerMessageType.SERVER_MESSAGE_INFO.id);
				payload.writeBytes("Content type not implemented!".getBytes());
				ctx.channel().writeAndFlush(payload);
				return isSuccesfull;
			}
		}
		case COMMAND -> {
			try {
				ClientCommandType commandType = ClientCommandType.fromId(in.readInt());
				maxLength = switch (commandType) {
				case ADD_ACCOUNT_ICON -> 
					maxMessageFileLength;
				default -> 
					maxMessageTextLength;
				};
			} catch (IndexOutOfBoundsException iooe) {
				System.out.println("command not known");
				ByteBuf payload = ctx.alloc().ioBuffer();
				payload.writeInt(ServerMessageType.SERVER_MESSAGE_INFO.id);
				payload.writeBytes("Command not known!".getBytes());
				ctx.channel().writeAndFlush(payload);
				return isSuccesfull;
			}
		}
		default -> {
			ByteBuf payload = ctx.alloc().ioBuffer();
			payload.writeInt(ServerMessageType.SERVER_MESSAGE_INFO.id);
			payload.writeBytes("Message type not implemented!".getBytes());
			ctx.channel().writeAndFlush(payload);
			return isSuccesfull;
		}
		}
		
		if (maxLength < length) {
			sendMessageExceedsMaximumMessageLength(ctx, maxLength);
			return isSuccesfull;
		}
		
		isSuccesfull = true;
		return isSuccesfull;
	}
	
}
