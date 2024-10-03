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
package main.java.server.codec;

import org.chatapp.commons.EnumIntConverter;
import org.chatapp.commons.ServerMessageType;
import org.chatapp.commons.ContentType;
import org.chatapp.commons.ClientCommandType;
import org.chatapp.commons.ClientMessageType;

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
			messageType = EnumIntConverter.getIntAsEnum(in.readInt(), ClientMessageType.class);
		} catch (IndexOutOfBoundsException iobe) {
			ByteBuf payload = ctx.alloc().ioBuffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
			payload.writeBytes("Message type not known!".getBytes());
			ctx.channel().writeAndFlush(payload);
			return isSuccesfull;
		}
		
		final int maxLength;
		
		switch (messageType) {
		case CLIENT_CONTENT -> {
			
			ContentType contentType;

			try {
				contentType = EnumIntConverter.getIntAsEnum(in.readInt(), ContentType.class);
			} catch (IndexOutOfBoundsException iobe) {
				ByteBuf payload = ctx.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
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
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
				payload.writeBytes("Content type not implemented!".getBytes());
				ctx.channel().writeAndFlush(payload);
				return isSuccesfull;
			}
		}
		case COMMAND -> {
			try {
				ClientCommandType commandType = EnumIntConverter.getIntAsEnum(in.readInt(), ClientCommandType.class);
				maxLength = maxMessageTextLength;
			} catch (IndexOutOfBoundsException iooe) {
				ByteBuf payload = ctx.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
				payload.writeBytes("Command not known!".getBytes());
				ctx.channel().writeAndFlush(payload);
				return isSuccesfull;
			}
		}
		default -> {
			ByteBuf payload = ctx.alloc().ioBuffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
			payload.writeBytes("Message type not implemented!".getBytes());
			ctx.channel().writeAndFlush(payload);
			return isSuccesfull;
		}
		}
		
		if (maxLength < length) {
			sendMessageExceedesMaximumMessageLengthWarning(ctx, maxLength);
			return isSuccesfull;
		}
		
		isSuccesfull = true;
		return isSuccesfull;
	}
	
}
