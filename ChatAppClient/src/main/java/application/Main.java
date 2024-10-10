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
package main.java.application;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import main.java.application.starting_screen.StartingScreenInterface;
import main.java.controllers.chat_interface.ChatInterfaceController;
import main.java.info.GeneralAppInfo;
import main.java.info.chat_interface.ChatInterfaceInfo;

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
		startChatInterface(primaryStage);
	}
	
	private void startChatInterface(Stage primaryStage) throws IOException {
		
		FXMLLoader loader = new FXMLLoader(ChatInterfaceInfo.FXML_LOCATION);
		Parent root = loader.load();
		ChatInterfaceController controller = loader.getController();
		controller.setStage(primaryStage);
		controller.setHostServices(getHostServices());

		Scene scene = new Scene(root);
		scene.getStylesheets().add(ChatInterfaceInfo.CSS_LOCATION);
		scene.setFill(Color.TRANSPARENT);

		primaryStage.setOnCloseRequest(e -> {
			controller.closeClient();
			System.exit(0);
		});
		primaryStage.setMinHeight(ChatInterfaceInfo.STAGE_MIN_HEIGHT);
		primaryStage.setMinWidth(ChatInterfaceInfo.STAGE_MIN_WIDTH);
		primaryStage.getIcons().add(GeneralAppInfo.MAIN_ICON);
		primaryStage.setTitle(GeneralAppInfo.TITLE);
		primaryStage.setMaximized(true);
		primaryStage.setScene(scene);

		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		primaryStage.setWidth(screenBounds.getWidth());
		primaryStage.setHeight(screenBounds.getHeight());

		ScaleTransition st = new ScaleTransition(Duration.millis(900), root);
		st.setInterpolator(Interpolator.EASE_OUT);
		st.setFromX(0.8);
		st.setFromY(0.8);
		st.setToX(1.0);
		st.setToY(1.0);
		st.play();

		primaryStage.show();
		primaryStage.requestFocus();
	}
}
