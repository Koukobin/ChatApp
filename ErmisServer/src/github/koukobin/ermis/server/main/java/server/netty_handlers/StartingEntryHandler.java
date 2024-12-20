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
package github.koukobin.ermis.server.main.java.server.netty_handlers;

import java.io.IOException;

import github.koukobin.ermis.common.entry.EntryType;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.ErmisDatabase;
import github.koukobin.ermis.server.main.java.server.ClientInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Ilias Koukovinis
 * 
 */
public final class StartingEntryHandler extends ParentHandler {
	
	private boolean isIPVerified;
	
	public StartingEntryHandler(ClientInfo clientInfo, boolean isLoggedIn) {
		super(clientInfo);
		this.isIPVerified = isLoggedIn;
	}
	
	public StartingEntryHandler(ClientInfo clientInfo) {
		super(clientInfo);

		try (ErmisDatabase.GeneralPurposeDBConnection conn = ErmisDatabase.getGeneralPurposeConnection()) {
			isIPVerified = conn.isLoggedIn(clientInfo.getChannel().remoteAddress().getAddress());
		}
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		ctx.channel().writeAndFlush(Unpooled.copyBoolean(isIPVerified));
	}

	@Override
	public void channelRead1(ChannelHandlerContext ctx, ByteBuf msg) throws IOException {

		EntryType entryType = EntryType.fromId(msg.readInt());

		switch (entryType) {
		case LOGIN -> {
			if (isIPVerified && msg.readableBytes() > 0) {
				
				byte[] email = new byte[msg.readInt()];
				msg.readBytes(email);
				
				byte[] passwordHash = new byte[msg.readableBytes()];
				msg.readBytes(passwordHash);
				
				boolean check;
				try (ErmisDatabase.GeneralPurposeDBConnection conn = ErmisDatabase.getGeneralPurposeConnection()) {
					check = conn.checkAuthenticationViaHash(new String(email), new String(passwordHash));
				}
				
				ctx.channel().writeAndFlush(Unpooled.copyBoolean(check));
				if (check) {
					EntryHandler.login(ctx, clientInfo);
					return;
				}
			}
			logger.debug("Moving into login!");
			ctx.pipeline().replace(this, LoginHandler.class.getName(), new LoginHandler(clientInfo));
		}
		case CREATE_ACCOUNT -> {
			logger.debug("Moving into account creation!");
			ctx.pipeline().replace(this, CreateAccountHandler.class.getName(), new CreateAccountHandler(clientInfo));
		}
		default -> logger.debug("Unknown registration type");
		}

	}
}

