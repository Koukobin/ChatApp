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
package github.chatapp.common.message_types;

/**
 * @author Ilias Koukovinis
 *
 */
public enum ClientCommandType {

	// Account Management 
	CHANGE_USERNAME(CommandLevel.HEAVY),
	CHANGE_PASSWORD(CommandLevel.HEAVY),
	LOGOUT(CommandLevel.LIGHT),

	// User Information Requests
	FETCH_USERNAME(CommandLevel.LIGHT),
	FETCH_CLIENT_ID(CommandLevel.LIGHT),

	// Chat Management
	FETCH_CHAT_REQUESTS(CommandLevel.LIGHT),
	FETCH_CHAT_SESSIONS(CommandLevel.LIGHT),
	SEND_CHAT_REQUEST(CommandLevel.HEAVY),
	ACCEPT_CHAT_REQUEST(CommandLevel.HEAVY),
	DECLINE_CHAT_REQUEST(CommandLevel.HEAVY),
	DELETE_CHAT_SESSION(CommandLevel.HEAVY),
	DELETE_CHAT_MESSAGE(CommandLevel.HEAVY),
	FETCH_WRITTEN_TEXT(CommandLevel.HEAVY),
	DOWNLOAD_FILE(CommandLevel.HEAVY),

	// External Pages
	REQUEST_DONATION_PAGE(CommandLevel.LIGHT),
	REQUEST_SOURCE_CODE_PAGE(CommandLevel.LIGHT);

	
	/*
	 * This enum determines whether the command requested by the client is "Heavy" or "Light."
	 * 
	 * - "Heavy" commands, as the name suggests, require more server resources to process.
	 * - "Light" commands, on the other hand, are less resource-intensive and quicker for the server to handle.
	 * 
	 * Classifying commands in such a way may seem unnecessary at first glance, however, this allows the server 
	 * to put a delay in how many "Heavy" commands a client can submit within a given time frame which helps shield 
	 * the server from potential spam attacks.
	 */
	public enum CommandLevel {
		LIGHT, HEAVY
	}
	
	private final CommandLevel commandLevel;
	
	ClientCommandType(CommandLevel commandLevel) {
		this.commandLevel = commandLevel;
	}

	public CommandLevel getCommandLevel() {
		return commandLevel;
	}
}
