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
package github.koukobin.ermis.server.main.java.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ilias Koukovinis
 *
 */
public class ActiveChatSessions {

	private static final Map<Integer, ChatSession> chatSessionIDSToActiveChatSessions = new ConcurrentHashMap<>(100);

	private ActiveChatSessions() {}
	
	public static ChatSession getChatSession(int chatSessionID) {
		return chatSessionIDSToActiveChatSessions.get(chatSessionID);
	}
	
	public static void addChatSession(int chatSessionID, ChatSession chatSession) {
		chatSessionIDSToActiveChatSessions.put(chatSessionID, chatSession);
	}
	
	public static void removeChatSession(int chatSessionID) {
		chatSessionIDSToActiveChatSessions.remove(chatSessionID);
	}

	public static void addMember(int chatSessionID, ClientInfo member) {
		chatSessionIDSToActiveChatSessions.get(chatSessionID).getActiveMembers().add(member);
	}
	
	public static void removeMember(int chatSessionID, ClientInfo member) {
		chatSessionIDSToActiveChatSessions.get(chatSessionID).getActiveMembers().remove(member);
	}
}
