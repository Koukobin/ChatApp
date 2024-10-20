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
package github.chatapp.client.main.java.info.chat_interface;

import java.net.URL;

/**
 * @author Ilias Koukovinis
 *
 */
public final class ChatInterfaceInfo {

	public static final URL FXML_LOCATION = ChatInterfaceInfo.class.getResource("/github/chatapp/client/main/resources/view/chat_interface/chat-interface-scene.fxml");
	public static final String CSS_LOCATION = ChatInterfaceInfo.class.getResource("/github/chatapp/client/main/resources/css/chat_interface/chat-interface.css").toExternalForm();
	public static final String NOTIFICATION_SOUND_LOCATION = ChatInterfaceInfo.class.getResource("/github/chatapp/client/main/resources/sounds/notification.wav").toExternalForm();
	
	public static final int STAGE_MIN_HEIGHT = 550;
	public static final int STAGE_MIN_WIDTH = 550;

	private ChatInterfaceInfo() {}
}
