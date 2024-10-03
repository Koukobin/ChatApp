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

import java.util.Objects;

/**
 * @author Ilias Koukovinis
 *
 */
public class ResultHolder {

	private boolean isSuccesfull;
	private String resultMessage;

	public ResultHolder(boolean isSuccesfull, String resultMessage) {
		this.isSuccesfull = isSuccesfull;
		this.resultMessage = resultMessage;
	}

	public void setIsSuccesfull(boolean isSuccesfull) {
		this.isSuccesfull = isSuccesfull;
	}
	
	public void setResultMessage(String resultMessage) {
		this.resultMessage = resultMessage;
	}
	
	public void addTextToResultMessage(String text) {
		resultMessage = resultMessage + "\n\n" + text;
	}
	
	public boolean isSuccesfull() {
		return isSuccesfull;
	}

	public String getResultMessage() {
		return resultMessage;
	}

	@Override
	public int hashCode() {
		return Objects.hash(isSuccesfull, resultMessage);
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		ResultHolder other = (ResultHolder) obj;
		return isSuccesfull == other.isSuccesfull && Objects.equals(resultMessage, other.resultMessage);
	}

	@Override
	public String toString() {
		return "ResultHolder [isSuccesfull=" + isSuccesfull + ", resultMessage=" + resultMessage + "]";
	}
}
