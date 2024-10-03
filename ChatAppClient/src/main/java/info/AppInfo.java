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
package main.java.info;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.chatapp.commons.util.FileEditor;

/**
 * @author Ilias Koukovinis
 *
 */
public final class AppInfo {

	private static final File CLIENT_INFO_FILE = new File(GeneralAppInfo.CLIENT_INFO_PATH);
	private static final Properties CLIENT_INFO_PROPERTIES;

	static {
		try {
			CLIENT_INFO_PROPERTIES = FileEditor.readPropertiesFile(CLIENT_INFO_FILE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public enum Key {
		HAS_AGREED_TO_LICENCE_AGREEMENT("hasAgreedToLicenceAgreement");
		
		private final String stringKey;
		private final String value;
		
		Key(String stringKey) {
			this.stringKey = stringKey;
			this.value = CLIENT_INFO_PROPERTIES.getProperty(stringKey);
		}
		
		public void replaceValue(String newValue) throws IOException {
			CLIENT_INFO_PROPERTIES.replace(stringKey, newValue);
			try (FileWriter fileWriter = new FileWriter(CLIENT_INFO_FILE)){
				CLIENT_INFO_PROPERTIES.store(fileWriter, null);
			}
		}
		
		public String keyString() {
			return stringKey;
		}
		
		public String value() {
			return value;
		}
	}

	private AppInfo() {}
}

