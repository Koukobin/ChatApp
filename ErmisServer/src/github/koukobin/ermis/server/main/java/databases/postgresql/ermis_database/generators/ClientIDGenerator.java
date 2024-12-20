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
package github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.generators;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;

/**
 * @author Ilias Koukovinis
 *
 */
public final class ClientIDGenerator {
	
	private static final Logger logger = LogManager.getLogger("database");

	private static final Object lockObjectForGeneratingOrRetrievingClientIDS = new Object();
	
	private static final int IDS_PER_GENERATION = 5_000;
	private static final Deque<Integer> clientIDS = new ConcurrentLinkedDeque<>();

	private ClientIDGenerator() {}
	
	/**
	 * 
	 * @param conn Connection to ermis database
	 */
	public static void generateAvailableClientIDS(Connection connToDatabase) {
		synchronized (lockObjectForGeneratingOrRetrievingClientIDS) {
			try (PreparedStatement pstmt = connToDatabase.prepareStatement(
					"SELECT client_id FROM users;",
					ResultSet.TYPE_SCROLL_SENSITIVE, 
					ResultSet.CONCUR_UPDATABLE)) {
				
				ResultSet rs = pstmt.executeQuery();

				Set<Integer> usedIDs = new HashSet<>();
				while (rs.next()) {
					usedIDs.add(rs.getInt(1));
				}

				List<Integer> availableIDs = new ArrayList<>(IDS_PER_GENERATION);
				for (int id = 0; availableIDs.size() < IDS_PER_GENERATION; id++) {
					if (!usedIDs.contains(id)) {
						availableIDs.add(id);
					}
				}

				Collections.shuffle(availableIDs);
				clientIDS.addAll(availableIDs);
			} catch (SQLException sqle) {
				logger.fatal(Throwables.getStackTraceAsString(sqle));
				throw new RuntimeException(sqle);
			}
		}
	}
	
	public static int retrieveAndDelete(Connection conn) {
		int id = retrieve(conn);
	    if (id != -1) {
	        delete(id);
	    }
	    return id;
	}
	
	public static void undo(int chatSessionID) {
		clientIDS.add(chatSessionID);
	}
	
	private static int retrieve(Connection conn) {
	    if (clientIDS.isEmpty()) {
	        generateAvailableClientIDS(conn);

	        if (clientIDS.isEmpty()) {
	            logger.warn("Failed to retrieve a client ID; no IDs available.");
	            return -1;
	        }
	    }

	    return clientIDS.peekLast();
	}
	
	private static void delete(Integer chatSessionID) {
		boolean removed = clientIDS.remove(chatSessionID);
		if (!removed) {
			logger.warn("Attempted to delete a non-existent client ID: {}", chatSessionID);
		}
	}
	
}
