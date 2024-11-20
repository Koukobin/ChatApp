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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.jfoenix.controls.JFXListCell;
import com.jfoenix.controls.JFXListView;

import github.koukobin.ermis.client.main.java.service.client.ChatSession;
import github.koukobin.ermis.client.main.java.service.client.io_client.Client;
import github.koukobin.ermis.client.main.java.util.Threads;
import github.koukobin.ermis.common.message_types.Message;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * @author Ilias Koukovinis
 *
 */
public class ChatsController extends GeneralController {

	@FXML
	private JFXListView<ChatSession> chatSessionsListView;
	
	@FXML
	private TextField searchForChatSessionsTextField;
	
	private JFXListCell<?> lastSelectedCell;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		chatSessionsListView.setOnMouseClicked((MouseEvent e) -> {

			if (!(e.getPickResult().getIntersectedNode() instanceof JFXListCell<?> cell)) {
				return;
			}

			// If the user clicked the same active cell simply return
			if (cell.equals(lastSelectedCell)) {
				return;
			}
			
			if (e.getButton() == MouseButton.PRIMARY /* Left Click */) {
				
				ChatSession chatSession = (ChatSession) cell.getItem();

				RootReferences.getMessagingController().clearMessages();
				if (!chatSession.haveChatMessagesBeenCached()) {
					try {
						Client.getCommands().fetchWrittenText(chatSession.getChatSessionIndex());
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
				} else {
					List<Message> messages = chatSession.getMessages();
					RootReferences.getMessagingController().addMessages(messages.toArray(new Message[0]), 
							chatSession.getChatSessionIndex(),
							getActiveChatSessionIndex());
				}
			} else if (e.getButton() == MouseButton.SECONDARY /* Right Click */) {

				final ContextMenu contextMenu = new ContextMenu();
				MenuItem delete = new MenuItem("Delete");
				contextMenu.getItems().addAll(delete);

				delete.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						try {
							Client.getCommands().deleteChatSession(getActiveChatSessionIndex());
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
				});

				contextMenu.show(cell, e.getScreenX(), e.getScreenY());
			}
			
			lastSelectedCell = cell;
		});
	}

	@FXML
	public void searchChatSession(KeyEvent event) {
		
		String search = searchForChatSessionsTextField.getText();
		
		List<ChatSession> items = chatSessionsListView.getItems();

		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).toString().contains(search)) {
				ChatSession temp = items.get(i);
				items.set(i, items.get(i - 1));
				items.set(i - 1, temp);
			}
		}
		
		// Ensure the list view refreshes
		chatSessionsListView.refresh();
	}
	
	@FXML
	public void refreshChatSessionsListView(ActionEvent event) {
		try {
			Client.getCommands().fetchChatSessions();

			RootReferences.getMessagingController().clearMessages();
			MFXProgressSpinner progressSpinner = new MFXProgressSpinner();
			Button refreshButton = (Button) event.getSource();
			
			refreshButton.setDisable(true);
			getRoot().getChildren().remove(chatSessionsListView);
			getRoot().getChildren().add(progressSpinner);
			Platform.runLater(() -> {
				Threads.delay(1000, () -> {
					refreshButton.setDisable(false);
					getRoot().getChildren().remove(progressSpinner);
					getRoot().getChildren().add(chatSessionsListView);
				});
			});
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void clearChatSessions() {
		chatSessionsListView.getItems().clear();
	}
	
	public void addChatSessions(Iterable<ChatSession> chatSessions) {
		for (ChatSession chatSession : chatSessions) {
			addChatSession(chatSession);
		}
	}
	
	public void addChatSession(ChatSession chatSession) {
		chatSessionsListView.getItems().add(chatSession);
	}
	
	public int getActiveChatSessionIndex() {
		return chatSessionsListView.getSelectionModel().getSelectedIndex();
	}
	
	public ChatSession getActiveChatSession() {
		return chatSessionsListView.getSelectionModel().getSelectedItem();
	}
	
}

