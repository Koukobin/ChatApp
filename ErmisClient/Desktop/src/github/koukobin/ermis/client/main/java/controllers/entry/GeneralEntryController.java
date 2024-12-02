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
package github.koukobin.ermis.client.main.java.controllers.entry;

import github.koukobin.ermis.client.main.java.service.client.io_client.Client;
import github.koukobin.ermis.client.main.java.util.MemoryUtil;
import github.koukobin.ermis.client.main.java.util.dialogs.DialogsUtil;
import github.koukobin.ermis.common.entry.EntryType;
import github.koukobin.ermis.common.entry.EntryType.CredentialInterface;
import github.koukobin.ermis.common.results.ResultHolder;

import java.io.IOException;
import java.util.Map;

import com.jfoenix.controls.JFXCheckBox;

import javafx.application.Platform;
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
	
	protected FXMLLoader originalFXMLLoader;
	
	@FXML
	protected TextField emailTextField;
	@FXML
	protected TextField passwordFieldTextVisible;
	@FXML
	protected PasswordField passwordFieldTextHidden;
	@FXML
	protected JFXCheckBox changePasswordVisibilityCheckBox;
	
	public void closeEntry(ActionEvent event) {
		Platform.runLater(() -> {
			Node node = (Node) event.getSource();
			Stage stage = (Stage) node.getScene().getWindow();
			stage.hide();
		});
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
	public abstract void switchScene(ActionEvent event)  throws IOException;
	
	@FXML
	public abstract void register(ActionEvent event)  throws IOException;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected boolean sendAndValidateCredentials(Client.Entry clientEntry, Map<? extends CredentialInterface, String> credentials) throws IOException {

		clientEntry.sendCredentials(credentials);

		// Clear sensitive data from memory
		for (Map.Entry<?, String> entry : credentials.entrySet()) {
			MemoryUtil.freeStringFromMemory(entry.getValue());
		}

		ResultHolder entryResult = clientEntry.getResult();
		boolean isSuccessful = entryResult.isSuccessful();
		String resultMessage = entryResult.getResultMessage();

		if (!isSuccessful) {
			DialogsUtil.showErrorDialog(resultMessage);
		}
	
		return isSuccessful;
	}
	
	protected boolean performVerification() throws IOException {
		
		Client.VerificationEntry verificationEntry = Client.createNewVerificationEntry();
		VerificationDialog verificationDialog = new VerificationDialog(verificationEntry);
		ResultHolder entryResult = null;
		
		boolean isSuccessful = false;
		
		while (!verificationEntry.isVerificationComplete()) {

			verificationDialog.showAndWait();
			verificationEntry.sendVerificationCode(verificationDialog.getVerificationCode());

			entryResult = verificationEntry.getResult();
			isSuccessful = entryResult.isSuccessful();
			String resultMessage = entryResult.getResultMessage();

			if (isSuccessful) {
				DialogsUtil.showSuccessDialog(resultMessage);
				break;
			}

			DialogsUtil.showErrorDialog(resultMessage);
		}

		return isSuccessful;
	}
	
	public void setFXMLLoader(FXMLLoader loader) {
		this.originalFXMLLoader = loader;
	}
	
	public String getEmail() {
		return emailTextField.getText();
	}
	
	public String getPassword() {
		return changePasswordVisibilityCheckBox.isSelected() ? passwordFieldTextVisible.getText()
				: passwordFieldTextHidden.getText();
	}
	
	public EntryType getRegistrationType() {
		return registrationType;
	}
	
	public GeneralEntryController getController() {
		return originalFXMLLoader.getController();
	}
}