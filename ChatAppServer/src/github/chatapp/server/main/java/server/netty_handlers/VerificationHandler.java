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

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.mail.MessagingException;

import github.chatapp.common.entry.Verification.Action;
import github.chatapp.common.entry.Verification.Result;
import github.chatapp.common.results.ResultHolder;
import github.chatapp.common.util.EnumIntConverter;
import github.chatapp.server.main.java.server.ClientInfo;
import github.chatapp.server.main.java.server.EmailerService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Ilias Koukovinis
 * 
 */
abstract non-sealed class VerificationHandler extends EntryHandler {

	private static final SecureRandom secureRandom = new SecureRandom();

	private static final int ATTEMPTS = 3;
	private int attemptsRemaining = ATTEMPTS;
	
	private static final int generatedVerificationCodeLength = 9;
	private final byte[] generatedVerificationCode;
	
	private final String emailAddress;
	
	{
		generatedVerificationCode = new byte[generatedVerificationCodeLength];
		secureRandom.nextBytes(generatedVerificationCode);
	}
	
	VerificationHandler(ClientInfo clientInfo, String email) {
		super(clientInfo);
		
		this.emailAddress = email;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws MessagingException {
		sendVerificationCode();
	}
	
	private void sendVerificationCode() throws MessagingException {
		String generatedVerificationCodeEncoded = Base64.getEncoder().encodeToString(generatedVerificationCode);
		EmailerService.sendEmail("Security Alert", createEmailMessage(generatedVerificationCodeEncoded), emailAddress);
	}
	
	@Override
	public void doEntryAction(ByteBuf msg) throws MessagingException {
		
		Action action = EnumIntConverter.getIntAsEnum(msg.readInt(), Action.class);

		switch (action) {
		case RESEND_CODE -> {
			sendVerificationCode();
		}
		}
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		
		ResultHolder entryResult = Result.WRONG_CODE.resultHolder;
		attemptsRemaining--;
		
		boolean isVerificationComplete = false;
		
		try {

			byte[] clientGuessForVerificationCode = new byte[msg.readableBytes()];
			msg.readBytes(clientGuessForVerificationCode);

			// Throws IllegalArgumentException if src is not in valid Base64 scheme
			clientGuessForVerificationCode = Base64.getDecoder().decode(clientGuessForVerificationCode);

			if (Arrays.equals(generatedVerificationCode, clientGuessForVerificationCode)) {
				entryResult = executeWhenVerificationSuccesfull();
				isVerificationComplete = true;
			}
		} catch (IllegalArgumentException iae) {
			// Do nothing.
		}

		if (isVerificationComplete) {
			if (entryResult.isSuccesfull()) {
				success(ctx);
			} else {
				failed(ctx);
			}
		} else {
			if (attemptsRemaining == 0) {
				entryResult = Result.RUN_OUT_OF_ATTEMPTS.resultHolder;
				failed(ctx);
				isVerificationComplete = true;
			}
		}

		byte[] resultMessageBytes = entryResult.getResultMessage().getBytes();
		
		ByteBuf payload = ctx.alloc().ioBuffer();
		payload.writeBoolean(isVerificationComplete);
		payload.writeBoolean(entryResult.isSuccesfull());
		payload.writeBytes(resultMessageBytes);
		
		ctx.channel().writeAndFlush(payload);
	}

	public abstract String createEmailMessage(String generatedVerificationCode);
	public abstract ResultHolder executeWhenVerificationSuccesfull() throws Exception;
	
	protected Runnable onSuccess(ChannelHandlerContext ctx) {
		return () ->  login(ctx, clientInfo);
	}
}
