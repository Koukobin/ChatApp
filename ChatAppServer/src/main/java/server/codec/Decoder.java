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

import java.util.List;

import org.chatapp.commons.EnumIntConverter;
import org.chatapp.commons.ServerMessageType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

/**
 * @author Ilias Koukovinis
 *
 */
public abstract class Decoder extends ReplayingDecoder<ByteBuf> {

	private boolean handleMessageSuccessfull;
	private boolean hasReadLength = false;
	private int length;
	
	protected Decoder() {}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		if (!hasReadLength) {

			length = in.readInt();

			int readerIndex = in.readerIndex();
			handleMessageSuccessfull = handleMessage(ctx, length, in);
			in.readerIndex(readerIndex);

			checkpoint();
			hasReadLength = true;
		}

		if (hasReadLength) {

			if (handleMessageSuccessfull) {
				ByteBuf payload = Unpooled.buffer(length);
				in.readBytes(payload);
				out.add(payload);
			} else {
				in.skipBytes(length);
				handleMessageSuccessfull = false;
			}

			hasReadLength = false;
			checkpoint();
		}
	}
	
	/**
	 * 
	 * @param ctx
	 * @param length
	 * @param in
	 * @return Whether or not handling message was succesfull
	 */
	public abstract boolean handleMessage(ChannelHandlerContext ctx, int length, ByteBuf in);

	protected void sendMessageExceedesMaximumMessageLengthWarning(ChannelHandlerContext ctx, int maxLength) {
		ByteBuf payload = ctx.alloc().ioBuffer();
		payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
		payload.writeBytes(("Message length exceedes maximum message length (" + maxLength + " characters)").getBytes());
		ctx.channel().writeAndFlush(payload);
	}
}
