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
package github.chatapp.client.main.java.application.decide_server_to_connect;

import java.util.Map;

import github.chatapp.client.main.java.database.ClientDatabase;
import github.chatapp.client.main.java.database.ServerInfo;
import github.chatapp.client.main.java.general_dialogs.MFXDialog;
import github.chatapp.client.main.java.info.GeneralAppInfo;
import github.chatapp.client.main.java.info.choose_server_dialog.ChooseServerDialogInfo;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.materialfx.controls.MFXToggleButton;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.materialfx.enums.ScrimPriority;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * @author Ilias Koukovinis
 *
 */
public class ChooseServerDialog extends MFXDialog {

	private ClientDatabase.DBConnection serverInfoDatabaseConnection = ClientDatabase.createDBConnection();
	private ServerInfo serverInfo;

	private boolean checkServerCertificate = true;
	
	private boolean isCanceled = true;
	
	public ChooseServerDialog(Stage stage, Parent rootPane) {
		super(stage, rootPane);
		
		MFXComboBox<ServerInfo> mfxComboBox = new MFXComboBox<>();
		mfxComboBox.setFloatMode(FloatMode.ABOVE);
		mfxComboBox.setPromptText("Server URL");
		mfxComboBox.setFont(defaultFont);
		mfxComboBox.setPrefHeight(50);
		mfxComboBox.setPrefColumnCount(20);
		mfxComboBox.getItems().addAll(serverInfoDatabaseConnection.getServerInfos());
		
		{
			MFXContextMenu contextMenu = mfxComboBox.getMFXContextMenu();
			MFXContextMenuItem deleteServerURL = new MFXContextMenuItem("Delete");
			deleteServerURL.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					
					ServerInfo serverInfo = mfxComboBox.getSelectedItem();

					if (serverInfo != null) {
						serverInfoDatabaseConnection.removeServerInfo(serverInfo);
						mfxComboBox.getItems().remove(mfxComboBox.getSelectedIndex());
					}
				}
			});
			
			contextMenu.getItems().add(deleteServerURL);
		}
		
		MFXToggleButton toggleCheckServerCertificateButton = new MFXToggleButton("Check server certificate");
		toggleCheckServerCertificateButton.setSelected(true);
		
		dialogContent.setContent(mfxComboBox);
		dialogContent.addActions(
				Map.entry(new MFXButton("Add"), (MouseEvent e) -> {
					
					AddServerDialog dialog = new AddServerDialog(this, super.getScene().getRoot(), serverInfoDatabaseConnection);
					dialog.showAndWait();
					
					if (dialog.isCanceled()) {
						return;
					}

					ServerInfo serverInfo = dialog.getServerInfo();

					if (!mfxComboBox.getItems().contains(serverInfo)) {
						mfxComboBox.getItems().add(serverInfo);
					}
				}), Map.entry(toggleCheckServerCertificateButton, (MouseEvent e) -> {
					checkServerCertificate = !checkServerCertificate;
				}), Map.entry(new MFXButton("Connect"), (MouseEvent e) -> {
					
					serverInfo = mfxComboBox.getSelectedItem();
					
					isCanceled = false;
					super.close();
				}), Map.entry(new MFXButton("Cancel"), (MouseEvent e) -> {
					super.close();
				}));
		
		super.setWidth(ChooseServerDialogInfo.STAGE_WIDTH);
		super.setHeight(ChooseServerDialogInfo.STAGE_HEIGHT);
		super.setScrimPriority(ScrimPriority.WINDOW);
		dialogContent.setHeaderText("Choose server to connect to");
		
		ImageView headerIcon = new ImageView(GeneralAppInfo.MAIN_ICON);
		headerIcon.setFitWidth(30);
		headerIcon.setFitHeight(30);
		dialogContent.setHeaderIcon(headerIcon);
		
        super.getContent().getStylesheets().add(GeneralAppInfo.DARK_THEME_CSS);
	}

	@Override
	public void showAndWait() {
		try {
			super.showAndWait();
		} finally {
			serverInfoDatabaseConnection.close();
		}
	}
	
	public boolean isCanceled() {
		return isCanceled;
	}

	public ServerInfo getResult() {
		return serverInfo;
	}
	
	public boolean shouldCheckServerCertificate() {
		return checkServerCertificate;
	}
}
