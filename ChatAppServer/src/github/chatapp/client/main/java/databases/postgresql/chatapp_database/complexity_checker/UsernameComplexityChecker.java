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
package github.chatapp.client.main.java.databases.postgresql.chatapp_database.complexity_checker;

import java.util.Objects;

import github.chatapp.common.reults.IsUsernameValidResult;
import github.chatapp.common.reults.ResultHolder;

/**
 * @author Ilias Koukovinis
 *
 */
public final class UsernameComplexityChecker {

	private Requirements requirements;
	
	public UsernameComplexityChecker() {}

	public UsernameComplexityChecker(Requirements requirements) {
		this.requirements = requirements;
	}

	public void setRequirements(Requirements requirements) {
		this.requirements = requirements;
	}
	
	public ResultHolder getResultWhenUnsuccesfull() {
		ResultHolder result = IsUsernameValidResult.REQUIREMENTS_NOT_MET.resultHolder;
		result.addTextToResultMessage("Requirements:\n" + requirements.toString());
		return result;
	}

	public Requirements getRequirements() {
		return requirements;
	}

	public boolean estimate(String username) {
		return ComplexityCheckerUtil.estimate(requirements, username);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requirements);
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

		UsernameComplexityChecker other = (UsernameComplexityChecker) obj;
		return Objects.equals(requirements, other.requirements);
	}

	@Override
	public String toString() {
		return "UsernameComplexityChecker [requirements=" + requirements + "]";
	}
}
