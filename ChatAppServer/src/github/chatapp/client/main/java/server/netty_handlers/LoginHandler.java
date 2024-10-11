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

import java.util.EnumMap;
import java.util.Map;

import github.chatapp.client.main.java.configs.ServerSettings;
import github.chatapp.client.main.java.databases.postgresql.chatapp_database.ChatAppDatabase;
import github.chatapp.client.main.java.server.ClientInfo;
import github.chatapp.common.entry.LoginInfo.Action;
import github.chatapp.common.entry.LoginInfo.Credential;
import github.chatapp.common.entry.LoginInfo.PasswordType;
import github.chatapp.common.reults.ResultHolder;
import github.chatapp.common.util.EnumIntConverter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Ilias Koukovinis
 * 
 */
final class LoginHandler extends EntryHandler {

	private PasswordType passwordType = PasswordType.PASSWORD;
	
	private Map<Credential, String> credentials = new EnumMap<>(Credential.class);
	
	LoginHandler(ClientInfo clientInfo) {
		super(clientInfo);
	}
	
	@Override
	public void doEntryAction(ByteBuf msg) {

		int readerIndex = msg.readerIndex();

		Action action = EnumIntConverter.getIntAsEnum(msg.readInt(), Action.class);

		switch (action) {
		case TOGGLE_PASSWORD_TYPE -> {
			
			passwordType = switch (passwordType) {
			case PASSWORD -> PasswordType.BACKUP_VERIFICATION_CODE;
			case BACKUP_VERIFICATION_CODE -> PasswordType.PASSWORD;
			};
			
		}
		}

		msg.readerIndex(readerIndex);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, ByteBuf msg) {
		
		{
			Credential credential = EnumIntConverter.getIntAsEnum(msg.readInt(), Credential.class);
			
			byte[] msgBytes = new byte[msg.readableBytes()];
			msg.readBytes(msgBytes);
			
			credentials.put(credential, new String(msgBytes));
		}

		if (credentials.size() == Credential.values().length) {

			String email = credentials.get(Credential.EMAIL);

			ResultHolder resultHolder;
			try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
				resultHolder = conn.checkIfUserMeetsRequirementsToLogin(email);
			}

			ByteBuf payload = ctx.alloc().ioBuffer();
			payload.writeBoolean(resultHolder.isSuccesfull());

			if (resultHolder.isSuccesfull()) {
				success(ctx);
			} else {
				failed(ctx);
			}
			
			byte[] resultMessageBytes = resultHolder.getResultMessage().getBytes();
			payload.writeBytes(resultMessageBytes);
			
			ctx.channel().writeAndFlush(payload);
		}
	}

	@Override
	protected Runnable onSuccess(ChannelHandlerContext ctx) {
		return new Runnable() {

			@Override
			public void run() {

				String email = credentials.get(Credential.EMAIL);
				String password = credentials.get(Credential.PASSWORD);

				switch (passwordType) {
				case BACKUP_VERIFICATION_CODE -> {
					
					ResultHolder entryResult;
					
					try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
						entryResult = conn.loginUsingBackupVerificationCode(clientInfo.getChannel().remoteAddress().getAddress(), email, password);
					}
					
					if (entryResult.isSuccesfull()) {
						login(ctx, clientInfo);
					} else {
						failLogin(ctx, clientInfo);
					}
					
					byte[] resultMessageBytes = entryResult.getResultMessage().getBytes();
					
					ByteBuf payload = ctx.alloc().ioBuffer();
					payload.writeBoolean(entryResult.isSuccesfull());
					payload.writeBytes(resultMessageBytes);
					
					ctx.channel().writeAndFlush(payload);
				}
				case PASSWORD -> {
					VerificationHandler verificationHandler = new VerificationHandler(
							clientInfo,
							email) {
						
						@Override
						public ResultHolder executeWhenVerificationSuccesfull() {
							try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
								return conn.loginUsingPassword(clientInfo.getChannel().remoteAddress().getAddress(), email, password);
							}
						}
						
						@Override
						public String createEmailMessage(String generatedVerificationCode) {
							return ServerSettings.EmailCreator.Verification.Login.createEmail(email, generatedVerificationCode);
						}
					};
					
					ctx.pipeline().replace(LoginHandler.this, VerificationHandler.class.getName(), verificationHandler);
				}
				}
				
			}
		};
	}
}