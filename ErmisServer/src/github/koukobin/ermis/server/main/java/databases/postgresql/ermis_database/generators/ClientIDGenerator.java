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

import github.koukobin.ermis.server.main.java.configs.DatabaseSettings;

/**
 * @author Ilias Koukovinis
 *
 */
public final class ClientIDGenerator {
	
	private static final Logger logger = LogManager.getLogger("database");

	private static final Object lockObjectForGeneratingOrRetrievingClientIDS = new Object();
	
	private static final int AMOUNT_OF_CLIENT_IDS_TO_GENERATE_IN_EACH_GENERATION = 5000;
	private static final Deque<Integer> clientIDS = new ArrayDeque<>();

	private ClientIDGenerator() {}
	
	/**
	 * 
	 * @param conn Connection to ermis database
	 */
	public static void generateAvailableClientIDS(Connection connToDatabase) {
		try (Statement stmt = connToDatabase.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			
			ResultSet rs = stmt.executeQuery("SELECT client_id FROM users;");
			
			// <GET USER COUNT>
			rs.last();
			int userCount = rs.getRow();
			rs.first();
			// </GET USER COUNT>
			
			if (userCount == DatabaseSettings.MAX_USERS) {
				return;
			}
			
			int[] clientIDSToExclude = new int[userCount];
			for (int i = 0; i < clientIDSToExclude.length; i++, rs.next()) {
				clientIDSToExclude[i] = rs.getInt(1);
			}

			List<Integer> availableClientIDS = new ArrayList<>(AMOUNT_OF_CLIENT_IDS_TO_GENERATE_IN_EACH_GENERATION);

			for (int clientID = 0; clientID < DatabaseSettings.MAX_USERS; clientID++) {
				
				boolean isAlreadyUsed = false;
				
				for (int j = 0; j < clientIDSToExclude.length; j++) {
					if (clientID == clientIDSToExclude[j]) {
						isAlreadyUsed = true;
					}
				}
				
				if (isAlreadyUsed) {
					continue;
				}

				availableClientIDS.add(clientID);

				if (AMOUNT_OF_CLIENT_IDS_TO_GENERATE_IN_EACH_GENERATION == availableClientIDS.size()) {
					break;
				}
			}

			Collections.shuffle(availableClientIDS);
			
			for (int j = 0; j < availableClientIDS.size(); j++) {
				clientIDS.push(availableClientIDS.get(j));
			}
		} catch (SQLException sqle) {
			logger.fatal(Throwables.getStackTraceAsString(sqle));
		}
	}
	
	/**
	 * @return an available client id. If no client id is available then it returns -1
	 */
	public static int retrieveAndDelete(Connection conn) {
		synchronized (lockObjectForGeneratingOrRetrievingClientIDS) {

			if (clientIDS.isEmpty()) {

				generateAvailableClientIDS(conn);
				
				if (clientIDS.isEmpty()) {
					return -1;
				}
			}

			return clientIDS.pollLast();
		}
	}
	
}
