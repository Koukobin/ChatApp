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

import org.apache.commons.lang3.SystemUtils;

import javafx.scene.image.Image;

/**
 * @author Ilias Koukovinis
 *
 */
public final class GeneralAppInfo {

	public static final String GENERAL_NAME = "ChatApp";
	public static final String TITLE = GENERAL_NAME + "-Client";

	public static final Image MAIN_ICON;

	public static final String CLIENT_DATABASE_PATH;
	public static final String CLIENT_INFO_PATH;
	public static final String SOURCE_CODE_HTML_PAGE_URL = "https://github.com/Koukobin/ChatApp";

	public static final String DARK_THEME_CSS = GeneralAppInfo.class
			.getResource("/github/chatapp/client/main/resources/css/mfx_dialogs/dark-theme.css")
			.toExternalForm();

	static {

		String appInstallationFolder;

		if (SystemUtils.IS_OS_WINDOWS) {
			appInstallationFolder = "C:\\Program Files (x86)\\";
		} else if (SystemUtils.IS_OS_LINUX) {
			appInstallationFolder = "/opt/";
		} else {
			throw new RuntimeException("Unknown OS type");
		}

		appInstallationFolder = appInstallationFolder + TITLE + File.separator;
		
		String mainIconPath = "file:" + appInstallationFolder + "icons" + File.separator + TITLE + ".png";
		
		MAIN_ICON = new Image(mainIconPath);
		
		String appInfoFolder = appInstallationFolder + "info" + File.separator;
		
		CLIENT_DATABASE_PATH = appInfoFolder + "Client.db";
		CLIENT_INFO_PATH = appInfoFolder + "chatapp-info.txt";
	}
	
	private GeneralAppInfo() {}
}
