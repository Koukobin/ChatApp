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

import github.koukobin.ermis.server.main.java.server.ClientInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Ilias Koukovinis
 *
 */
abstract sealed class EntryHandler extends ParentHandler permits LoginHandler, CreateAccountHandler, VerificationHandler {

	private boolean hasFailed = false;
	private boolean isSuccesfull = false;
	
	private Runnable resultRunnable;
	
	protected EntryHandler(ClientInfo clientInfo) {
		super(clientInfo);
	}

	@Override
	public final void channelReadComplete(ChannelHandlerContext ctx) {
		if (hasFailed || isSuccesfull) {
			resultRunnable.run();
		}
	}

	public abstract void executeEntryAction(ChannelHandlerContext ctx, ByteBuf msg) throws IOException;
	public abstract void channelRead2(ChannelHandlerContext ctx, ByteBuf msg) throws IOException;

	@Override
	public final void channelRead1(ChannelHandlerContext ctx, ByteBuf msg) throws IOException {

		boolean isAction = msg.readBoolean();
		
		if (isAction) {
			executeEntryAction(ctx, msg);
		} else {
			channelRead2(ctx, msg);
		}
	}
	
	protected abstract Runnable onSuccess(ChannelHandlerContext ctx);

	protected void success(ChannelHandlerContext ctx) {
		resultRunnable = onSuccess(ctx);
		isSuccesfull = true;
	}

	public void failed(ChannelHandlerContext ctx) {
		resultRunnable = () -> registrationFailed(ctx, clientInfo);
		hasFailed = true;
	}
	
	public static void login(ChannelHandlerContext ctx, ClientInfo clientInfo) {
		ctx.pipeline().replace(ctx.handler(), MessageHandler.class.getName(), new MessageHandler(clientInfo));
	}

	public static void registrationFailed(ChannelHandlerContext ctx, ClientInfo clientInfo) {
		ctx.pipeline().replace(ctx.handler(), StartingEntryHandler.class.getName(), new StartingEntryHandler(clientInfo));
	}
}
