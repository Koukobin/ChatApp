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
import org.checkerframework.checker.index.qual.UpperBoundBottom;

import com.google.common.base.Throwables;
import com.zaxxer.hikari.HikariDataSource;

import github.koukobin.ermis.common.LoadedInMemoryFile;
import github.koukobin.ermis.common.entry.AddedInfo;
import github.koukobin.ermis.common.entry.CreateAccountInfo;
import github.koukobin.ermis.common.entry.LoginInfo;
import github.koukobin.ermis.common.message_types.ContentType;
import github.koukobin.ermis.common.message_types.Message;
import github.koukobin.ermis.common.results.ChangePasswordResult;
import github.koukobin.ermis.common.results.ChangeUsernameResult;
import github.koukobin.ermis.common.results.EntryResult;
import github.koukobin.ermis.common.results.ResultHolder;
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
import io.netty.util.internal.EmptyArrays;

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
						.replace("USERNAME_LENGTH", Integer.toString(usernameMaxLength))
						.replace("PASSWORD_HASH_LENGTH", Integer.toString(hashLength))
						.replace("BACKUP_VERIFICATION_CODES_LENGTH", Integer.toString(DatabaseSettings.Client.BackupVerificationCodes.AMOUNT_OF_CODES))
						.replace("BACKUP_VERIFICATION_CODES_AMOUNT", Integer.toString(backupVerificationCodesCharactersLength))
						.replace("SALT_LENGTH", Integer.toString(saltLength));

				stmt.execute(setupSQL);

				ChatSessionIDGenerator.generateAvailableChatSessionIDS(conn);
				ClientIDGenerator.generateAvailableClientIDS(conn);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static GeneralPurposeDBConnection getGeneralPurposeConnection() {
		return new GeneralPurposeDBConnection();
	}

	public static WriteChatMessagesDBConnection getWriteChatMessagesConnection() {
		return new WriteChatMessagesDBConnection();
	}

	private static class DBConnection implements AutoCloseable {

		protected final Connection conn;

		private DBConnection(HikariDataSource hikariDataSource) {
			try {
				conn = hikariDataSource.getConnection();
			} catch (SQLException sqle) {
				throw new RuntimeException(sqle);
			}
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

			try (PreparedStatement addMessage = conn.prepareStatement(
					"INSERT INTO chat_messages (chat_session_id, message_id, client_id, text, file_name, file_bytes, content_type) "
					+ "VALUES(?, ?, ?, ?, ?, ?, ?) RETURNING message_id;")) {

				int chatSessionID = message.getChatSessionID();

				addMessage.setInt(1, chatSessionID);
				addMessage.setInt(2, MessageIDGenerator.incrementAndGetMessageID(chatSessionID, conn));
				addMessage.setInt(3, message.getClientID());
				addMessage.setBytes(4, message.getText()); // keep in mind this converts the bytes to hexadecimal form
				addMessage.setBytes(5, message.getFileName()); // keep in mind this converts the bytes to hexadecimal form
				addMessage.setBytes(6, message.getFileBytes());
				addMessage.setInt(7, ContentTypeConverter.getContentTypeAsDatabaseInt(message.getContentType()));

				ResultSet rs = addMessage.executeQuery();
				rs.next();

				messageID = rs.getInt(1);
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
		public ResultHolder checkAndCreateAccount(String username, String password, InetAddress inetAddress,
				String emailAddress) {

			ResultHolder resultHolder = checkIfUserMeetsRequirementsToCreateAccount(username, password, emailAddress);

			if (!resultHolder.isSuccessful()) {
				return resultHolder;
			}

			return createAccount(username, password, inetAddress, emailAddress);
		}

		public ResultHolder createAccount(String username, String password, InetAddress inetAddress,
				String emailAddress) {

			int clientID = ClientIDGenerator.retrieveAndDelete(conn);

			if (clientID == -1) {
				return CreateAccountInfo.CreateAccount.Result.DATABASE_MAX_SIZE_REACHED.resultHolder;
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

			try (PreparedStatement createAccount = conn.prepareStatement("INSERT INTO users ("
					+ "username, password_hash, email, client_id, ips_logged_into, backup_verification_codes, salt) "
					+ "VALUES(?, ?, ?, ?, ARRAY [?], ?, ?);")) {

				createAccount.setString(1, username);
				createAccount.setString(2, passwordHashResult);
				createAccount.setString(3, emailAddress);
				createAccount.setInt(4, clientID);
				createAccount.setString(5, inetAddress.getHostName());

				Array backupVerificationCodesArray = conn.createArrayOf("TEXT", hashedBackupVerificationCodes);
				createAccount.setArray(6, backupVerificationCodesArray);
				backupVerificationCodesArray.free();

				createAccount.setString(7, salt);

				int resultUpdate = createAccount.executeUpdate();

				if (resultUpdate == 1) {
					
					ResultHolder result = CreateAccountInfo.CreateAccount.Result.SUCCESFULLY_CREATED_ACCOUNT.resultHolder;
					result.addTextToResultMessage("Backup Verification Codes:\n" + String.join("\n", hashedBackupVerificationCodes));
					result.addTextToResultMessage("Backup Verification Codes are automatically generated and sent to your email in the case that you run out.");
					
					return result;
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return CreateAccountInfo.CreateAccount.Result.ERROR_WHILE_CREATING_ACCOUNT.resultHolder;
		}

		@Deprecated
		@UpperBoundBottom
		public int deleteAccount(String email, String password) {

			int resultUpdate = 0;

//			if (!checkAuthentication(email, password) ) {
//				return resultUpdate;
//			}
			
			if (checkAuthentication(email, password) != null ) {
			return resultUpdate;
		}

			try (PreparedStatement deleteAccount = conn.prepareStatement("DELETE FROM users WHERE email=?;")) {

				deleteAccount.setString(1, email);

				resultUpdate = deleteAccount.executeUpdate();
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

		public EntryResult checkRequirementsAndLogin(String email, String password, InetAddress inetAddress) {

			ResultHolder resultHolder = checkIfUserMeetsRequirementsToLogin(email);

			if (!resultHolder.isSuccessful()) {
				return new EntryResult(resultHolder);
			}

			return loginUsingPassword(inetAddress, email, password);
		}

		public ResultHolder loginUsingBackupVerificationCode(InetAddress inetAddress, String email, String backupVerificationCode) {

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
			int resultUpdate = addIpAddressLoggedInto(inetAddress, email);
			
			if (resultUpdate == 1) {
				
				ResultHolder result = LoginInfo.Login.Result.SUCCESFULLY_LOGGED_IN.resultHolder;
				
				// If has regenerated backup verification codes then add the to the result message
				if (hasRegeneratedBackupVerificationCodes) {
					result.addTextToResultMessage("Backup Verification Codes:\n" + String.join("\n", getBackupVerificationCodesAsStringArray(email)));
				}
				
				return result;
			}

			return LoginInfo.Login.Result.ERROR_WHILE_LOGGING_IN.resultHolder;
		}
		
		public EntryResult loginUsingPassword(InetAddress inetAddress, String email, String password) {

			String passwordHash = checkAuthentication(email, password);
			if (passwordHash == null) {
				return new EntryResult(LoginInfo.Login.Result.INCORRECT_PASSWORD.resultHolder);
			}

			// Add address to user logged in ip addresses
			int resultUpdate = addIpAddressLoggedInto(inetAddress, email);

			if (resultUpdate == 1) {
				Map<AddedInfo, String> info = new EnumMap<>(AddedInfo.class);
				info.put(AddedInfo.PASSWORD_HASH, passwordHash);
				return new EntryResult(LoginInfo.Login.Result.SUCCESFULLY_LOGGED_IN.resultHolder, info);
			}

			return new EntryResult(LoginInfo.Login.Result.ERROR_WHILE_LOGGING_IN.resultHolder);
		}
		
		public int addIpAddressLoggedInto(InetAddress inetAddress, String email) {
			
			int resultUpdate = 0;
			
			try (PreparedStatement updateIPS = conn.prepareStatement(
					"UPDATE users SET ips_logged_into=array_append(ips_logged_into, ?) where email=?;")) {

				updateIPS.setString(1, inetAddress.getHostName());
				updateIPS.setString(2, email);

				resultUpdate = updateIPS.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}
			
			return resultUpdate;
		}

		public ResultHolder changeUsername(int clientID, String newUsername) {

			if (!usernameComplexityChecker.estimate(newUsername)) {
				return usernameComplexityChecker.getResultWhenUnsuccesfull();
			}

			try (PreparedStatement changeUsername = conn
					.prepareStatement("UPDATE users SET username=? WHERE client_id=?")) {

				changeUsername.setString(1, newUsername);
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
		
		public String getUsername(InetAddress inetAddress) {

			String username = null;

			try (PreparedStatement getUsername = conn
					.prepareStatement("SELECT username FROM users WHERE ?=ANY(ips_logged_into);")) {

				getUsername.setString(1, inetAddress.getHostName());
				ResultSet rs = getUsername.executeQuery();

				if (rs.next()) {
					username = rs.getString(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return username;
		}

		public String getUsername(String emailAddress) {

			String username = null;

			try (PreparedStatement getUsername = conn.prepareStatement("SELECT username FROM users WHERE email=?;")) {

				getUsername.setString(1, emailAddress);
				ResultSet rs = getUsername.executeQuery();

				if (rs.next()) {
					username = rs.getString(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return username;
		}

		public String getUsername(int clientID) {

			String username = null;

			try (PreparedStatement getUsername = conn
					.prepareStatement("SELECT username FROM users WHERE client_id=?;")) {

				getUsername.setInt(1, clientID);
				ResultSet rs = getUsername.executeQuery();

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

			try (PreparedStatement getPasswordHash = conn
					.prepareStatement("SELECT password_hash FROM users WHERE email=?")) {

				getPasswordHash.setString(1, email);

				ResultSet rs = getPasswordHash.executeQuery();

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
			
			try (PreparedStatement removeBackupVerificationCode = conn.prepareStatement("UPDATE users SET backup_verification_codes=array_remove(backup_verification_codes, ?) WHERE email=?")) {
				
				removeBackupVerificationCode.setString(1, backupVerificationCode);
				removeBackupVerificationCode.setString(2, email);
				
				removeBackupVerificationCode.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}
		
		public int getNumberOfBackupVerificationCodesLeft(String email) {
		
			int numberOfBackupVerificationCodesLeft = 0;
			
			try (PreparedStatement getNumberOfBackupVerificationCodesLeft = conn
					.prepareStatement("SELECT array_length(backup_verification_codes, 1) FROM users WHERE email=?;")) {
				
				getNumberOfBackupVerificationCodesLeft.setString(1, email);
				
				ResultSet rs = getNumberOfBackupVerificationCodesLeft.executeQuery();
				
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

			try (PreparedStatement getClientID = conn
					.prepareStatement("SELECT client_id FROM users WHERE ?=ANY(ips_logged_into);")) {

				getClientID.setString(1, address.getHostName());
				ResultSet rs = getClientID.executeQuery();

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

			try (PreparedStatement getClientID = conn.prepareStatement("SELECT client_id FROM users WHERE email=?;")) {

				getClientID.setString(1, email);
				ResultSet rs = getClientID.executeQuery();

				if (rs.next()) {
					clientID = rs.getInt(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return clientID;
		}

		public String[] getIPSLoggedInto(String email) {

			String[] ipsLoggedInto = ArrayUtils.EMPTY_STRING_ARRAY;

			try (PreparedStatement getClientID = conn
					.prepareStatement("SELECT ips_logged_into FROM users WHERE email=?;")) {

				getClientID.setString(1, email);
				ResultSet rs = getClientID.executeQuery();

				if (!rs.next()) {
					return ipsLoggedInto;
				}

				ipsLoggedInto = (String[]) rs.getArray(1).getArray();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return ipsLoggedInto;
		}

		public int createChatIfDoesntExist(int chatSessionID, int[] membersClientIDs) {

			int resultUpdate = 0;

			try (PreparedStatement createChat = conn.prepareStatement(
					"INSERT INTO chat_sessions (chat_session_id, members) VALUES(?, ?) ON CONFLICT DO NOTHING;")) {

				createChat.setInt(1, chatSessionID);

				Array memberClientIDSObjectArray = conn.createArrayOf("INTEGER", ArrayUtils.toObject(membersClientIDs));
				createChat.setArray(2, memberClientIDSObjectArray);
				memberClientIDSObjectArray.free();

				resultUpdate = createChat.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}

		private int createChat(int chatSessionID, int[] membersClientIDs) {

			int resultUpdate = 0;

			try (PreparedStatement createChat = conn
					.prepareStatement("INSERT INTO chat_sessions (chat_session_id, members) VALUES(?, ?);")) {

				createChat.setInt(1, chatSessionID);

				Integer[] membersClientIDSObject = ArrayUtils.toObject(membersClientIDs);

				Array array = conn.createArrayOf("INTEGER", membersClientIDSObject);
				createChat.setArray(2, array);
				array.free();

				resultUpdate = createChat.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}

		/**
		 * 
		 * @return the newly created chat session id. If the creation of the chat
		 *         session was unsuccesfull returns -1
		 */
		public int acceptChatRequestIfExists(int senderClientID, int receiverClientID) {

			int chatSessionID = -1;

			try {

				// Check to see if the client even has a friend request with the given client id
				try (PreparedStatement doesUserHaveFriendRequest = conn
						.prepareStatement("SELECT 1 FROM users WHERE client_id=? AND (chat_requests @> ARRAY[?]);")) {

					doesUserHaveFriendRequest.setInt(1, senderClientID);
					doesUserHaveFriendRequest.setInt(2, receiverClientID);

					ResultSet rs = doesUserHaveFriendRequest.executeQuery();

					if (!rs.next()) {
						return chatSessionID;
					}

					chatSessionID = acceptChatRequest(senderClientID, receiverClientID);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return chatSessionID;
		}

		/**
		 * 
		 * @return the newly created chat session id. If the creation of the chat
		 *         session was unsuccesfull returns -1
		 */
		public int acceptChatRequest(int senderClientID, int receiverClientID) {

			int chatSessionID = -1;

			try {

				int chatSessionIDTemp = ChatSessionIDGenerator.retrieveAndDelete(conn);

				try (PreparedStatement updateChatSessionIDS = conn.prepareStatement(
						"UPDATE users SET chat_session_ids = array_append(chat_session_ids, ?) WHERE client_id=?;")) {

					updateChatSessionIDS.setInt(1, chatSessionIDTemp);
					updateChatSessionIDS.setInt(2, senderClientID);

					int resultUpdate = updateChatSessionIDS.executeUpdate();

					if (resultUpdate == 0) {
						return chatSessionID;
					}

					updateChatSessionIDS.setInt(2, receiverClientID);
					resultUpdate = updateChatSessionIDS.executeUpdate();

					if (resultUpdate == 0) {
						return chatSessionID;
					}
				}

				createChat(chatSessionIDTemp, new int[] { receiverClientID, senderClientID });
				deleteChatRequest(senderClientID, receiverClientID);

				chatSessionID = chatSessionIDTemp;
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return chatSessionID;
		}

		public int deleteChatRequest(int senderClientID, int receiverClientID) {

			int resultUpdate = 0;

			try (PreparedStatement deleteFriendRequest = conn.prepareStatement(
					"UPDATE users SET chat_requests = array_remove(chat_requests, ?) WHERE client_id=?;")) {

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

			try (PreparedStatement updateUserFriendRequests = conn
					.prepareStatement("UPDATE users SET chat_requests = array_append(chat_requests, ?) "
							+ "WHERE client_id=? " + "AND NOT (chat_requests @> ARRAY[?]);")) {

				updateUserFriendRequests.setInt(1, senderClientID);
				updateUserFriendRequests.setInt(2, receiverClientID);
				updateUserFriendRequests.setInt(3, senderClientID);

				resultUpdate = updateUserFriendRequests.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}

		public Integer[] getChatRequests(int clientID) {

			Integer[] friendRequests = ArrayUtils.EMPTY_INTEGER_OBJECT_ARRAY;

			try (PreparedStatement getFriendRequests = conn
					.prepareStatement("SELECT chat_requests FROM users WHERE client_id=?;")) {

				getFriendRequests.setInt(1, clientID);
				ResultSet rs = getFriendRequests.executeQuery();

				if (!rs.next()) {
					return friendRequests;
				}

				friendRequests = (Integer[]) rs.getArray(1).getArray();
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
		 * @return the ids of the chat sessions that the user belongs to.
		 */
		public Integer[] getChatSessionsUserBelongsTo(int clientID) {

			Integer[] chatSessions = ArrayUtils.EMPTY_INTEGER_OBJECT_ARRAY;

			try (PreparedStatement getChatSessionIDS = conn
					.prepareStatement("SELECT chat_session_ids FROM users WHERE client_id=?;")) {

				getChatSessionIDS.setInt(1, clientID);
				ResultSet rs = getChatSessionIDS.executeQuery();

				if (!rs.next()) {
					return chatSessions;
				}

				chatSessions = (Integer[]) rs.getArray(1).getArray();
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

			try (PreparedStatement getMembersOfChatSessions = conn
					.prepareStatement("SELECT members FROM chat_sessions WHERE chat_session_id=?;")) {

				getMembersOfChatSessions.setInt(1, chatSessionID);
				ResultSet rs = getMembersOfChatSessions.executeQuery();

				if (rs.next()) {
					members = (Integer[]) rs.getArray(1).getArray();
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return members;
		}
		
		public int logout(InetAddress address, int clientID) {

			int resultUpdate = 0;

			try (PreparedStatement logout = conn.prepareStatement(
					"UPDATE users SET ips_logged_into = array_remove(ips_logged_into, ?) WHERE client_id=?;")) {

				logout.setString(1, address.getHostName());
				logout.setInt(2, clientID);

				resultUpdate = logout.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}

		public boolean isLoggedIn(InetAddress address) {

			boolean isLoggedIn = false;

			try (PreparedStatement getIsLoggedIn = conn
					.prepareStatement("SELECT 1 from users WHERE ?=ANY(ips_logged_into);")) {

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

			try (PreparedStatement addMessage = conn.prepareStatement(
					"UPDATE users SET user_icon = ? WHERE client_id = ?;")) {

				addMessage.setBytes(1, icon);
				addMessage.setInt(2, clientID);

				resultUpdate = addMessage.executeUpdate();
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}

			return resultUpdate;
		}
		
		public byte[] selectUserIcon(int clientID) {
			
			byte[] icon = null;

			try (PreparedStatement addMessage = conn.prepareStatement(
					"SELECT user_icon FROM users WHERE client_id = ?;")) {

				addMessage.setInt(1, clientID);

				ResultSet rs = addMessage.executeQuery();
				
				if (rs.next()) {
					icon = rs.getBytes(1);
				}
			} catch (SQLException sqle) {
				logger.error(Throwables.getStackTraceAsString(sqle));
			}
			
			if (icon == null) {
				icon = EmptyArrays.EMPTY_BYTES;
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
					"SELECT message_id, client_id, convert_from(text, 'UTF8'), convert_from(file_name, 'UTF8'), ts_entered, content_type "
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
