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
package github.koukobin.ermis.client.main.java.controllers.chat_interface;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.jfoenix.controls.JFXButton;

import github.koukobin.ermis.client.main.java.info.Icons;
import github.koukobin.ermis.client.main.java.info.chat_interface.SettingsInfo;
import github.koukobin.ermis.client.main.java.service.client.io_client.Client;
import github.koukobin.ermis.client.main.java.util.UITransitions;
import github.koukobin.ermis.client.main.java.util.UITransitions.Direction.Which;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import javafx.animation.Interpolator;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

/**
 * @author Ilias Koukovinis
 *
 */
public class AccountSettingsController extends GeneralController {

	@FXML
	private ImageView addAccountIconImage;
	
	@FXML
	private Label clientIDLabel;
	
	@FXML
	private TextField changeDisplayNameTextField;
	
	@FXML
	private JFXButton changeDisplayNameButton;
	
	@FXML
	private ImageView displayNameButtonImageView;
	
	@FXML
	private HBox changeDisplayNameHbox;

	@FXML
	private MFXPasswordField changePasswordField;
	
	@FXML
	private JFXButton changePasswordButton;
	
	@FXML
	private ImageView passwordButtonImageView;
	
	@FXML
	private HBox changePasswordHbox;
	
	private int count = 0;
	
	private int count1 = 0;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		clientIDLabel.setText(String.valueOf(Client.getClientID()));
		
		changeDisplayNameTextField.setText(Client.getDisplayName());
		disableDisplayNameTextField();
		disablePasswordTextField();

		EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				
				enableDisplayNameTextField();
				
				changeDisplayNameButton.setOnAction(new EventHandler<ActionEvent>() {
					
					@Override
					public void handle(ActionEvent event) {
						
						String newDisplayName = changeDisplayNameTextField.getText();
						
						if (newDisplayName.isBlank()) {
							return;
						}
						
						try {
							Client.getCommands().changeDisplayName(newDisplayName);
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
				});
				
				// Listener to disable textfield and remove CSS once it loses focus
				ChangeListener<Boolean> focusListener = new ChangeListener<>() {

					private boolean set = false;
					
					@Override
					public void changed(ObservableValue<? extends Boolean> observable, 
							Boolean oldValue,
							Boolean newValue) {

						if (Boolean.TRUE.equals(newValue)) {
							return;
						}
						
						if (!set) {
							count++;
							set = true;
						}
						
						if (count == 2) {
							disableDisplayNameTextField();
							changeDisplayNameTextField.focusedProperty().removeListener(this);
							changeDisplayNameButton.focusedProperty().removeListener(this);
							changeDisplayNameButton.setOnAction((event) -> handle(event));
							count = 0;
						}
					}
				};
				
				// Listener to disable textfield and remove CSS once it loses focus
				ChangeListener<Boolean> focusListener2 = new ChangeListener<>() {

					private boolean set = false;
					
					@Override
					public void changed(ObservableValue<? extends Boolean> observable, 
							Boolean oldValue,
							Boolean newValue) {

						if (Boolean.TRUE.equals(newValue)) {
							return;
						}
						
						if (!set) {
							count++;
							set = true;
						}
					}
				};
				
				changeDisplayNameButton.focusedProperty().addListener(focusListener);
				changeDisplayNameTextField.focusedProperty().addListener(focusListener2);
			}
		};
		
		changeDisplayNameButton.setOnAction(handler);
		
		EventHandler<ActionEvent> handler2 = new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				
				enablePasswordTextField();
				
				changePasswordButton.setOnAction(new EventHandler<ActionEvent>() {
					
					@Override
					public void handle(ActionEvent event) {
						
						String newPassword = changePasswordField.getText();
						
						if (newPassword.isBlank()) {
							return;
						}
						
						try {
							Client.getCommands().changePassword(newPassword);
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
				});
				
				// Listener to disable textfield and remove CSS once it loses focus
				ChangeListener<Boolean> focusListener = new ChangeListener<>() {

					private boolean set = false;
					
					@Override
					public void changed(ObservableValue<? extends Boolean> observable, 
							Boolean oldValue,
							Boolean newValue) {

						if (Boolean.TRUE.equals(newValue)) {
							return;
						}
						
						if (!set) {
							count1++;
							set = true;
						}
						
						if (count1 == 2) {
							disablePasswordTextField();
							changePasswordField.focusedProperty().removeListener(this);
							changePasswordButton.focusedProperty().removeListener(this);
							changePasswordButton.setOnAction((event) -> handle(event));
							count1 = 0;
						}
					}
				};
				
				// Listener to disable textfield and remove CSS once it loses focus
				ChangeListener<Boolean> focusListener2 = new ChangeListener<>() {

					private boolean set = false;
					
					@Override
					public void changed(ObservableValue<? extends Boolean> observable, 
							Boolean oldValue,
							Boolean newValue) {

						if (Boolean.TRUE.equals(newValue)) {
							return;
						}
						
						if (!set) {
							count1++;
							set = true;
						}
					}
				};
				
				changePasswordButton.focusedProperty().addListener(focusListener);
				changePasswordField.focusedProperty().addListener(focusListener2);
			}
		};
		
		changePasswordButton.setOnAction(handler2);
	}
	
	private void enableDisplayNameTextField() {
		enableTextField(changeDisplayNameHbox, changeDisplayNameTextField, displayNameButtonImageView);
	}
	
	private void disableDisplayNameTextField() {
		disableTextField(changeDisplayNameHbox, changeDisplayNameTextField, displayNameButtonImageView);
	}
	
	private void enablePasswordTextField() {
		enableTextField(changePasswordHbox, changePasswordField, passwordButtonImageView);
	}
	
	private void disablePasswordTextField() {
		disableTextField(changePasswordHbox, changePasswordField, passwordButtonImageView);
	}
	
	private static void enableTextField(HBox hbox, TextField textField, ImageView textFieldButtonImageView) {
		textField.setDisable(false);
		textField.setEditable(true);
		textField.setFocusTraversable(true);
		textField.requestFocus();
		hbox.getStylesheets().add(SettingsInfo.AccountSettings.ACCOUNT_SETTINGS_FOCUSED_CSS_LOCATION);
		textFieldButtonImageView.setImage(Icons.CHECK);
	}
	
	private static void disableTextField(HBox hbox, TextField textField, ImageView textFieldButtonImageView) {
		textField.setDisable(true);
		textField.setEditable(false);
		textField.setFocusTraversable(false);
		hbox.getStylesheets().remove(SettingsInfo.AccountSettings.ACCOUNT_SETTINGS_FOCUSED_CSS_LOCATION);
		textFieldButtonImageView.setImage(Icons.EDIT);
	}
	
	public void setIcon(byte[] icon) {
		addAccountIconImage.setImage(new Image(new ByteArrayInputStream(icon)));
	}
	
	@FXML
	public void addAccountIcon(ActionEvent event) throws IOException {
		
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Add account icon");
		File iconFile = fileChooser.showOpenDialog(getStage());

		if (iconFile == null) {
			return;
		}
		
		Client.getCommands().addAccountIcon(iconFile);
	}
	
	@FXML
	public void transitionBackToPlainSettings(ActionEvent event) {

		Runnable transition = new UITransitions.Builder()
				.setDirection(UITransitions.Direction.XAxis.LEFT_TO_RIGHT)
				.setDuration(Duration.seconds(0.5))
				.setInterpolator(Interpolator.EASE_BOTH)
				.setNewComponent(RootReferences.getSettingsRoot())
				.setOldComponent(getRoot())
				.setParentContainer((StackPane) getRoot().getParent())
				.setWhich(Which.OLD)
				.build();
		
		transition.run();
	}
}
