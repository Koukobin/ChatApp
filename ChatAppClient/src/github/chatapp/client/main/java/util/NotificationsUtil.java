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
package github.chatapp.client.main.java.util;

import org.controlsfx.control.Notifications;

import github.chatapp.client.main.java.info.GeneralAppInfo;
import github.chatapp.client.main.java.info.chat_interface.ChatInterfaceInfo;
import javafx.geometry.Pos;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/**
 * @author Ilias Koukovinis
 *
 */
public final class NotificationsUtil {

	private static final MediaPlayer notificationPlayer;

	static {
		Media media = new Media(ChatInterfaceInfo.NOTIFICATION_SOUND_LOCATION);
		notificationPlayer = new MediaPlayer(media);
		notificationPlayer.setCycleCount(Integer.MAX_VALUE); // Playable infinite times
		notificationPlayer.setOnEndOfMedia(() -> {
			// once media is over stop the player so it does not continue to repeat
			notificationPlayer.stop();
		});
	}
	
	private NotificationsUtil() {}
	
	public static void createNotification(String message) {
		
		Notifications.create()
		.title(GeneralAppInfo.TITLE)
		.text(message)
		.hideAfter(Duration.seconds(5))
		.position(Pos.TOP_RIGHT)
		.darkStyle()
		.showInformation();
		
		notificationPlayer.play();
	}
}
