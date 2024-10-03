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
package main.java.dialogs;

import java.util.Map;

import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.dialogs.MFXStageDialogBuilder;
import io.github.palexdev.materialfx.enums.ScrimPriority;
import io.github.palexdev.materialfx.font.MFXFontIcon;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.java.info.GeneralAppInfo;

/**
 * @author Ilias Koukovinis
 *
 */
public class MFXDialog {

	protected static final Font defaultFont = Font.font(14);
	
	private static final BoxBlur blur = new BoxBlur(3, 3, 3);
	
	protected final MFXGenericDialog dialogContent;
	protected final MFXStageDialog mfxDialog;
	
	public MFXDialog(Stage stage, Parent rootPane) {
		
		dialogContent = MFXGenericDialogBuilder.build()
				.makeScrollable(true)
				.get();
		
		MFXStageDialogBuilder mfxDialogBuilder = MFXGenericDialogBuilder.build(dialogContent)
				.toStageDialogBuilder()
				.initModality(Modality.APPLICATION_MODAL)
				.setDraggable(true)
				.setTitle(GeneralAppInfo.TITLE)
				.setOwnerNode((Pane) rootPane)
				.setScrimPriority(ScrimPriority.NODE)
				.setScrimOwner(true);
		
		if (stage != null && rootPane != null) {
			mfxDialogBuilder.initOwner(stage);
			mfxDialogBuilder.setOnHidden(e -> rootPane.setEffect(null));
			mfxDialogBuilder.setOnShown(e -> rootPane.setEffect(blur));
		}
		
		mfxDialog = mfxDialogBuilder.get();
		mfxDialog.getIcons().add(GeneralAppInfo.MAIN_ICON);
	}

	@SafeVarargs
	public final void addActions(Map.Entry<Node, EventHandler<MouseEvent>>... actions) {
		dialogContent.addActions(actions);
	}
	
	public void setHeaderText(String headerText) {
		dialogContent.setHeaderText(headerText);
	}
	
	public void setContent(Node node) {
		dialogContent.setContent(node);
	}
	
	public void setContentText(String text) {
		dialogContent.setContentText(text);
	}
	
	public void setHeaderIcon(MFXFontIcon infoIcon) {
		dialogContent.setHeaderIcon(infoIcon);
	}
	
	public void addStyleClass(String style) {
		dialogContent.getStyleClass().add(style);
	}
	
	public void showAndWait() {
		mfxDialog.showAndWait();
	}
	
	public void close() {
		mfxDialog.close();
	}
}
