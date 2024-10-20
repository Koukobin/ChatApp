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
package github.chatapp.client.main.java.application;

import java.io.IOException;
import java.util.Optional;

import github.chatapp.client.main.java.application.chat_interface.ChatInterface;
import github.chatapp.client.main.java.application.decide_server_to_connect.ChooseServerDialog;
import github.chatapp.client.main.java.application.starting_screen.StartingScreenInterface;
import github.chatapp.client.main.java.database.ServerInfo;
import github.chatapp.client.main.java.service.client.io_client.Client;
import github.chatapp.client.main.java.util.dialogs.CustomDialogButtonTypes;
import github.chatapp.client.main.java.util.dialogs.DialogsUtil;
import javafx.application.Application;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * @author Ilias Koukovinis
 *
 */
public class Main extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		System.setProperty("prism.lcdtext", "false"); // Disable LCD anti-aliasing to improve text clarity

		// Show the starting screen and wait for it to close
		{
			StartingScreenInterface startingScreen = new StartingScreenInterface();
			startingScreen.showAndWait();
		}

		// Decide server to connect to
		{
			boolean retry = false;
			
			do {
				try {
					ChooseServerDialog dialog = new ChooseServerDialog(null, null);
					dialog.showAndWait();
					
					if (dialog.isCanceled()) {
						return;
					}
					
					ServerInfo serverInfo = dialog.getResult();
					
					if (serverInfo == null) {
						DialogsUtil.showErrorDialog("Server info cannot be empty!");
						retry = true;
						continue;
					}

					Client.ServerCertificateVerification verify = dialog.shouldCheckServerCertificate()
							? Client.ServerCertificateVerification.VERIFY
							: Client.ServerCertificateVerification.IGNORE;

					Client.initialize(serverInfo.getAddress(), serverInfo.getPort(), verify);
					
					retry = false;
				} catch (Exception e) {
					
					Optional<ButtonType> exceptionDialogResult = DialogsUtil.showExceptionDialog(e);
					
					if (!exceptionDialogResult.isPresent() || exceptionDialogResult.get() == CustomDialogButtonTypes.RETRY_BUTTON) {
						retry = true;
						continue;
					}
					
					return;
				}
			} while (retry);
		}

		// Start the chat interface
		{
			ChatInterface chatInterface = new ChatInterface(primaryStage, getHostServices());
			chatInterface.start();
		}
	}
}
