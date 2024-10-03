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
package main.java.util.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import main.java.info.GeneralAppInfo;

/**
 * @author Ilias Koukovinis
 *
 */
public class Dialogs {

	private Dialogs() {}
	
	public static Alert createConfirmationDialog() {
		return createConfirmationDialog("Confirmation!", "Are you sure you want to move foward with this action");
	}
	
	public static Alert createConfirmationDialog(String headerText, String contentText) {
		return createConfirmationDialog(headerText, contentText, new ButtonType[0]);
	}
	
	public static Alert createConfirmationDialog(String headerText, String contentText, ButtonType... buttons) {

		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(GeneralAppInfo.TITLE);
		alert.getDialogPane().getScene().getWindow().sizeToScene();
		alert.setHeaderText(headerText);
		addButtons(alert, buttons);
		addIcon(alert, GeneralAppInfo.MAIN_ICON);

		TextArea textArea = new TextArea(contentText);
		textArea.setEditable(false);
		textArea.setWrapText(false);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane content = new GridPane();
		content.setMaxWidth(Double.MAX_VALUE);
		content.add(textArea, 0, 1);
		alert.getDialogPane().setExpandableContent(content);

		return alert;
	}
	
	public static void showInfoDialog(String contentText) {

		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(GeneralAppInfo.TITLE);
		alert.setHeaderText("Info!");
		alert.setContentText(contentText);
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		addIcon(alert, GeneralAppInfo.MAIN_ICON);
		
		alert.showAndWait();
	}
	
	public static void showSuccessDialog(String contentText) {

		// The reason this kind of text area is used to display the text is so the contentText can be copyable
		TextArea textArea = new TextArea(contentText);
		textArea.setEditable(false);
		textArea.setWrapText(true);
		GridPane gridPane = new GridPane();
		gridPane.setMaxWidth(Double.MAX_VALUE);
		gridPane.add(textArea, 0, 0);

		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(GeneralAppInfo.TITLE);
		alert.setHeaderText("Sucess!");
		alert.getDialogPane().setContent(gridPane);
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		addIcon(alert, GeneralAppInfo.MAIN_ICON);
		
		alert.showAndWait();
	}
	
	public static void showErrorDialog(String contentText) {

		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(GeneralAppInfo.TITLE);
		alert.setHeaderText("Error!");
		alert.setContentText(contentText);
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		addIcon(alert, GeneralAppInfo.MAIN_ICON);
		
		alert.showAndWait();
	}
	
	public static void showExceptionDialog(String headerText, String contentText) {
		
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(GeneralAppInfo.TITLE);
		alert.getDialogPane().getScene().getWindow().sizeToScene();
		alert.setHeaderText(headerText);
		addIcon(alert, GeneralAppInfo.MAIN_ICON);

		TextArea textArea = new TextArea(contentText);
		textArea.setEditable(false);
		textArea.setWrapText(false);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane content = new GridPane();
		content.setMaxWidth(Double.MAX_VALUE);
		content.add(textArea, 0, 1);
		alert.getDialogPane().setExpandableContent(content);
		
		alert.showAndWait();
	}

	public static Alert createCustomAlertDialog(String headerText, String contentText, AlertType alertType,
			ButtonType... buttons) {

		Alert alert = new Alert(alertType);
		alert.setTitle(GeneralAppInfo.TITLE);
		alert.setHeaderText(headerText);
		alert.setContentText(contentText);
		addButtons(alert, buttons);
		addIcon(alert, GeneralAppInfo.MAIN_ICON);

		return alert;
	}
	
	public static TextInputDialog createTextInputDialog(String headerText, String contentText, String defaultValue, ButtonType... buttons) {
		
		TextInputDialog serverToConnectToTD = new TextInputDialog(defaultValue);
		serverToConnectToTD.setHeaderText(headerText);
		serverToConnectToTD.setContentText(contentText);
		serverToConnectToTD.setTitle(GeneralAppInfo.TITLE);
		serverToConnectToTD.getDialogPane().getScene().getWindow().sizeToScene();
		addButtons(serverToConnectToTD, buttons);
		addIcon(serverToConnectToTD, GeneralAppInfo.MAIN_ICON);
		
		return serverToConnectToTD;
	}
	
	public static <T> ChoiceDialog<T> createChoiceDialog(String headerText, String contentText, T defaultChoice, T[] choices, ButtonType... buttons){
		
		ChoiceDialog<T> choiceDialog = new ChoiceDialog<>(defaultChoice, choices);
		choiceDialog.setHeaderText(headerText);
		choiceDialog.setContentText(contentText);
		choiceDialog.setTitle(GeneralAppInfo.TITLE);
		choiceDialog.getDialogPane().getScene().getWindow().sizeToScene();
		addButtons(choiceDialog, buttons);
		addIcon(choiceDialog, GeneralAppInfo.MAIN_ICON);
		
		return choiceDialog;
	}
	
	private static void addButtons(Dialog<?> dialog, ButtonType[] buttons) {
		
		if (buttons.length == 0) {
			return;
		}
		
		DialogPane dialogPane = dialog.getDialogPane();
		dialogPane.getButtonTypes().clear();
		dialogPane.getButtonTypes().addAll(buttons);
	}
	
	private static void addIcon(Dialog<?> dialog, Image image) {
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(image);
	}
}