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
package github.koukobin.ermis.common.message_types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import github.koukobin.ermis.common.util.EnumIntConverter;

/**
 * @author Ilias Koukovinis
 *
 */
public enum ClientCommandType {

	// Account Management 
	CHANGE_USERNAME(CommandLevel.HEAVY, 0),
	CHANGE_PASSWORD(CommandLevel.HEAVY, 1),
	ADD_ACCOUNT_ICON(CommandLevel.HEAVY, 2),
	LOGOUT(CommandLevel.LIGHT, 3),

	// User Information Requests
	FETCH_USERNAME(CommandLevel.LIGHT, 4),
	FETCH_CLIENT_ID(CommandLevel.LIGHT, 5),
	FETCH_ACCOUNT_ICON(CommandLevel.HEAVY, 6),

	// Chat Management
	FETCH_CHAT_REQUESTS(CommandLevel.LIGHT, 7),
	FETCH_CHAT_SESSIONS(CommandLevel.LIGHT, 8),
	SEND_CHAT_REQUEST(CommandLevel.HEAVY, 9),
	ACCEPT_CHAT_REQUEST(CommandLevel.HEAVY, 10),
	DECLINE_CHAT_REQUEST(CommandLevel.HEAVY, 11),
	DELETE_CHAT_SESSION(CommandLevel.HEAVY, 12),
	DELETE_CHAT_MESSAGE(CommandLevel.HEAVY, 13),
	FETCH_WRITTEN_TEXT(CommandLevel.HEAVY, 14),
	DOWNLOAD_FILE(CommandLevel.HEAVY, 15),

	// External Pages
	REQUEST_DONATION_PAGE(CommandLevel.LIGHT, 16),
	REQUEST_SOURCE_CODE_PAGE(CommandLevel.LIGHT, 17);

	
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
	
	private static final HashMap<Integer, ClientCommandType> values;
	
	static {
		values = new HashMap<>(
				Arrays.stream(ClientCommandType.values())
				.collect(Collectors.toMap(type -> type.id, type -> type))
				);
	}
	
	private final CommandLevel commandLevel;
	public final int id;
	
	ClientCommandType(CommandLevel commandLevel, int id) {
		this.commandLevel = commandLevel;
		this.id = id;
	}

	public CommandLevel getCommandLevel() {
		return commandLevel;
	}
	
	public static ClientCommandType fromId(int id) {
		return EnumIntConverter.fromId(values, id);
	}
}
