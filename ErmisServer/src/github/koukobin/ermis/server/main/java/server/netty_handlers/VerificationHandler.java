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
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import javax.mail.MessagingException;

import com.google.common.base.Throwables;

import github.koukobin.ermis.common.entry.Verification.Action;
import github.koukobin.ermis.common.entry.Verification.Result;
import github.koukobin.ermis.common.results.ResultHolder;
import github.koukobin.ermis.server.main.java.server.ClientInfo;
import github.koukobin.ermis.server.main.java.server.util.EmailerService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Ilias Koukovinis
 * 
 */
abstract non-sealed class VerificationHandler extends EntryHandler {

	private static final SecureRandom secureRandom = new SecureRandom();

	private static final int ATTEMPTS = 3;
	private static final int GENERATED_VERIFICATIIN_CODE_LENGTH = 9;
	
	private static final CompletableFuture<?> pendingEmailsQueue = 
			CompletableFuture.runAsync(() -> {}); // initialize it like this for thenRunAsync to work (i don't know why)

	private int attemptsRemaining = ATTEMPTS;
	private final byte[] generatedVerificationCode;
	
	private final String emailAddress;
	
	{
		generatedVerificationCode = new byte[GENERATED_VERIFICATIIN_CODE_LENGTH];
		secureRandom.nextBytes(generatedVerificationCode);
	}
	
	VerificationHandler(ClientInfo clientInfo, String email) {
		super(clientInfo);
		
		this.emailAddress = email;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		sendVerificationCode();
	}
	
	private void sendVerificationCode() {
		String generatedVerificationCodeEncoded = Base64.getEncoder().encodeToString(generatedVerificationCode);
		pendingEmailsQueue.thenRunAsync(() -> {
			try {
				EmailerService.sendEmailWithHTML("Security Alert", createEmailMessage(clientInfo.toString(), generatedVerificationCodeEncoded), emailAddress);
			} catch (MessagingException me) {
				logger.debug(Throwables.getStackTraceAsString(me));
			}
		});
	}
	
	@Override
	public void doEntryAction(ChannelHandlerContext ctx, ByteBuf msg) {
		
		Action action = Action.fromId(msg.readInt());

		switch (action) {
		case RESEND_CODE -> {
			sendVerificationCode();
		}
		}
	}
	
	@Override
	public void channelRead2(ChannelHandlerContext ctx, ByteBuf msg) throws IOException {
		
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
			if (entryResult.isSuccessful()) {
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
		payload.writeBoolean(entryResult.isSuccessful());
		payload.writeBytes(resultMessageBytes);
		
		ctx.channel().writeAndFlush(payload);
	}

	public abstract String createEmailMessage(String account, String generatedVerificationCode);
	public abstract ResultHolder executeWhenVerificationSuccesfull() throws IOException;
	
	protected Runnable onSuccess(ChannelHandlerContext ctx) {
		return () ->  login(ctx, clientInfo);
	}
}
