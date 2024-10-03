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
package main.java.server.netty_handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import main.java.server.ClientInfo;

/**
 * @author Ilias Koukovinis
 * 
 */
abstract sealed class ParentHandler extends SimpleChannelInboundHandler<ByteBuf> permits MessageHadler, StartingEntryHandler, EntryHandler {

	protected static final Logger logger = LogManager.getLogger("server");
	protected final ClientInfo clientInfo;
	
	protected ParentHandler(ClientInfo clientInfo) {
		this.clientInfo = clientInfo;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.debug(Throwables.getStackTraceAsString(cause));
	}
}