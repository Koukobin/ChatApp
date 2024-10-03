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
package github.chatapp.common;

/**
 * @author Ilias Koukovinis
 *
 */
public enum IsUsernameValidResult {
	SUCCESFULLY_VALIDATED_USERNAME(true, "Succesfully validated username!"),
	REQUIREMENTS_NOT_MET(false, "Username requirements not met!");

	public final ResultHolder resultHolder;
	
	IsUsernameValidResult(boolean isSuccesfull, String message) {
		resultHolder = new ResultHolder(isSuccesfull, message);
	}
}
