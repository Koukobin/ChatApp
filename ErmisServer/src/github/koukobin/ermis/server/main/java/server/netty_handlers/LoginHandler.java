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

import java.util.EnumMap;
import java.util.Map;

import github.koukobin.ermis.common.entry.LoginInfo.Action;
import github.koukobin.ermis.common.entry.LoginInfo.Credential;
import github.koukobin.ermis.common.entry.LoginInfo.PasswordType;
import github.koukobin.ermis.common.results.EntryResult;
import github.koukobin.ermis.common.results.ResultHolder;
import github.koukobin.ermis.server.main.java.configs.ServerSettings;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.ErmisDatabase;
import github.koukobin.ermis.server.main.java.server.ClientInfo;
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
	public void executeEntryAction(ChannelHandlerContext ctx, ByteBuf msg) {

		int readerIndex = msg.readerIndex();

		Action action = Action.fromId(msg.readInt());

		switch (action) {
		case TOGGLE_PASSWORD_TYPE:
			passwordType = switch (passwordType) {
			case PASSWORD -> PasswordType.BACKUP_VERIFICATION_CODE;
			case BACKUP_VERIFICATION_CODE -> PasswordType.PASSWORD;
			};
			break;
		}

		msg.readerIndex(readerIndex);
	}
	
	@Override
	public void channelRead2(ChannelHandlerContext ctx, ByteBuf msg) {
		
		{
			Credential credential = Credential.fromId(msg.readInt());
			
			byte[] msgBytes = new byte[msg.readableBytes()];
			msg.readBytes(msgBytes);
			
			credentials.put(credential, new String(msgBytes));
		}

		if (credentials.size() == Credential.values().length) {

			String email = credentials.get(Credential.EMAIL);

			ResultHolder resultHolder;
			try (ErmisDatabase.GeneralPurposeDBConnection conn = ErmisDatabase.getGeneralPurposeConnection()) {
				resultHolder = conn.checkIfUserMeetsRequirementsToLogin(email);
			}
			
			ByteBuf payload = ctx.alloc().ioBuffer();
			payload.writeBoolean(resultHolder.isSuccessful());

			if (resultHolder.isSuccessful()) {
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
					
					try (ErmisDatabase.GeneralPurposeDBConnection conn = ErmisDatabase.getGeneralPurposeConnection()) {
						entryResult = conn.loginUsingBackupVerificationCode(clientInfo.getChannel().remoteAddress().getAddress(), email, password);
					}
					
					if (entryResult.isSuccessful()) {
						login(ctx, clientInfo);
					} else {
						registrationFailed(ctx, clientInfo);
					}
					
					byte[] resultMessageBytes = entryResult.getResultMessage().getBytes();
					
					ByteBuf payload = ctx.alloc().ioBuffer();
					payload.writeBoolean(entryResult.isSuccessful());
					payload.writeBytes(resultMessageBytes);
					
					ctx.channel().writeAndFlush(payload);
				}
				case PASSWORD -> {
					VerificationHandler verificationHandler = new VerificationHandler(
							clientInfo,
							email) {
						
						@Override
						public EntryResult executeWhenVerificationSuccesfull() {
							try (ErmisDatabase.GeneralPurposeDBConnection conn = ErmisDatabase.getGeneralPurposeConnection()) {
								return conn.loginUsingPassword(clientInfo.getChannel().remoteAddress().getAddress(), email, password);
							}
						}
						
						@Override
						public String createEmailMessage(String account, String generatedVerificationCode) {
							return ServerSettings.EmailCreator.Verification.Login.createEmail(email, account, generatedVerificationCode);
						}
					};
					
					ctx.pipeline().replace(LoginHandler.this, VerificationHandler.class.getName(), verificationHandler);
				}
				}
				
			}
		};
	}
}