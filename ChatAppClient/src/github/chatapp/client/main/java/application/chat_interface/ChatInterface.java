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
package github.chatapp.client.main.java.application.chat_interface;

import java.io.IOException;

import github.chatapp.client.main.java.controllers.chat_interface.ChatInterfaceController;
import github.chatapp.client.main.java.info.GeneralAppInfo;
import github.chatapp.client.main.java.info.chat_interface.ChatInterfaceInfo;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * @author Ilias Koukovinis
 *
 */
public class ChatInterface {
	
	private Stage stage;
	
	public ChatInterface(Stage primaryStage, HostServices hostServices) throws IOException {
		
		this.stage = primaryStage;
		
		FXMLLoader loader = new FXMLLoader(ChatInterfaceInfo.FXML_LOCATION);
		Parent root = loader.load();
		ChatInterfaceController controller = loader.getController();
		controller.setStage(stage);
		controller.setHostServices(hostServices);

		Scene scene = new Scene(root);
		scene.getStylesheets().add(ChatInterfaceInfo.CSS_LOCATION);
		scene.setFill(Color.TRANSPARENT);

		stage.setOnCloseRequest((WindowEvent e) -> {
			controller.closeClient();
			stage.close();
		});
		stage.setMinHeight(ChatInterfaceInfo.STAGE_MIN_HEIGHT);
		stage.setMinWidth(ChatInterfaceInfo.STAGE_MIN_WIDTH);
		stage.getIcons().add(GeneralAppInfo.MAIN_ICON);
		stage.setTitle(GeneralAppInfo.TITLE);
		stage.setMaximized(true);
		stage.setScene(scene);

		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		stage.setWidth(screenBounds.getWidth());
		stage.setHeight(screenBounds.getHeight());

		ScaleTransition st = new ScaleTransition(Duration.millis(900), root);
		st.setInterpolator(Interpolator.EASE_OUT);
		st.setFromX(0.8);
		st.setFromY(0.8);
		st.setToX(1.0);
		st.setToY(1.0);
		st.play();
	}
	
	public void start() {
		stage.show();
		stage.requestFocus();
	}
	
}