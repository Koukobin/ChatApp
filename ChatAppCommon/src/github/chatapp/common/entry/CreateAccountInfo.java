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

import github.chatapp.common.ResultHolder;

/**
 * 
 * @author Ilias Koukovinis
 *
 */
public final class CreateAccountInfo {

	private CreateAccountInfo() {}

	public enum Credential {
		USERNAME, PASSWORD, EMAIL;
	}
	
	public enum AuthenticationStage {
		CREDENTIALS_EXCHANGE, LOGIN;
		
		public static class CredentialsExchange {
			public enum Result {
				SUCCESFULLY_EXCHANGED_CREDENTIALS(true, "Succesfully exchanged credentials!"),
				UNABLE_TO_GENERATE_CLIENT_ID(false, "Unable to generate client id!"),
				EMAIL_ALREADY_USED(false, "Email is already used!"),
				USERNAME_REQUIREMENTS_NOT_MET(false, "Username requirements not met!"),
				PASSWORD_REQUIREMENTS_NOT_MET(false, "Password requirements not met!");

				public final ResultHolder resultHolder;
				
				Result(boolean isSuccesfull, String message) {
					resultHolder = new ResultHolder(isSuccesfull, message);
				}
			}
		}
		
		public static class CreateAccount {
			public enum Result {
				SUCCESFULLY_CREATED_ACCOUNT(true, "Account successfully created!"),
				ERROR_WHILE_CREATING_ACOUNT(false, "An error occured while creating your account! "), 
				DATABASE_MAX_SIZE_REACHED(false, "Database max user count reached!"), 
				EMAIL_ALREADY_USED(false, "Email is already used!"),
				USERNAME_ALREADY_USED(false, "Username is already used!");
				
				public final ResultHolder resultHolder;
				
				Result(boolean isSuccesfull, String message) {
					resultHolder = new ResultHolder(isSuccesfull, message);
				}
			}
		}
		
	}
}