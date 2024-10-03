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
package main.java.databases.postgresql.chatapp_database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Ilias Koukovinis
 *
 */
final class MessageIDGenerator {

	private static final Logger logger = LogManager.getLogger("database");
	
	private static final Object lockObjectForGettingMessageIDCountAsAtomicInteger = new Object();
	private static final Map<Integer, AtomicInteger> chatSessionIDSToMessageIDValue = new TreeMap<>();

	private MessageIDGenerator() {}
	
	private static AtomicInteger getMessageIDAsAtomicInteger(int chatSessionID, Connection conn) {
		
		AtomicInteger messageID = chatSessionIDSToMessageIDValue.get(chatSessionID);
		
		// Check if messageID is null
		if (messageID == null) {
			
			// Synchronize so not multiple threads can try run getMessageIDCountAsAtomicInteger
			synchronized (lockObjectForGettingMessageIDCountAsAtomicInteger) {
				
				messageID = chatSessionIDSToMessageIDValue.get(chatSessionID);

				// Check again to see if it is null as this might have already been run by another thread
				if (messageID == null) {
					try (PreparedStatement getLastMessageID = conn
							.prepareStatement("SELECT message_id FROM chat_messages WHERE chat_session_id=? ORDER BY message_id DESC LIMIT 1")) {

						getLastMessageID.setInt(1, chatSessionID);
						ResultSet rs = getLastMessageID.executeQuery();
						
						if (rs.next()) {
							messageID = new AtomicInteger(rs.getInt(1));
						} else {
							messageID = new AtomicInteger(0);
						}
						
						chatSessionIDSToMessageIDValue.put(chatSessionID, messageID);
						
						return messageID;
					} catch (SQLException sqle) {
						throw new RuntimeException(sqle);
					}
				}
				
			}
		}
		
		return messageID;
	}
	
	public static int incrementAndGetMessageID(Integer chatSessionID, Connection conn) {

		AtomicInteger messageID = getMessageIDAsAtomicInteger(chatSessionID, conn);

		return messageID.incrementAndGet();
	}

	
	public static int getMessageIDCount(Integer chatSessionID, Connection conn) {

		AtomicInteger messageID = getMessageIDAsAtomicInteger(chatSessionID, conn);

		return messageID.intValue();
	}
	
}
