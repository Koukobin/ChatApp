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
import java.util.EnumMap;
import java.util.Map;

import github.koukobin.ermis.common.entry.CreateAccountInfo;
import github.koukobin.ermis.common.entry.CreateAccountInfo.Credential;
import github.koukobin.ermis.common.results.ResultHolder;
import github.koukobin.ermis.server.main.java.configs.ServerSettings;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.ErmisDatabase;
import github.koukobin.ermis.server.main.java.server.ClientInfo;
import github.koukobin.ermis.server.main.java.server.util.EmailerService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Ilias Koukovinis
 * 
 */
final class CreateAccountHandler extends EntryHandler {

	private Map<Credential, String> credentials = new EnumMap<>(Credential.class);
	
	CreateAccountHandler(ClientInfo clientInfo) {
		super(clientInfo);
	}
	
	@Override
	public void doEntryAction(ChannelHandlerContext ctx, ByteBuf msg) {
		// Do nothing
	}
	
	@Override
	public void channelRead2(ChannelHandlerContext ctx, ByteBuf msg) throws IOException {
		
		{
			Credential credential = Credential.fromId(msg.readInt());

			byte[] payloadBytes = new byte[msg.readableBytes()];
			msg.readBytes(payloadBytes);

			credentials.put(credential, new String(payloadBytes));
		}
		
		if (credentials.size() == Credential.values().length) {
			
			String username = credentials.get(Credential.USERNAME);
			String password = credentials.get(Credential.PASSWORD);
			String email = credentials.get(Credential.EMAIL);
			
			ResultHolder resultHolder;

			if (EmailerService.isValidEmailAddress(email)) {
				try (ErmisDatabase.GeneralPurposeDBConnection conn = ErmisDatabase.getGeneralPurposeConnection()) {
					resultHolder = conn.checkIfUserMeetsRequirementsToCreateAccount(username, password, email);
				}
			} else {
				resultHolder = CreateAccountInfo.CredentialValidation.Result.INVALID_EMAIL_ADDRESS.resultHolder;
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
				
				VerificationHandler verificationHandler = new VerificationHandler(
						clientInfo,
						email) {

					@Override
					public ResultHolder executeWhenVerificationSuccesfull() {
						
						String username = credentials.get(Credential.USERNAME);
						String password = credentials.get(Credential.PASSWORD);

						try (ErmisDatabase.GeneralPurposeDBConnection conn = ErmisDatabase.getGeneralPurposeConnection()) {
							return conn.createAccount(username, password, clientInfo.getChannel().remoteAddress().getAddress(), email);
						}
					}

					@Override
					public String createEmailMessage(String account, String generatedVerificationCode) {
						return ServerSettings.EmailCreator.Verification.CreateAccount.createEmail(email, account, generatedVerificationCode);
					}
				};

				ctx.pipeline().replace(CreateAccountHandler.this, VerificationHandler.class.getName(), verificationHandler);
			}
			
		};
	}
}
