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
package github.chatapp.common.entry;

import github.chatapp.common.reults.ResultHolder;

/**
 * 
 * @author Ilias Koukovinis
 *
 */
public final class LoginInfo {
	
	private LoginInfo() {}
	
	public enum Credential {
		EMAIL, PASSWORD;
	}
	
	public enum PasswordType {
		PASSWORD, BACKUP_VERIFICATION_CODE
	}

	public enum Action {
		TOGGLE_PASSWORD_TYPE;
	}
	
	public enum AuthenticationStage {
		CREDENTIALS_EXCHANGE, LOGIN;
		
		public static class CredentialsExchange {
			public enum Result {
				SUCCESFULLY_EXCHANGED_CREDENTIALS(true, "Succesfully exchanged credentials!"),
				INCORRECT_EMAIL(false, "Incorrect email!"), 
				ACCOUNT_DOESNT_EXIST(false, "Account doesn't exist!");

				public final ResultHolder resultHolder;
				
				Result(boolean isSuccesfull, String message) {
					resultHolder = new ResultHolder(isSuccesfull, message);
				}
			}
		}
		
		public static class Login {
			public enum Result {
				SUCCESFULLY_LOGGED_IN(true, "Succesfully logged into your account!"), 
				ERROR_WHILE_LOGGING_IN(false,
						"An error occured while logging into your account! "
								+ "Please contanct the server administrator "
								+ "and let them know know that their server is broken."),
				INCORRECT_PASSWORD(false, "Incorrect password."),
				INCORRECT_BACKUP_VERIFICATION_CODE(false, "Incorrect backup verification code.");

				public final ResultHolder resultHolder;

				Result(boolean isSuccesfull, String message) {
					resultHolder = new ResultHolder(isSuccesfull, message);
				}
			}
		}
		
	}
}