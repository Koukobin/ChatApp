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
package github.chatapp.client.main.java.controllers.chat_interface.dialogs.decide_server_to_connect;

import java.net.MalformedURLException;
import java.net.PortUnreachableException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;

import com.google.common.base.Throwables;

import github.chatapp.client.main.java.databases.ClientDatabase;
import github.chatapp.client.main.java.databases.ServerInfo;
import github.chatapp.client.main.java.dialogs.MFXActionDialog;
import github.chatapp.client.main.java.util.dialogs.Dialogs;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * @author Ilias Koukovinis
 *
 */
class AddServerDialog extends MFXActionDialog {

	private ServerInfo serverInfo;
	
	AddServerDialog(Stage stage, Parent rootPane, ClientDatabase.DBConnection serverInfoDatabaseConnection) {
		
		super(stage, rootPane);
		
		MFXTextField newServerTextField = new MFXTextField();
		newServerTextField.setFloatMode(FloatMode.ABOVE);
		newServerTextField.setPromptText("Server URL");
		newServerTextField.setFont(defaultFont);
		newServerTextField.setPrefColumnCount(15);
		
		dialogContent.setContent(newServerTextField);
		dialogContent.clearActions();
		dialogContent.addActions(
				Map.entry(new MFXButton("Save"), (MouseEvent e) -> {
					try {
						serverInfoDatabaseConnection.addServerInfo(new ServerInfo(new URL(newServerTextField.getText())));
					} catch (PortUnreachableException | UnknownHostException | MalformedURLException x) {
						Dialogs.showExceptionDialog(x.getMessage(), Throwables.getStackTraceAsString(x));
					}
				}),
				Map.entry(new MFXButton("Add"), (MouseEvent e) -> {
					try {

						serverInfo = new ServerInfo(new URL(newServerTextField.getText()));

						isCanceled = false;
						mfxDialog.close();
					} catch (MalformedURLException | UnknownHostException | PortUnreachableException x) {
						Dialogs.showExceptionDialog(x.getMessage(), Throwables.getStackTraceAsString(x));
					}
				}), Map.entry(new MFXButton("Cancel"), (MouseEvent e) -> {
					isCanceled = true;
					mfxDialog.close();
				}));
		
		dialogContent.setHeaderText("Add Server Dialog");
		dialogContent.autosize();
	}

	public ServerInfo getServerInfo() {
		return serverInfo;
	}
}
