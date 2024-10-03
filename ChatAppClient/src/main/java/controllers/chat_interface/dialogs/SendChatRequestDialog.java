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
package main.java.controllers.chat_interface.dialogs;

import java.util.Map;

import com.google.common.base.Throwables;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.materialfx.font.MFXFontIcon;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import main.java.dialogs.MFXActionDialog;
import main.java.util.dialogs.MFXDialogs;

/**
 * @author Ilias Koukovinis
 *
 */
public class SendChatRequestDialog extends MFXActionDialog {

	private int clientID;
	
	public SendChatRequestDialog(Stage stage, Parent rootPane)	{

		super(stage, rootPane);

		MFXTextField friendRequestTextField = new MFXTextField();
		friendRequestTextField.setFloatMode(FloatMode.ABOVE);
		friendRequestTextField.setPromptText("User ClientID");
		friendRequestTextField.setFont(defaultFont);
		friendRequestTextField.setPrefColumnCount(14);
		
		dialogContent.setContent(friendRequestTextField);
		dialogContent.clearActions();
		dialogContent.addActions(
				Map.entry(new MFXButton("Confirm"), (MouseEvent e) -> {
					try {

						clientID = Integer.parseInt(friendRequestTextField.getText());
						
						isCanceled = false;
						mfxDialog.close();
					} catch (NumberFormatException nfe) {
						MFXDialogs.showSimpleInformationDialog(null, null, Throwables.getStackTraceAsString(nfe));
					}
				}), 
				Map.entry(new MFXButton("Cancel"), (MouseEvent e) -> {
					isCanceled = true;
					mfxDialog.close();
				}));

		MFXFontIcon infoIcon = new MFXFontIcon("mfx-info-circle-filled", 18);
		dialogContent.setHeaderText("Send Chat Request Dialog");
		dialogContent.setHeaderIcon(infoIcon);
		dialogContent.getStyleClass().add("mfx-info-dialog");
	}
	
	public int getFriendRequest() {
		return clientID;
	}
}
