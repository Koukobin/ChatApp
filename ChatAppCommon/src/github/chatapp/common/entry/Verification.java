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
public final class Verification {
	
	private Verification() {}
	
	public enum Action {
		RESEND_CODE;
	}
	
	public enum Result {
		SUCCESFULLY_VERIFIED(true, "Succesfully verified!"),
		WRONG_CODE(false, "Incorrent code!"),
		RUN_OUT_OF_ATTEMPTS(false, "Run out of attempts!");

		public final ResultHolder resultHolder;
		
		Result(boolean isSuccesfull, String message) {
			resultHolder = new ResultHolder(isSuccesfull, message);
		}
	}
}