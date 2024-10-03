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
package main.java.application.starting_screen;

import java.util.Arrays;

import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import main.java.info.GeneralAppInfo;
import main.java.info.starting_screen.StartingScreenInfo;

/**
 * @author Ilias Koukovinis
 *
 */
public class StartingScreenInterface {

	private Stage stage;

	public StartingScreenInterface() {

		ImageView icon = new ImageView(GeneralAppInfo.MAIN_ICON);
		icon.setFitWidth(StartingScreenInfo.ICON_WITDH);
		icon.setFitHeight(StartingScreenInfo.ICON_HEIGHT);
		icon.setPreserveRatio(false);

		Label label = new Label(GeneralAppInfo.GENERAL_NAME);
		label.setGraphic(icon);
		label.setContentDisplay(ContentDisplay.CENTER);

		MFXProgressSpinner spinner = new MFXProgressSpinner();
		BorderPane root = new BorderPane();

		Scene scene = new Scene(root, StartingScreenInfo.STAGE_WIDTH, StartingScreenInfo.STAGE_HEIGHT);
		scene.setFill(Color.TRANSPARENT);
		scene.getStylesheets().add(StartingScreenInfo.CSS_LOCATION);

		BorderPane.setAlignment(spinner, Pos.CENTER);
		BorderPane.setMargin(spinner, new Insets(0, 0, 50, 0));
		root.setCenter(label);
		root.setBottom(spinner);

		stage = new Stage();
		stage.initStyle(StageStyle.TRANSPARENT); // Removes window decorations
		stage.getIcons().add(GeneralAppInfo.MAIN_ICON);
		stage.setScene(scene);
		stage.setOpacity(1.0f);
	}

	public void show() {
		stage.show();
	}

	public void showAndWait() {
			
		stage.getScene().getRoot().setOpacity(0.0);

		Timeline timeline = new Timeline();
		KeyFrame key = new KeyFrame(Duration.millis(550),
				new KeyValue(stage.getScene().getRoot().opacityProperty(), 1));
		timeline.getKeyFrames().add(key);
		timeline.setOnFinished(e -> delay(3000, stage::close));
		timeline.play();

		stage.showAndWait();
	}

	private static void delay(long millis, Runnable... continuation) {

		Task<Void> sleeper = new Task<>() {
			@Override
			protected Void call() throws Exception {
				Thread.sleep(millis);
				return null;
			}
		};
		sleeper.setOnSucceeded(event -> Arrays.stream(continuation).forEach(Runnable::run));

		Thread sleeperThread = new Thread(sleeper);
		sleeperThread.setDaemon(true);
		sleeperThread.start();
	}
}
