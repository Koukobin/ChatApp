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
package github.chatapp.client.main.java.server.netty_handlers;

import github.chatapp.client.main.java.server.ClientInfo;
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

	public abstract void doEntryAction(ByteBuf msg) throws Exception;
	public abstract void channelRead(ChannelHandlerContext ctx, ByteBuf msg) throws Exception;

	@Override
	public final void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

		boolean isAction = msg.readBoolean();
		
		if (isAction) {
			doEntryAction(msg);
		} else {
			channelRead(ctx, msg);
		}
	}
	
	protected abstract Runnable onSuccess(ChannelHandlerContext ctx);

	protected void success(ChannelHandlerContext ctx) {
		resultRunnable = onSuccess(ctx);
		isSuccesfull = true;
	}

	public void failed(ChannelHandlerContext ctx) {
		resultRunnable = () -> failLogin(ctx, clientInfo);
		hasFailed = true;
	}
	
	public static void login(ChannelHandlerContext ctx, ClientInfo clientInfo) {
		ctx.pipeline().replace(ctx.handler(), MessageHadler.class.getName(), new MessageHadler(clientInfo));
	}

	public static void failLogin(ChannelHandlerContext ctx, ClientInfo clientInfo) {
		ctx.pipeline().replace(ctx.handler(), StartingEntryHandler.class.getName(), new StartingEntryHandler(clientInfo));
	}
}
