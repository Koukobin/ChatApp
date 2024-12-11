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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.luben.zstd.Zstd;
import com.google.common.base.Throwables;

import github.koukobin.ermis.common.message_types.ClientCommandType;
import github.koukobin.ermis.common.message_types.ClientMessageType;
import github.koukobin.ermis.common.message_types.ContentType;
import github.koukobin.ermis.common.util.CompressionDetector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Ilias Koukovinis
 *
 */
public class MessageHandlerDecoder extends Decoder {

	private static final Logger logger = LogManager.getLogger("server");
	
	private final int maxMessageTextLength;
	private final int maxMessageFileLength;
	
	public MessageHandlerDecoder(int maxMessageTextLength, int maxMessageFileLength) {
		this.maxMessageTextLength = maxMessageTextLength;
		this.maxMessageFileLength = maxMessageFileLength;
	}

	private static ByteBuf decompress(ChannelHandlerContext ctx, ByteBuf in) {
        int compressedLength = in.readInt();
        byte[] compressedData = new byte[compressedLength];
        in.readBytes(compressedData);

        try {
            return ctx.alloc().ioBuffer()
					.writeBytes(Zstd.decompress(compressedData, (int) Zstd.decompressedSize(compressedData)));
		} catch (Exception e) {
			logger.debug(Throwables.getStackTraceAsString(e));
			createErrorResponse(ctx, "Decompression failed");
			return null; // Indicate failure by returning null
		}
	}
	

	@Override
	public boolean handleMessage(ChannelHandlerContext ctx, int length, ByteBuf in) {
		ByteBuf data = in;

		if (CompressionDetector.isZstdCompressed(data)) {
			data = decompress(ctx, in);
			if (data == null) {
				return false; // Decompression failed, terminate the method early
			}
		}

		ClientMessageType messageType;
		try {
			messageType = ClientMessageType.fromId(data.readInt());
		} catch (IndexOutOfBoundsException iobe) {
			logger.debug(Throwables.getStackTraceAsString(iobe));
			createErrorResponse(ctx, "Message type not known!");
			return false;
		}

		int maxLength = determineMaxLength(ctx, data, messageType);
		if (maxLength == -1) {
			return false; // Invalid message type or content type
		}

		if (maxLength < length) {
			sendMessageExceedsMaximumMessageLength(ctx, maxLength);
			return false;
		}

		return true;

	}

	private int determineMaxLength(ChannelHandlerContext ctx, ByteBuf data, ClientMessageType messageType) {
		switch (messageType) {
		case CLIENT_CONTENT:
			ContentType contentType;
			try {
				contentType = ContentType.fromId(data.readInt());
			} catch (IndexOutOfBoundsException iobe) {
				logger.debug(Throwables.getStackTraceAsString(iobe));
				createErrorResponse(ctx, "Content type not known!");
				return -1;
			}
			return getMaxLengthForContentType(ctx, contentType);
		case COMMAND:
			return getMaxLengthForCommand(ctx, data);
		default:
			logger.debug("Message type not implemented!");
			createErrorResponse(ctx, "Message type not implemented!");
			return -1;
		}
	}

	private int getMaxLengthForContentType(ChannelHandlerContext ctx, ContentType contentType) {
		switch (contentType) {
		case FILE, IMAGE:
			return maxMessageFileLength;
		case TEXT:
			return maxMessageTextLength;
		default:
			logger.debug("Content type not implemented!");
			createErrorResponse(ctx, "Content type not implemented!");
			return -1;
		}
	}

	private int getMaxLengthForCommand(ChannelHandlerContext ctx, ByteBuf data) {
		try {
			ClientCommandType commandType = ClientCommandType.fromId(data.readInt());
			return (commandType == ClientCommandType.ADD_ACCOUNT_ICON) ? maxMessageFileLength : maxMessageTextLength;
		} catch (IndexOutOfBoundsException iooe) {
			logger.debug(Throwables.getStackTraceAsString(iooe));
			createErrorResponse(ctx, "Command not known!");
			return -1;
		}
	}
	
}
