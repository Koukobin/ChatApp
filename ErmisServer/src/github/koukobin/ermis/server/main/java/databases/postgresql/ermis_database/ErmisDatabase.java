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
 
import java.net.InetAddress;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;
import com.zaxxer.hikari.HikariDataSource;

import github.koukobin.ermis.common.DeviceType;
import github.koukobin.ermis.common.LoadedInMemoryFile;
import github.koukobin.ermis.common.UserDeviceInfo;
import github.koukobin.ermis.common.entry.AddedInfo;
import github.koukobin.ermis.common.entry.CreateAccountInfo;
import github.koukobin.ermis.common.entry.LoginInfo;
import github.koukobin.ermis.common.message_types.ContentType;
import github.koukobin.ermis.common.message_types.Message;
import github.koukobin.ermis.common.results.ChangePasswordResult;
import github.koukobin.ermis.common.results.ChangeUsernameResult;
import github.koukobin.ermis.common.results.EntryResult;
import github.koukobin.ermis.common.results.ResultHolder;
import github.koukobin.ermis.common.util.EmptyArrays;
import github.koukobin.ermis.common.util.FileEditor;
import github.koukobin.ermis.server.main.java.configs.ConfigurationsPaths.Database;
import github.koukobin.ermis.server.main.java.configs.DatabaseSettings;
import github.koukobin.ermis.server.main.java.databases.postgresql.PostgresqlDatabase;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.complexity_checker.PasswordComplexityChecker;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.complexity_checker.UsernameComplexityChecker;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.generators.BackupVerificationCodesGenerator;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.generators.ChatSessionIDGenerator;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.generators.ClientIDGenerator;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.generators.MessageIDGenerator;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.hashing.HashUtil;
import github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database.hashing.SimpleHash;

/**
 * @author Ilias Koukovinis
 *
 */
public final class ErmisDatabase {

	private static final Logger logger;
	private static final HikariDataSource generalPurposeDataSource;
	private static final HikariDataSource writeChatMessagesDataSource;

	private ErmisDatabase() throws IllegalAccessException {
		throw new IllegalAccessException("Database cannot be constructed since it is statically initialized!");
	}

	public static void initialize() {
		// Helper method to initialize class
	}
	
	static {
		logger = LogManager.getLogger("database");
	}
	
	static {
		try {
			
			generalPurposeDataSource = new PostgresqlDatabase.HikariDataSourceBuilder()
					.setUser(DatabaseSettings.USER)
					.setServerNames(DatabaseSettings.DATABASE_ADDRESS)
					.setDatabaseName(DatabaseSettings.DATABASE_NAME)
					.setUserPassword(DatabaseSettings.USER_PASSWORD)
					.setPortNumbers(DatabaseSettings.DATABASE_PORT)
					.addDriverProperties(DatabaseSettings.Driver.getDriverProperties())
					.setMinimumIdle(DatabaseSettings.ConnectionPool.GeneralPurposePool.MIN_IDLE)
					.setMaximumPoolSize(DatabaseSettings.ConnectionPool.GeneralPurposePool.MAX_POOL_SIZE)
					.setConnectionTimeout(0)
					.build();
			
			writeChatMessagesDataSource = new PostgresqlDatabase.HikariDataSourceBuilder()
					.setUser(DatabaseSettings.USER)
					.setServerNames(DatabaseSettings.DATABASE_ADDRESS)
					.setDatabaseName(DatabaseSettings.DATABASE_NAME)
					.setUserPassword(DatabaseSettings.USER_PASSWORD)
					.setPortNumbers(DatabaseSettings.DATABASE_PORT)
					.addDriverProperties(DatabaseSettings.Driver.getDriverProperties())
					.setMinimumIdle(DatabaseSettings.ConnectionPool.WriteChatMessagesPool.MIN_IDLE)
					.setMaximumPoolSize(DatabaseSettings.ConnectionPool.WriteChatMessagesPool.MAX_POOL_SIZE)
					.setConnectionTimeout(0)
					.build();

			try (Connection conn = generalPurposeDataSource.getConnection(); Statement stmt = conn.createStatement()) {

				// Create users table
				/*
				 * Since the hash and salt are encoded into base64 without padding we
				 * calculate the base64 encoded data size using the formula (size * 8 + 5) / 6
				 */
				int hashLength = (DatabaseSettings.Client.Password.Hashing.HASH_LENGTH * 8 + 5) / 6;
				int saltLength = (DatabaseSettings.Client.General.SaltForHashing.SALT_LENGTH * 8 + 5) / 6;
				int backupVerificationCodesCharactersLength = (DatabaseSettings.Client.BackupVerificationCodes.Hashing.HASH_LENGTH * 8 + 5) / 6;
				int usernameMaxLength = DatabaseSettings.Client.Username.REQUIREMENTS.getMaxLength();

				String setupSQL = FileEditor.readFile(Database.DATABASE_SETUP_FILE)
						.replace("DISPLAY_LENGTH", Integer.toString(usernameMaxLength))
						.replace("PASSWORD_HASH_LENGTH", Integer.toString(hashLength))
						.replace("BACKUP_VERIFICATION_CODES_AMOUNT", Integer.toString(DatabaseSettings.Client.BackupVerificationCodes.AMOUNT_OF_CODES))
						.replace("BACKUP_VERIFICATION_CODES_LENGTH", Integer.toString(backupVerificationCodesCharactersLength))
						.replace("SALT_LENGTH", Integer.toString(saltLength));

				stmt.execute(setupSQL);

				ChatSessionIDGenerator.generateAvailableChatSessionIDS(conn);
				ClientIDGenerator.generateAvailableClientIDS(conn);
			}
		} catch (Exception e) {
			logger.fatal(Throwables.getStackTraceAsString(e));
			throw new RuntimeException(e);
		}
	}

	public static GeneralPurposeDBConnection getGeneralPurposeConnection() {
		return new GeneralPurposeDBConnection();
	}

	public static WriteChatMessagesDBConnection getWriteChatMessagesConnection() {
		return new WriteChatMessagesDBConnection();
	}

	public enum Constant {
	    NOT_FOUND, SUCCESSFUL_INSERT, DUPLICATE_ENTRY, NOTHING_CHANGED;
	}
	
	private static class DBConnection implements AutoCloseable {

		protected final Connection conn;

		private DBConnection(HikariDataSource hikariDataSource) {
			try {
				conn = hikariDataSource.getConnection();
			} catch (SQLException sqle) {
				logger.fatal(Throwables.getStackTraceAsString(sqle));
				throw new RuntimeException(sqle);
			}
		}

		@Override
		public void close() {
			try {
				conn.close();
			} catch (SQLException sqle) {
				logger.fatal(Throwables.getStackTraceAsString(sqle));
				throw new RuntimeException(sqle);
			}
		}
	}

	public static class WriteChatMessagesDBConnection extends DBConnection {

		private WriteChatMessagesDBConnection() {
			super(writeChatMessagesDataSource);
		}

		/**
		 * 
		 * @param message
		 * @return the message's id in the database (if adding the message was unsuccesfull then returns -1)
		 */
		public int addMessage(DatabaseChatMessage message) {

			int messageID = -1;

			String sql = """
					    INSERT INTO chat_messages
					    (chat_session_id, message_id, client_id, text, file_name, file_bytes, content_type)
					    VALUES (?, ?, ?, ?, ?, ?, ?)
					    RETURNING message_id;
					""";

			try (PreparedStatement addMessage = conn.prepareStatement(sql)) {
				int chatSessionID = message.getChatSessionID();
		        int generatedMessageID = MessageIDGenerator.incrementAndGetMessageID(chatSessionID, conn);

				addMessage.setInt(1, chatSessionID);
				addMessage.setInt(2, generatedMessageID);
				addMessage.setInt(3, message.getClientID());
				addMessage.setBytes(4, message.getText()); // keep in mind this converts the bytes to hexadecimal form
				addMessage.setBytes(5, message.getFileName()); // keep in mind this converts the bytes to hexadecimal form
				addMessage.setBytes(6, message.getFileBytes());
				addMessage.setInt(7, ContentTypeConverter.getContentTypeAsDatabaseInt(message.getContentType()));

				try (ResultSet rs = addMessage.executeQuery()) {
					rs.next();
					messageID = rs.getInt(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return messageID;
		}
	}

	public static class GeneralPurposeDBConnection extends DBConnection {

		private static final UsernameComplexityChecker usernameComplexityChecker;
		private static final PasswordComplexityChecker passwordComplexityChecker;

		static {
			usernameComplexityChecker = new UsernameComplexityChecker(DatabaseSettings.Client.Username.REQUIREMENTS);
			passwordComplexityChecker = new PasswordComplexityChecker(DatabaseSettings.Client.Password.REQUIREMENTS);
		}

		private GeneralPurposeDBConnection() {
			super(generalPurposeDataSource);
		}

		public ResultHolder checkIfUserMeetsRequirementsToCreateAccount(String username, String password,
				String emailAddress) {

			// Check if username and password meets the requirements
			if (!usernameComplexityChecker.estimate(username)) {
				return usernameComplexityChecker.getResultWhenUnsuccesfull();
			}

			if (!passwordComplexityChecker.estimate(password)) {
				return passwordComplexityChecker.getResultWhenUnsuccesfull();
			}

			if (accountWithEmailExists(emailAddress)) {
				return CreateAccountInfo.CredentialValidation.Result.EMAIL_ALREADY_USED.resultHolder;
			}
			
			return CreateAccountInfo.CredentialValidation.Result.SUCCESFULLY_EXCHANGED_CREDENTIALS.resultHolder;
		}

		/**
		 * First checks if user meets requirements method before createAccount method
		 */
		public EntryResult checkAndCreateAccount(String username, String password, UserDeviceInfo deviceInfo,
				String emailAddress) {

			EntryResult resultHolder = new EntryResult(checkIfUserMeetsRequirementsToCreateAccount(username, password, emailAddress));

			if (!resultHolder.isSuccessful()) {
				return resultHolder;
			}

			return createAccount(username, password, deviceInfo, emailAddress);
		}

		public EntryResult createAccount(String username,
				String password,
				UserDeviceInfo deviceInfo, String emailAddress) {

			// Retrieve and delete a unique client ID. If account creation fails, 
			// the deleted client ID will be regenerated during the next generation.
			int clientID = ClientIDGenerator.retrieveAndDelete(conn);

			if (clientID == -1) {
				return new EntryResult(CreateAccountInfo.CreateAccount.Result.DATABASE_MAX_SIZE_REACHED.resultHolder);
			}

			String salt;
			String passwordHashResult;
			String[] hashedBackupVerificationCodes;

			{
				SimpleHash passwordHash = HashUtil.createHash(password,
						DatabaseSettings.Client.General.SaltForHashing.SALT_LENGTH,
						DatabaseSettings.Client.Password.Hashing.HASHING_ALGORITHM);

				passwordHashResult = passwordHash.getHashString();
				salt = passwordHash.getSalt();
				
				hashedBackupVerificationCodes = BackupVerificationCodesGenerator.generateHashedBackupVerificationCodes(salt);
			}

			try (PreparedStatement createUser = conn.prepareStatement("INSERT INTO users ("
					+ "email, password_hash, client_id, backup_verification_codes, salt) "
					+ "VALUES(?, ?, ?, ?, ?);")) {

				createUser.setString(1, emailAddress);
				createUser.setString(2, passwordHashResult);
				createUser.setInt(3, clientID);

				Array backupVerificationCodesArray = conn.createArrayOf("TEXT", hashedBackupVerificationCodes);
				createUser.setArray(4, backupVerificationCodesArray);
				backupVerificationCodesArray.free();

				createUser.setString(5, salt);

				int resultUpdate = createUser.executeUpdate();
				
				if (resultUpdate == 0) {
					return new EntryResult(CreateAccountInfo.CreateAccount.Result.ERROR_WHILE_CREATING_ACCOUNT.resultHolder);
				}
			} catch (SQLException sqle) {
				logger.trace(Throwables.getStackTraceAsString(sqle));
			}
			
			try (PreparedStatement createProfile = conn.prepareStatement("INSERT INTO user_profiles ("
					+ "display_name, client_id, about) "
					+ "VALUES(?, ?, ?);")) {

				createProfile.setString(1, username);
				createProfile.setInt(2, clientID);
				createProfile.setString(3, "");

				int resultUpdate = createProfile.executeUpdate();

				if (resultUpdate == 1) {

					insertUserIp(clientID, deviceInfo);
					
					Map<AddedInfo, String> addedInfo = new EnumMap<>(AddedInfo.class);
					addedInfo.put(AddedInfo.PASSWORD_HASH, passwordHashResult);
					addedInfo.put(AddedInfo.BACKUP_VERIFICATION_CODES, String.join("\n", hashedBackupVerificationCodes));
					
					return new EntryResult(CreateAccountInfo.CreateAccount.Result.SUCCESFULLY_CREATED_ACCOUNT.resultHolder, addedInfo);
				}
			} catch (SQLException sqle) {
				logger.trace(Throwables.getStackTraceAsString(sqle));
			}

			return new EntryResult(CreateAccountInfo.CreateAccount.Result.ERROR_WHILE_CREATING_ACCOUNT.resultHolder);
		}

		public int deleteAccount(String enteredEmail, String enteredPassword, int clientID) {

			int resultUpdate = 0;

			// Verify that the entered email is associated with the provided client ID
			int associatedClientID = getClientID(enteredEmail);
			if (associatedClientID != clientID) {
				return resultUpdate;
			}

			// Perform authentication to ensure email and password match
			if (checkAuthentication(enteredEmail, enteredPassword) == null) {
				return resultUpdate;
			}

			try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE client_id=?;")) {
				pstmt.setInt(1, clientID);

				resultUpdate = pstmt.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}

		public boolean checkAuthenticationViaHash(String email, String enteredPasswordpasswordHash) {
			String passwordHash = getPasswordHash(email);
			
			if (passwordHash != null) {
				return passwordHash.equals(enteredPasswordpasswordHash);
			}
			
			return false;
		}
		
		public String checkAuthentication(String email, String enteredPassword) {
			String passwordHash = getPasswordHash(email);
			SimpleHash enteredPasswordHash = HashUtil.createHash(enteredPassword, getSalt(email), DatabaseSettings.Client.Password.Hashing.HASHING_ALGORITHM);
			return Arrays.equals(passwordHash.getBytes(), enteredPasswordHash.getHashBytes()) ? passwordHash : null;
		}
		
		public ResultHolder checkIfUserMeetsRequirementsToLogin(String emailAddress) {
			if (!accountWithEmailExists(emailAddress)) {
				return LoginInfo.CredentialsExchange.Result.ACCOUNT_DOESNT_EXIST.resultHolder;
			}

			return LoginInfo.CredentialsExchange.Result.SUCCESFULLY_EXCHANGED_CREDENTIALS.resultHolder;
		}

		public EntryResult checkRequirementsAndLogin(String email, String password, UserDeviceInfo deviceInfo) {

			ResultHolder resultHolder = checkIfUserMeetsRequirementsToLogin(email);

			if (!resultHolder.isSuccessful()) {
				return new EntryResult(resultHolder);
			}

			return loginUsingPassword(email, password, deviceInfo);
		}

		@Deprecated
		public ResultHolder loginUsingBackupVerificationCode(String email, String backupVerificationCode, UserDeviceInfo deviceInfo) {

			String[] backupVerificationCodes = getBackupVerificationCodesAsStringArray(email);
			
			boolean isBackupVerificationCodeCorrect = false;
			
			for (int i = 0; i < backupVerificationCodes.length; i++) {
				if (backupVerificationCodes[i].equals(backupVerificationCode)) {
					isBackupVerificationCodeCorrect = true;
					break;
				}
			}
			
			if (!isBackupVerificationCodeCorrect) {
				return LoginInfo.Login.Result.INCORRECT_BACKUP_VERIFICATION_CODE.resultHolder;
			}
			
			// Remove backup verification code from user - a backup verification code can only be used once
			removeBackupVerificationCode(backupVerificationCode, email);

			// Regenerate backup verification codes if they have become 0
			boolean hasRegeneratedBackupVerificationCodes = false;
			if (getNumberOfBackupVerificationCodesLeft(email) == 0) {
				regenerateBackupVerificationCodes(email);
				hasRegeneratedBackupVerificationCodes = true;
			}
			
			// Add address to user logged in ip addresses
			Constant resultC = insertUserIp(email, deviceInfo);
			
			if (resultC == Constant.SUCCESSFUL_INSERT) {
				
				ResultHolder result = LoginInfo.Login.Result.SUCCESFULLY_LOGGED_IN.resultHolder;
				
				// If has regenerated backup verification codes then add the to the result message
				if (hasRegeneratedBackupVerificationCodes) {
//					result.addTextToResultMessage("Backup Verification Codes:\n" + String.join("\n", getBackupVerificationCodesAsStringArray(email)));
				}
				
				return result;
			}

			return LoginInfo.Login.Result.ERROR_WHILE_LOGGING_IN.resultHolder;
		}
		
		public EntryResult loginUsingPassword(String email, String password, UserDeviceInfo deviceInfo) {

			String passwordHash = checkAuthentication(email, password);
			if (passwordHash == null) {
				return new EntryResult(LoginInfo.Login.Result.INCORRECT_PASSWORD.resultHolder);
			}

			// Add address to user logged in ip addresses
			Constant result = insertUserIp(email, deviceInfo);

			if (result != Constant.NOTHING_CHANGED) {
				Map<AddedInfo, String> info = new EnumMap<>(AddedInfo.class);
				info.put(AddedInfo.PASSWORD_HASH, passwordHash);
				return new EntryResult(LoginInfo.Login.Result.SUCCESFULLY_LOGGED_IN.resultHolder, info);
			}

			return new EntryResult(LoginInfo.Login.Result.ERROR_WHILE_LOGGING_IN.resultHolder);
		}

		public Constant insertUserIp(String email, UserDeviceInfo deviceInfo) {
			return insertUserIp(getClientID(email), deviceInfo);
		}
		
		public Constant insertUserIp(int clientID, UserDeviceInfo deviceInfo) {
			String sql = """
					  INSERT INTO user_ips (client_id, ip_address, device_type, os_name)
					  VALUES (?, ?, ?, ?)
					  ON CONFLICT (client_id, ip_address) DO NOTHING;
					""";
			
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setInt(1, clientID);
				pstmt.setString(2, deviceInfo.ipAddress());
				pstmt.setInt(3, DeviceTypeConverter.getDeviceTypeAsDatabaseInt(deviceInfo.deviceType()));
				pstmt.setString(4, deviceInfo.osName());
				
		        int affectedRows = pstmt.executeUpdate();
		        return affectedRows > 0 ? Constant.SUCCESSFUL_INSERT : Constant.DUPLICATE_ENTRY;
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return Constant.NOTHING_CHANGED;
		}

		public ResultHolder changeDisplayName(int clientID, String newDisplayName) {
			
			if (!usernameComplexityChecker.estimate(newDisplayName)) {
				return ChangeUsernameResult.REQUIREMENTS_NOT_MET.resultHolder;
			}
			
			String sql = "UPDATE user_profiles SET display_name=? WHERE client_id=?";
			try (PreparedStatement changeUsername = conn.prepareStatement(sql)) {
				changeUsername.setString(1, newDisplayName);
				changeUsername.setInt(2, clientID);

				int resultUpdate = changeUsername.executeUpdate();
				if (resultUpdate == 1) {
					return ChangeUsernameResult.SUCCESFULLY_CHANGED_USERNAME.resultHolder;
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return ChangeUsernameResult.ERROR_WHILE_CHANGING_USERNAME.resultHolder;
		}
		
		public ResultHolder changePassword(String emailAddress, String newPassword) {

			if (!passwordComplexityChecker.estimate(newPassword)) {
				return passwordComplexityChecker.getResultWhenUnsuccesfull();
			}

			String salt = getSalt(emailAddress);
			
			SimpleHash passwordHash = HashUtil.createHash(newPassword, salt, DatabaseSettings.Client.Password.Hashing.HASHING_ALGORITHM);
			
			try (PreparedStatement changePassword = conn
					.prepareStatement("UPDATE users SET password_hash=? WHERE email=?")) {

				changePassword.setString(1, passwordHash.getHashString());
				changePassword.setString(2, emailAddress);

				int resultUpdate = changePassword.executeUpdate();

				if (resultUpdate == 1) {
					return ChangePasswordResult.SUCCESFULLY_CHANGED_PASSWORD.resultHolder;
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return ChangePasswordResult.ERROR_WHILE_CHANGING_PASSWORD.resultHolder;
		}
		
//		public String getDisplayName(InetAddress inetAddress) {
//
//			String username = null;
//
//			try (PreparedStatement getUsername = conn
//					.prepareStatement("SELECT username FROM users WHERE ?=ANY(ips_logged_into);")) {
//
//				getUsername.setString(1, inetAddress.getHostName());
//				ResultSet rs = getUsername.executeQuery();
//
//				if (rs.next()) {
//					username = rs.getString(1);
//				}
//			} catch (SQLException sqle) {
//				logger.error(Throwables.getStackTraceAsString(sqle));
//			}
//
//			return username;
//		}

//		public String getUsername(String emailAddress) {
//
//			String username = null;
//
//			try (PreparedStatement getUsername = conn.prepareStatement("SELECT username FROM users WHERE email=?;")) {
//
//				getUsername.setString(1, emailAddress);
//				ResultSet rs = getUsername.executeQuery();
//
//				if (rs.next()) {
//					username = rs.getString(1);
//				}
//			} catch (SQLException sqle) {
//				logger.error(Throwables.getStackTraceAsString(sqle));
//			}
//
//			return username;
//		}

		public String getUsername(int clientID) {

			String username = null;

			try (PreparedStatement pstmt = conn
					.prepareStatement("SELECT display_name FROM user_profiles WHERE client_id=?;")) {

				pstmt.setInt(1, clientID);
				ResultSet rs = pstmt.executeQuery();

				if (rs.next()) {
					username = rs.getString(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return username;
		}

		public String getPasswordHash(String email) {

			String passwordHash = null;

			try (PreparedStatement pstmt = conn.prepareStatement("SELECT password_hash FROM users WHERE email=?")) {
				pstmt.setString(1, email);
				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					passwordHash = rs.getString(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return passwordHash;
		}
		
		public String getPasswordHash(int clientID) {

			String passwordHash = null;

			try (PreparedStatement pstmt = conn.prepareStatement("SELECT password_hash FROM users WHERE client_id=?")) {
				pstmt.setInt(1, clientID);
				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					passwordHash = rs.getString(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return passwordHash;
		}
		
		public String getSalt(String email) {

			String salt = null;

			try (PreparedStatement getPasswordHash = conn
					.prepareStatement("SELECT salt FROM users WHERE email=?")) {

				getPasswordHash.setString(1, email);

				ResultSet rs = getPasswordHash.executeQuery();
				if (rs.next()) {
					salt = rs.getString(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return salt;
		}
		
		public String getSalt(int clientID) {

			String salt = null;

			try (PreparedStatement getPasswordHash = conn
					.prepareStatement("SELECT salt FROM users WHERE client_id=?")) {

				getPasswordHash.setInt(1, clientID);

				ResultSet rs = getPasswordHash.executeQuery();
				if (rs.next()) {
					salt = rs.getString(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return salt;
		}

		public String getBackupVerificationCodesAsString(String email) {
			return String.join(",", getBackupVerificationCodesAsStringArray(email));
		}
		
		public String[] getBackupVerificationCodesAsStringArray(String email) {

			String[] backupVerificationCodes = null;

			try (PreparedStatement getBackupVerificationCodes = conn
					.prepareStatement("SELECT backup_verification_codes FROM users WHERE email=?")) {

				getBackupVerificationCodes.setString(1, email);

				ResultSet rs = getBackupVerificationCodes.executeQuery();

				if (rs.next()) {
					backupVerificationCodes = (String[]) rs.getArray(1).getArray();
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return backupVerificationCodes;
		}

		public byte[][] getBackupVerificationCodesAsByteArray(String email) {

			String[] backupVerificationCodesString = getBackupVerificationCodesAsStringArray(email);

			byte[][] backupVerificationCodes = new byte[backupVerificationCodesString.length][];
			for (int i = 0; i < backupVerificationCodesString.length; i++) {
				backupVerificationCodes[i] = backupVerificationCodesString[i].getBytes();
			}

			return backupVerificationCodes;
		}
		
		public int regenerateBackupVerificationCodes(String email) {
			
			int resultUpdate = 0;
			
			String salt = getSalt(email);

			String[] hashedBackupVerificationCodes = BackupVerificationCodesGenerator.generateHashedBackupVerificationCodes(salt);

			try (PreparedStatement replaceBackupVerificationCodes = conn
					.prepareStatement("UPDATE users SET backup_verification_codes=? WHERE email=?;")) {
				
				Array backupVerificationCodesArray = conn.createArrayOf("TEXT", hashedBackupVerificationCodes);
				replaceBackupVerificationCodes.setArray(1, backupVerificationCodesArray);
				backupVerificationCodesArray.free();
				
				replaceBackupVerificationCodes.setString(2, email);
				
				resultUpdate = replaceBackupVerificationCodes.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}
		
		public int removeBackupVerificationCode(String backupVerificationCode, String email) {
			
			int resultUpdate = 0;
			
			String sql = "UPDATE users SET backup_verification_codes=array_remove(backup_verification_codes, ?) WHERE email=?";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				
				pstmt.setString(1, backupVerificationCode);
				pstmt.setString(2, email);
				
				pstmt.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}
		
		public int getNumberOfBackupVerificationCodesLeft(String email) {
		
			int numberOfBackupVerificationCodesLeft = 0;

			String sql = "SELECT array_length(backup_verification_codes, 1) FROM users WHERE email=?;";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

				pstmt.setString(1, email);
				
				ResultSet rs = pstmt.executeQuery();
				
				if (rs.next()) {
					numberOfBackupVerificationCodesLeft = rs.getInt(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return numberOfBackupVerificationCodesLeft;
		}

		public int getClientID(InetAddress address) {

			int clientID = -1;

			String sql = "SELECT client_id FROM user_ips WHERE ip_address=?;";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

				pstmt.setString(1, address.getHostName());
				
				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					clientID = rs.getInt(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return clientID;
		}

		public int getClientID(String email) {

			int clientID = -1;

			try (PreparedStatement pstmt = conn.prepareStatement("SELECT client_id FROM users WHERE email=?;")) {

				pstmt.setString(1, email);
				ResultSet rs = pstmt.executeQuery();

				if (rs.next()) {
					clientID = rs.getInt(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return clientID;
		}

		public UserDeviceInfo[] getUserIPS(int clientID) {

			UserDeviceInfo[] userIPS = EmptyArrays.EMPTY_DEVICE_INFO_ARRAY;

			try (PreparedStatement pstmt = conn.prepareStatement(
					"SELECT ip_address, device_type, os_name FROM user_ips WHERE client_id=?", ResultSet.TYPE_SCROLL_SENSITIVE,
					ResultSet.CONCUR_UPDATABLE)) {

				pstmt.setInt(1, clientID);
				ResultSet rs = pstmt.executeQuery();

				// Move to the last row to get the row count
				rs.last();
				int rowCount = rs.getRow(); // Get total rows
				rs.beforeFirst();
				
				userIPS = new UserDeviceInfo[rowCount];
				
				int i = 0;
				while (rs.next()) {
					String address = rs.getString("ip_address");
					DeviceType deviceType = DeviceTypeConverter.getDatabaseIntAsDeviceType(rs.getInt("device_type"));
					String osName = rs.getString("os_name");
					userIPS[i] = new UserDeviceInfo(address, deviceType, osName);
					i++;
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return userIPS;
		}
		
//		public String[] getIPSLoggedInto(String email) {
//
//			String[] ipsLoggedInto = ArrayUtils.EMPTY_STRING_ARRAY;
//
//			try (PreparedStatement getClientID = conn
//					.prepareStatement("SELECT ips_logged_into FROM users WHERE email=?;")) {
//
//				getClientID.setString(1, email);
//				ResultSet rs = getClientID.executeQuery();
//
//				if (!rs.next()) {
//					return ipsLoggedInto;
//				}
//
//				ipsLoggedInto = (String[]) rs.getArray(1).getArray();
//			} catch (SQLException sqle) {
//				logger.error(Throwables.getStackTraceAsString(sqle));
//			}
//
//			return ipsLoggedInto;
//		}

		public int createChat(int chatSessionID, int... members) {

			int resultUpdate = 0;

			String createChatSQL = "INSERT INTO chat_sessions (chat_session_id) VALUES(?) ON CONFLICT DO NOTHING;";
			try (PreparedStatement psmtp = conn.prepareStatement(createChatSQL)) {
				psmtp.setInt(1, chatSessionID);
				resultUpdate = psmtp.executeUpdate();
				
				if (resultUpdate == 1) {
					insertMember(chatSessionID, members);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}
			

			return resultUpdate;
		}
		
		public void insertMember(int chatSessionID, int... members) {
			String insertMembers = "INSERT INTO chat_session_members (chat_session_id, member_id) VALUES(?, ?) ON CONFLICT DO NOTHING;";
			try (PreparedStatement psmtp = conn.prepareStatement(insertMembers)){
				
				for (int i = 0; i < members.length; i++) {
					psmtp.setInt(1, chatSessionID);
					psmtp.setInt(2, members[i]);
					psmtp.addBatch();
				}
				
				psmtp.executeBatch();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}
		}

//		private int createChat(int chatSessionID, int[] membersClientIDs) {
//
//			int resultUpdate = 0;
//
//			try (PreparedStatement createChat = conn
//					.prepareStatement("INSERT INTO chat_sessions (chat_session_id, members) VALUES(?, ?);")) {
//
//				createChat.setInt(1, chatSessionID);
//
//				Integer[] membersClientIDSObject = ArrayUtils.toObject(membersClientIDs);
//
//				Array array = conn.createArrayOf("INTEGER", membersClientIDSObject);
//				createChat.setArray(2, array);
//				array.free();
//
//				resultUpdate = createChat.executeUpdate();
//			} catch (SQLException sqle) {
//				logger.error(Throwables.getStackTraceAsString(sqle));
//			}
//
//			return resultUpdate;
//		}

		/**
		 * Accepts a chat request and creates a new chat session.
		 *
		 * @param senderClientID   ID of the client who sent the chat request.
		 * @param receiverClientID ID of the client who received the chat request.
		 * @return the newly created chat session ID. If the creation of the chat session fails, returns -1.
		 */
		public int acceptChatRequest(int receiverClientID, int senderClientID) {
			
			int chatSessionID = -1;

			try {
		        // Check if the chat request exists
		        String checkRequestSql = "SELECT 1 FROM chat_requests WHERE sender_client_id = ? AND receiver_client_id = ?";
		        try (PreparedStatement pstmt = conn.prepareStatement(checkRequestSql)) {
		            pstmt.setInt(1, senderClientID);
					pstmt.setInt(2, receiverClientID);
					if (!pstmt.execute()) {
						return chatSessionID; // Chat request does not exist
					}
		        }

		        // Generate a new chat session ID
		        int newChatSessionID = ChatSessionIDGenerator.retrieveAndDelete(conn);

				if (newChatSessionID == -1) {
					return newChatSessionID;
				}

		        // Attempt to create the chat session
		        if (createChat(newChatSessionID, senderClientID, receiverClientID) == 1) {
		            // Delete the chat request upon successful creation of the chat session
		            deleteChatRequest(receiverClientID, senderClientID);

		            // Set the generated chat session ID as the result
		            chatSessionID = newChatSessionID;

		        } else {
		        	ChatSessionIDGenerator.undo(newChatSessionID);
		        }
		    } catch (SQLException sqle) {
		        logger.debug("Error accepting chat request", sqle);
		    }

		    return chatSessionID;
		}


		public int deleteChatRequest(int receiverClientID, int senderClientID) {

			int resultUpdate = 0;

			try (PreparedStatement deleteFriendRequest = conn.prepareStatement(
					"DELETE FROM chat_requests WHERE receiver_client_id=? AND sender_client_id=?")) {

				deleteFriendRequest.setInt(1, receiverClientID);
				deleteFriendRequest.setInt(2, senderClientID);

				resultUpdate = deleteFriendRequest.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}

		public int sendChatRequest(int receiverClientID, int senderClientID) {

			int resultUpdate = 0;

		    String sql = "INSERT INTO chat_requests (receiver_client_id, sender_client_id) VALUES (?, ?)";
		    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
		        pstmt.setInt(1, receiverClientID);
		        pstmt.setInt(2, senderClientID);
				resultUpdate = pstmt.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}

		public Integer[] getChatRequests(int clientID) {

			Integer[] friendRequests = ArrayUtils.EMPTY_INTEGER_OBJECT_ARRAY;

			try (PreparedStatement pstmt = conn
					.prepareStatement("SELECT sender_client_id FROM chat_requests WHERE receiver_client_id=?;",
							ResultSet.TYPE_SCROLL_SENSITIVE, 
							ResultSet.CONCUR_UPDATABLE)) {

				pstmt.setInt(1, clientID);
				ResultSet rs = pstmt.executeQuery();

				// Move to the last row to get the row count
				rs.last();
				int rowCount = rs.getRow(); // Get total rows
				rs.beforeFirst();

				friendRequests = new Integer[rowCount];
				
				int i = 0;
				while (rs.next()) {
					friendRequests[i] = rs.getInt("sender_client_id");
					i++;
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return friendRequests;
		}

		public int deleteChatSession(int chatSessionID) {

			int resultUpdate = 0;

			try {

				try (PreparedStatement deleteUserFromChatSession = conn.prepareStatement(
						"UPDATE users SET chat_session_ids = array_remove(chat_session_ids, ?) WHERE client_id=?;")) {

					Integer[] chatMembersClientIDS = getMembersOfChatSession(chatSessionID);
					for (int i = 0; i < chatMembersClientIDS.length; i++) {

						deleteUserFromChatSession.setInt(1, chatSessionID);
						deleteUserFromChatSession.setInt(2, chatMembersClientIDS[i]);

						deleteUserFromChatSession.executeUpdate();
					}

					try (PreparedStatement deleteMessagesOfChatSession = conn
							.prepareStatement("DELETE FROM chat_messages WHERE chat_session_id=?")) {
						deleteMessagesOfChatSession.setInt(1, chatSessionID);
						deleteMessagesOfChatSession.executeUpdate();
					}

					try (PreparedStatement deleteChatSession = conn
							.prepareStatement("DELETE FROM chat_sessions WHERE chat_session_id=?")) {

						deleteChatSession.setInt(1, chatSessionID);

						resultUpdate = deleteChatSession.executeUpdate();
					}
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}

		public int deleteChatMessage(int chatSessionID, int messageID) {

			int resultUpdate = 0;

			try {

				try (PreparedStatement deleteMessage = conn
						.prepareStatement("DELETE FROM chat_messages WHERE chat_session_id=? AND message_id=?")) {

					deleteMessage.setInt(1, chatSessionID);
					deleteMessage.setInt(2, messageID);

					resultUpdate = deleteMessage.executeUpdate();
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}

		/**
		 * 
		 * @returns the ids of the chat sessions that the user belongs to.
		 */
		public Integer[] getChatSessionsUserBelongsTo(int clientID) {

			Integer[] chatSessions = ArrayUtils.EMPTY_INTEGER_OBJECT_ARRAY;

			try (PreparedStatement getChatSessionIDS = conn.prepareStatement(
					"SELECT chat_session_id FROM chat_session_members WHERE member_id=?;",
					ResultSet.TYPE_SCROLL_SENSITIVE,
					ResultSet.CONCUR_UPDATABLE /*
												 * Pass these parameters so ResultSets can move forwards and backwards
												 */)) {

				getChatSessionIDS.setInt(1, clientID);
				ResultSet rs = getChatSessionIDS.executeQuery();

				if (!rs.next()) {
					return chatSessions;
				}
				
				// Move to the last row to get the row count
				rs.last();
				int rowCount = rs.getRow(); // Get total rows
				rs.beforeFirst();

				chatSessions = new Integer[rowCount];

				int i = 0;
				while (rs.next()) {
					Integer chatSessionID = rs.getInt(1);
					System.out.println(chatSessionID);
					chatSessions[i] = chatSessionID;
					i++;
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return chatSessions;
		}

		/**
		 * 
		 * @return the client ids of the members in a chat session
		 */
		public Integer[] getMembersOfChatSession(int chatSessionID) {

			Integer[] members = ArrayUtils.EMPTY_INTEGER_OBJECT_ARRAY;

			try (PreparedStatement getMembersOfChatSessions = conn.prepareStatement(
					"SELECT member_id FROM chat_session_members WHERE chat_session_id=?;", 
					ResultSet.TYPE_SCROLL_SENSITIVE,
					ResultSet.CONCUR_UPDATABLE /*
												 * Pass these parameters so ResultSets can move forwards and backwards
												 */)) {

				getMembersOfChatSessions.setInt(1, chatSessionID);
				ResultSet rs = getMembersOfChatSessions.executeQuery();

				// Move to the last row to get the row count
				rs.last();
				int rowCount = rs.getRow(); // Get total rows
				rs.beforeFirst();

				members = new Integer[rowCount];

				int i = 0;
				while (rs.next()) {
					Integer memberID = rs.getInt(1);
					members[i] = memberID;
					i++;
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return members;
		}
		
		public int logout(InetAddress address, int clientID) {

			int resultUpdate = 0;

			String sql = "DELETE FROM user_ips WHERE ip_address=? AND client_id=?";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

				pstmt.setString(1, address.getHostName());
				pstmt.setInt(2, clientID);

				resultUpdate = pstmt.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}
		
		public int logoutAllDevices(int clientID) {

			int resultUpdate = 0;

			String sql = "DELETE FROM user_ips WHERE client_id=?";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setInt(1, clientID);

				resultUpdate = pstmt.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}

		public boolean isLoggedIn(InetAddress address) {

			boolean isLoggedIn = false;

			try (PreparedStatement getIsLoggedIn = conn
					.prepareStatement("SELECT 1 from user_ips WHERE ip_address=?;")) {

				getIsLoggedIn.setString(1, address.getHostName());
				ResultSet rs = getIsLoggedIn.executeQuery();
				isLoggedIn = rs.next();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return isLoggedIn;
		}

		public String getEmailAddress(int clientID) {

			String emailAddress = null;

			try (PreparedStatement getEmailAddress = conn
					.prepareStatement("SELECT email FROM users WHERE client_id=?;")) {

				getEmailAddress.setInt(1, clientID);
				ResultSet rs = getEmailAddress.executeQuery();

				if (rs.next()) {
					emailAddress = rs.getString(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return emailAddress;
		}

		public boolean accountWithEmailExists(String email) {

			boolean accountExists = false;

			try (PreparedStatement getEmailAddress = conn.prepareStatement("SELECT 1 FROM users WHERE email=?;")) {

				getEmailAddress.setString(1, email);
				ResultSet rs = getEmailAddress.executeQuery();

				accountExists = rs.next();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return accountExists;
		}
		
		public int addUserIcon(int clientID, byte[] icon) {
			
			int resultUpdate = 0;

			try (PreparedStatement pstmt = conn.prepareStatement(
					"UPDATE user_profiles SET profile_photo = ? WHERE client_id = ?;")) {

				pstmt.setBytes(1, icon);
				pstmt.setInt(2, clientID);

				resultUpdate = pstmt.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}
		
		public byte[] selectUserIcon(int clientID) {
			
			byte[] icon = null;

			try (PreparedStatement pstmt = conn.prepareStatement(
					"SELECT profile_photo FROM user_profiles WHERE client_id = ?;")) {

				pstmt.setInt(1, clientID);

				ResultSet rs = pstmt.executeQuery();
				
				if (rs.next()) {
					icon = rs.getBytes(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}
			
			if (icon == null) {
				icon = EmptyArrays.EMPTY_BYTE_ARRAY;
			}

			return icon;
		}

		public LoadedInMemoryFile getFile(int messageID, int chatSessionID) {

			LoadedInMemoryFile file = null;

			try (PreparedStatement getFileBytes = conn.prepareStatement(
					"SELECT file_bytes, file_name FROM chat_messages WHERE message_id=? AND chat_session_id=?;")) {

				getFileBytes.setInt(1, messageID);
				getFileBytes.setInt(2, chatSessionID);

				ResultSet rs = getFileBytes.executeQuery();

				if (rs.next()) {
					byte[] fileBytes = rs.getBytes(1);
					byte[] fileName = rs.getBytes(2);
					file = new LoadedInMemoryFile(new String(fileName), fileBytes);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return file;
		}

		public Message[] selectMessages(int chatSessionID, int numOfMessagesAlreadySelected, int numOfMessagesToSelect) {

			Message[] messages = new Message[0];

			try (PreparedStatement selectMessages = conn.prepareStatement(
					"SELECT message_id, client_id, text, file_name, ts_entered, content_type "
							+ "FROM chat_messages "
							+ "WHERE chat_session_id=? "
							+ "AND message_id <= ? "
							+ "ORDER BY message_id DESC LIMIT ?;",
					ResultSet.TYPE_SCROLL_SENSITIVE,
					ResultSet.CONCUR_UPDATABLE /*
												 * Pass these parameters so ResultSets can move forwards and backwards
												 */)) {

				int messageIDOfLatestMessage = MessageIDGenerator.getMessageIDCount(chatSessionID, conn);
				int messageIDToReadFrom = messageIDOfLatestMessage - numOfMessagesAlreadySelected;
				
				selectMessages.setInt(1, chatSessionID);
				selectMessages.setInt(2, messageIDToReadFrom);
				selectMessages.setInt(3, numOfMessagesToSelect);
				ResultSet rs = selectMessages.executeQuery();

				if (!rs.next()) {
					return messages;
				}

				// <GET MESSAGES SELECTED NUMBER>
				rs.last();
				int rowCount = rs.getRow();
				rs.first();
				// </GET MESSAGES SELECTED NUMBER>

				messages = new Message[rowCount];

				Map<Integer, String> clientIDSToUsernames = new HashMap<>();

				// reverse messages order from newest to oldest to oldest to newest
				for (int i = rowCount - 1; i >= 0; i--, rs.next()) {

					int messageID = rs.getInt(1);
					int clientID = rs.getInt(2);

					String username = clientIDSToUsernames.get(clientID);

					if (username == null) {
						username = getUsername(clientID);
						clientIDSToUsernames.put(clientID, username);
					}

					byte[] textBytes = rs.getBytes(3);
					byte[] fileNameBytes = rs.getBytes(4);

					Timestamp timeWritten = rs.getTimestamp(5);
					
					ContentType contentType = ContentTypeConverter.getDatabaseIntAsContentType(rs.getInt(6));
					
					messages[i] = new Message(username, clientID, messageID, chatSessionID, textBytes, fileNameBytes, timeWritten.getTime(), contentType);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return messages;
		}
	}
}
