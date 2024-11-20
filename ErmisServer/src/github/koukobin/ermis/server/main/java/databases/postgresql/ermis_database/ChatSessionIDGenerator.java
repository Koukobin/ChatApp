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
package github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;

/**
 * @author Ilias Koukovinis
 *
 */
final class ChatSessionIDGenerator {

	private static final Logger logger = LogManager.getLogger("database");

	private static final Object lockObjectForGeneratingOrRetrievingChatSessionIDS = new Object();
	
	private static final int AMOUNT_OF_CHAT_SESSION_IDS_TO_GENERATE_IN_EACH_GENERATION = 10_000;
	private static final Deque<Integer> chatSessionIDS = new ArrayDeque<>();
	
	private ChatSessionIDGenerator() {}
	
	/**
	 * 
	 * @param conn Connection to ermis database
	 */
	public static void generateAvailableChatSessionIDS(Connection conn) {
		try (Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			
			ResultSet rs = stmt.executeQuery("SELECT chat_session_id FROM chat_sessions;");

			// <GET CHAT SESSION COUNT>
			rs.last();
			int chatSessionsCount = rs.getRow();
			rs.first();
			// </GET CHAT SESSION COUNT>
			
			int[] chatSessionIDSToExclude = new int[chatSessionsCount];
			for (int i = 0; i < chatSessionIDSToExclude.length; i++, rs.next()) {
				chatSessionIDSToExclude[i] = rs.getInt(1);
			}

			List<Integer> availableChatSessionIDS = new ArrayList<>(AMOUNT_OF_CHAT_SESSION_IDS_TO_GENERATE_IN_EACH_GENERATION);

			for (int chatSessionID = 0; AMOUNT_OF_CHAT_SESSION_IDS_TO_GENERATE_IN_EACH_GENERATION != availableChatSessionIDS.size(); chatSessionID++) {

				boolean isAlreadyUsed = false;

				for (int j = 0; j < chatSessionIDSToExclude.length; j++) {
					if (chatSessionID == chatSessionIDSToExclude[j]) {
						isAlreadyUsed = true;
					}
				}

				if (isAlreadyUsed) {
					continue;
				}

				availableChatSessionIDS.add(chatSessionID);
			}
			
			Collections.shuffle(availableChatSessionIDS);
			
			for (int j = 0; j < availableChatSessionIDS.size(); j++) {
				chatSessionIDS.push(availableChatSessionIDS.get(j));
			}
		} catch (SQLException sqle) {
			logger.fatal(Throwables.getStackTraceAsString(sqle));
		}
	}

	public static int retrieveAndDelete(Connection conn) {
		synchronized (lockObjectForGeneratingOrRetrievingChatSessionIDS) {

			if (chatSessionIDS.isEmpty()) {
				generateAvailableChatSessionIDS(conn);
			}

			return chatSessionIDS.pollLast();
		}
	}
	
}
