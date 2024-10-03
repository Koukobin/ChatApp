/* Copyright (C) 2022 Ilias Koukovinis <ilias.koukovinis@gmail.com>
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
package main.java.controllers.entry;

import org.chatapp.commons.entry.EntryType;

import com.jfoenix.controls.JFXCheckBox;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * @author Ilias Koukovinis
 * 
 */
public abstract sealed class GeneralEntryController implements Initializable permits LoginSceneController, CreateAccountSceneController {

	protected EntryType registrationType;
	private String email;
	private String password;
	
	protected FXMLLoader originalFXMLLoader;
	
	@FXML
	protected TextField emailTextField;
	@FXML
	protected TextField passwordFieldTextVisible;
	@FXML
	protected PasswordField passwordFieldTextHidden;
	@FXML
	protected JFXCheckBox changePasswordVisibilityCheckBox;
	
	@FXML
	public void proceed(ActionEvent event) {
		
		Node source = (Node) event.getSource();
		Stage stage = (Stage) source.getScene().getWindow();
		stage.close();
		
		email = emailTextField.getText();
		password = changePasswordVisibilityCheckBox.isSelected() ? passwordFieldTextVisible.getText()
				: passwordFieldTextHidden.getText();
	}

	@FXML
	public void changePasswordVisibility(ActionEvent event) {

		if (changePasswordVisibilityCheckBox.isSelected()) {
			passwordFieldTextVisible.setText(passwordFieldTextHidden.getText());
			passwordFieldTextHidden.setVisible(false);
			passwordFieldTextVisible.setVisible(true);
			return;
		}

		passwordFieldTextHidden.setText(passwordFieldTextVisible.getText());
		passwordFieldTextVisible.setVisible(false);
		passwordFieldTextHidden.setVisible(true);
	}
	
	@FXML
	public abstract void switchScene(ActionEvent event) throws Exception;
	
	
	public void setFXMLLoader(FXMLLoader loader) {
		this.originalFXMLLoader = loader;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getPassword() {
		return password;
	}
	
	public EntryType getRegistrationType() {
		return registrationType;
	}
	
	public GeneralEntryController getController() {
		return originalFXMLLoader.getController();
	}
}