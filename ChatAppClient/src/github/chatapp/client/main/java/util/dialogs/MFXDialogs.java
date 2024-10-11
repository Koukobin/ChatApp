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
package github.chatapp.client.main.java.util.dialogs;

import java.util.Map;

import github.chatapp.client.main.java.dialogs.MFXDialog;
import github.chatapp.client.main.java.info.GeneralAppInfo;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.enums.ScrimPriority;
import io.github.palexdev.materialfx.font.MFXFontIcon;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @author Ilias Koukovinis
 *
 */
public final class MFXDialogs {

	private MFXDialogs() {}
	
	public static MFXGenericDialog createMFXGenericDialog(String contentText) {
		return MFXGenericDialogBuilder.build()
				.setContentText(contentText)
				.makeScrollable(true)
				.get();
	}
	
	public static MFXStageDialog createMFXStageDialog(Stage stage, MFXGenericDialog dialogContent, Pane ownerNode) {
		
		MFXStageDialog dialog = MFXGenericDialogBuilder.build(dialogContent)
				.toStageDialogBuilder()
				.initOwner(stage)
				.initModality(Modality.APPLICATION_MODAL)
				.setDraggable(true)
				.setTitle(GeneralAppInfo.TITLE)
				.setOwnerNode(ownerNode)
				.setScrimPriority(ScrimPriority.WINDOW)
				.setScrimOwner(true)
				.get();

		dialog.getIcons().add(GeneralAppInfo.MAIN_ICON);
		
		return dialog;
	}

	public static void showSimpleInformationDialog(Stage stage, Pane ownerNode, String contentText) {

		MFXDialog dialog = new MFXDialog(stage, ownerNode);
		dialog.setContentText(contentText);
		dialog.setHeaderText("Information");
		dialog.addActions(Map.entry(new MFXButton("Ok"), event -> dialog.close()));

		MFXFontIcon infoIcon = new MFXFontIcon("mfx-info-circle-filled", 18);
		dialog.setHeaderIcon(infoIcon);
		dialog.addStyleClass("mfx-info-dialog");

		dialog.showAndWait();
	}
}