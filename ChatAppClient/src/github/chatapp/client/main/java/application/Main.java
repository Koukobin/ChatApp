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

import github.chatapp.client.main.java.application.chat_interface.ChatInterface;
import github.chatapp.client.main.java.application.starting_screen.StartingScreenInterface;
import javafx.application.Application;
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
		StartingScreenInterface startingScreen = new StartingScreenInterface();
		startingScreen.showAndWait();

		// Start the chat interface
		ChatInterface chatInterface = new ChatInterface(primaryStage, getHostServices());
		chatInterface.start();
	}
}
