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
package github.chatapp.server.main.java.server.netty_handlers;

import github.chatapp.common.entry.EntryType;
import github.chatapp.common.util.EnumIntConverter;
import github.chatapp.server.main.java.databases.postgresql.chatapp_database.ChatAppDatabase;
import github.chatapp.server.main.java.server.ClientInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Ilias Koukovinis
 * 
 */
public final class StartingEntryHandler extends ParentHandler {
	
	private boolean isLoggedIn;
	
	public StartingEntryHandler(ClientInfo clientInfo, boolean isLoggedIn) {
		super(clientInfo);
		this.isLoggedIn = isLoggedIn;
	}
	
	public StartingEntryHandler(ClientInfo clientInfo) {
		super(clientInfo);

		try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
			isLoggedIn = conn.isLoggedIn(clientInfo.getChannel().remoteAddress().getAddress());
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.channel().writeAndFlush(Unpooled.copyBoolean(isLoggedIn));
		
		// if the user is logged in we remove this handler and move on to starting the
		// messanging handler, otherwise do nothing and just wait for the user to send entry
		// type which is read in channelRead0 method
		if (isLoggedIn) {
			EntryHandler.login(ctx, clientInfo);
		}
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		
		EntryType entryType = EnumIntConverter.getIntAsEnum(msg.readInt(), EntryType.class);

		if (entryType == EntryType.LOGIN) {
			ctx.pipeline().replace(this, LoginHandler.class.getName(), new LoginHandler(clientInfo));
		} else if (entryType == EntryType.CREATE_ACCOUNT) {
			ctx.pipeline().replace(this, CreateAccountHandler.class.getName(), new CreateAccountHandler(clientInfo));
		} else {
			logger.debug("Unknown registration type");
		}
		
	}
}

