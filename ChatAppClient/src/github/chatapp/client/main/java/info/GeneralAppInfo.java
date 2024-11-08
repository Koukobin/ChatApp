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
package github.chatapp.client.main.java.info;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.lang3.SystemUtils;

/**
 * @author Ilias Koukovinis
 *
 */
public final class GeneralAppInfo {

	public static final String GENERAL_NAME = "ChatApp";
	public static final String TITLE = GENERAL_NAME + "-Client";

	public static final String VERSION = "1.0-rc";
	
	public static final String CLIENT_DATABASE_PATH;
	public static final String SOURCE_CODE_HTML_PAGE_URL = "https://github.com/Koukobin/ChatApp";

	public static final String MAIN_PROJECT_PATH = "/github/chatapp/client/main/";
	
	static {

		String appDataFolder;

		if (SystemUtils.IS_OS_WINDOWS) {
			appDataFolder = System.getProperty("user.name") + "\\AppData\\Local\\";
		} else if (SystemUtils.IS_OS_LINUX) {
			appDataFolder = System.getProperty("user.home") + "/.";
		} else {
			throw new RuntimeException("Unknown OS type");
		}

		appDataFolder = appDataFolder + TITLE.toLowerCase() + File.separator;
		try {
			// Using Files.createDirectories instead of Files.createDirectory ensures that
			// all parent directories are created if they don't exist
			Files.createDirectories(Paths.get(appDataFolder));
		} catch (FileAlreadyExistsException faee) {
			// If directory already exists, simply move on.
		} catch (IOException e) {
			// print other I/O exceptions
			e.printStackTrace();
		}
		
		CLIENT_DATABASE_PATH = appDataFolder + "Client.db";
	}
	
	private GeneralAppInfo() {}
}
