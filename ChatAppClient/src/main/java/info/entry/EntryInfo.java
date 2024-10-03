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
package main.java.info.entry;

import java.net.URL;

/**
 * @author Ilias Koukovinis
 *
 */
public final class EntryInfo {

	public static class Login {
		
		public static final URL FXML_LOCATION = EntryInfo.class.getResource("/main/resources/view/entry/LoginScene.fxml");
		public static final String CSS_LOCATION = EntryInfo.class.getResource("/main/resources/css/entry/LoginScene.css").toExternalForm();
		
		private Login() {}
	}
	
	public static class CreateAccount {
		
		public static final URL FXML_LOCATION = EntryInfo.class.getResource("/main/resources/view/entry/CreateAccountScene.fxml");
		public static final String CSS_LOCATION = EntryInfo.class.getResource("/main/resources/css/entry/CreateAccountScene.css").toExternalForm();
		
		private CreateAccount() {}
	}

	private EntryInfo() {}
}
