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
package github.chatapp.client.main.java.databases;

import java.net.MalformedURLException;
import java.net.PortUnreachableException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import github.chatapp.client.main.java.info.GeneralAppInfo;

/**
 * @author Ilias Koukovinis
 *
 */
public class ClientDatabase {

	private ClientDatabase() throws IllegalAccessException {
		throw new IllegalAccessException("Database cannot be constructed since it is statically initialized!");
	}

	public static DBConnection createDBConnection() {
		return new DBConnection();
	}
	
	public static class DBConnection implements AutoCloseable {
		
		private static final String JDBC_URL;

		static { 
			JDBC_URL = "jdbc:sqlite:" + GeneralAppInfo.CLIENT_DATABASE_PATH;
		}

		private final Connection conn;
		
		public DBConnection() {
			try {
				conn = createConnection();
			} catch (SQLException sqle) {
				throw new RuntimeException(sqle);
			}
		}
		
		private static Connection createConnection() throws SQLException {
			
			Connection conn = DriverManager.getConnection(JDBC_URL);

			try (Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE IF NOT EXISTS server_info (server_url TEXT NOT NULL, PRIMARY KEY(server_url));");
			}
			
			return conn;
		}
		
		public int addServerInfo(ServerInfo serverInfo) {
			
			int resultUpdate = 0;

			try (PreparedStatement addServerInfo = conn.prepareStatement("INSERT INTO server_info (server_url) VALUES(?);")) {
				
				addServerInfo.setString(1, serverInfo.getURL().toString());
				resultUpdate = addServerInfo.executeUpdate();
			} catch (SQLException sqle) {
				sqle.printStackTrace();
			}
			
			return resultUpdate;
		}
		
		public int removeServerInfo(ServerInfo serverInfo) {
			
			int resultUpdate = 0;

			try (PreparedStatement addServerInfo = conn.prepareStatement("DELETE FROM server_info WHERE server_url=?;")) {
				
				addServerInfo.setString(1, serverInfo.getURL().toString());
				resultUpdate = addServerInfo.executeUpdate();
			} catch (SQLException sqle) {
				sqle.printStackTrace();
			}
			
			return resultUpdate;
		}
		
		public int setServerInfo(ServerInfo serverInfo) {
			
			int resultUpdate = 0;

			try (PreparedStatement addServerInfo = conn.prepareStatement("UPDATE server_info SET server_url=?;")) {
				
				addServerInfo.setString(1, serverInfo.getURL().toString());
				
				resultUpdate = addServerInfo.executeUpdate();
			} catch (SQLException sqle) {
				sqle.printStackTrace();
			}
			
			return resultUpdate;
		}
		
		public ServerInfo[] getServerInfos() {

			ArrayList<ServerInfo> serverInfos = new ArrayList<>();
			
			try (PreparedStatement addServerInfo = conn.prepareStatement("SELECT * FROM server_info;")) {
				
				ResultSet rs = addServerInfo.executeQuery();
				
				while (rs.next()) {
					String serverURL = rs.getString(1);
					serverInfos.add(new ServerInfo(new URL(serverURL)));
				}
			} catch (SQLException | UnknownHostException | PortUnreachableException | MalformedURLException e) {
				e.printStackTrace();
			}
			
			return serverInfos.toArray(new ServerInfo[] {});
		}

		@Override
		public void close() {
			try {
				conn.close();
			} catch (SQLException sqle) {
				throw new RuntimeException(sqle);
			}
		}
	}
}
