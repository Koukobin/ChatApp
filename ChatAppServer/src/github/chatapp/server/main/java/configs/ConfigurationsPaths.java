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
package github.chatapp.server.main.java.configs;

/**
 * @author Ilias Koukovinis
 *
 */
final class ConfigurationsPaths {

	public static final String CONFIGURATIONS_ROOT_FOLDER_PATH = "/srv/ChatApp-Server/configs/";
	
	public static class Server {
		
		public static final String SERVER_SETTINGS_PATH = CONFIGURATIONS_ROOT_FOLDER_PATH + "Server Settings/";
		
		public static final String GENERAL_SETTINGS_PATH = SERVER_SETTINGS_PATH + "General Settings.cnf";
		public static final String SSL_SETTINGS_PATH = SERVER_SETTINGS_PATH + "SSLSettings.cnf";

		public static class EmailCreator {

			public static class Verification {

				public static class Login {

					public static final String VERIFICATION_EMAIL_BODY_FILE_PATH = SERVER_SETTINGS_PATH + "Login Verification Message.txt";

					private Login() {}
				}

				public static class CreateAccount {

					public static final String VERIFICATION_EMAIL_BODY_FILE_PATH = SERVER_SETTINGS_PATH + "CreateAccount Verification Message.txt";

					private CreateAccount() {}
				}

				private Verification() {}
			}

			private EmailCreator() {}
		}
		
		private Server() {}
	}
	
	public static class Donations {
		
		public static final String DONATIONS_SETTINGS_PATH = CONFIGURATIONS_ROOT_FOLDER_PATH + "Donation Settings/";
		
		public static final String HTML_FILE_PATH = DONATIONS_SETTINGS_PATH + "index.html";
		
		private Donations() {}
	}
	
	public static class Emailer {
		
		public static final String EMAILER_SETTINGS_PATH = CONFIGURATIONS_ROOT_FOLDER_PATH + "Emailer Settings/";
		
		public static final String GENERAL_SETTINGS_PATH = EMAILER_SETTINGS_PATH + "GeneralSettings.cnf";
		
		private Emailer() {}
	}
	
	public static class Database {
		
		public static final String DATABASE_SETTINGS_PATH = CONFIGURATIONS_ROOT_FOLDER_PATH + "Database Settings/";
		
		public static final String GENERAL_SETTINGS_PATH = DATABASE_SETTINGS_PATH + "GeneralSettings.cnf";
		public static final String DRIVER_SETTINGS_PATH = DATABASE_SETTINGS_PATH + "DriverSettings.cnf";
		public static final String POOLING_SETTINGS_PATH = DATABASE_SETTINGS_PATH + "PoolingSettings.cnf";
		
		public static class Client {
			
			public static final String CLIENT_SETTINGS_PATH = DATABASE_SETTINGS_PATH + "Client Settings/";
			
			public static final String GENERAL_SETTINGS_PATH = CLIENT_SETTINGS_PATH + "General Settings.cnf";
			public static final String USERNAME_SETTINGS_PATH = CLIENT_SETTINGS_PATH + "UsernameSettings.cnf";
			
			public static class Password {
				
				public static final String PASSWORD_SETTINGS_PATH = CLIENT_SETTINGS_PATH + "Password Settings/";
				
				public static final String GENERAL_SETTINGS_PATH = PASSWORD_SETTINGS_PATH + "General Settings.cnf";

				public static class HashingAlgorithms {
					
					public static final String HASHING_ALGORITHMS_SETTINGS_PATH = PASSWORD_SETTINGS_PATH + "Hashing Algorithms/";
					
					public static final String ARGON2_SETTINGS_PATH = HASHING_ALGORITHMS_SETTINGS_PATH + "Argon2.cnf";
					public static final String BCRYPT_SETTINGS_PATH = HASHING_ALGORITHMS_SETTINGS_PATH + "Bcrypt.cnf";
					public static final String SCRYPT_SETTINGS_PATH = HASHING_ALGORITHMS_SETTINGS_PATH + "Scrypt.cnf";
					
					private HashingAlgorithms() {}
				}
				
				private Password() {}
			}
			
			public static class BackupVerificationCodes {
				
				public static final String BACKUP_VERIFICATION_CODES_SETTINGS_PATH = CLIENT_SETTINGS_PATH + "Backup Verification Codes Settings/";
				
				public static final String GENERAL_SETTINGS_PATH = BACKUP_VERIFICATION_CODES_SETTINGS_PATH + "General Settings.cnf";
				
				public static class HashingAlgorithms {
					
					public static final String HASHING_ALGORITHMS_SETTINGS_PATH = BACKUP_VERIFICATION_CODES_SETTINGS_PATH + "Hashing Algorithms/";
					
					public static final String ARGON2_SETTINGS_PATH = HASHING_ALGORITHMS_SETTINGS_PATH + "Argon2.cnf";
					public static final String BCRYPT_SETTINGS_PATH = HASHING_ALGORITHMS_SETTINGS_PATH + "Bcrypt.cnf";
					public static final String SCRYPT_SETTINGS_PATH = HASHING_ALGORITHMS_SETTINGS_PATH + "Scrypt.cnf";
					
					private HashingAlgorithms() {}
				}
				
				private BackupVerificationCodes() {}
			}
			
			private Client() {}
		}
		
		private Database() {}
	}
	
	public static class LoggerSettingsPath {
		
		public static final String LOGGER_SETTINGS_PATH = CONFIGURATIONS_ROOT_FOLDER_PATH + "Logger Settings/";
		
		public static final String LOG4J_SETTINGS_PATH = LOGGER_SETTINGS_PATH + "log4j2.xml";
		
		private LoggerSettingsPath() {}
	}
	
	private ConfigurationsPaths() {}
}
