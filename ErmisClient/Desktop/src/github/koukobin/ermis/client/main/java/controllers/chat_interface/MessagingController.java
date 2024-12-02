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

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ResourceBundle;

import com.jfoenix.controls.JFXButton;

import github.koukobin.ermis.client.main.java.context_menu.MyContextMenuItem;
import github.koukobin.ermis.client.main.java.info.Icons;
import github.koukobin.ermis.client.main.java.service.client.io_client.Client;
import github.koukobin.ermis.client.main.java.util.ContextMenusUtil;
import github.koukobin.ermis.client.main.java.util.NotificationsUtil;
import github.koukobin.ermis.client.main.java.util.dialogs.MFXDialogsUtil;
import github.koukobin.ermis.common.message_types.ContentType;
import github.koukobin.ermis.common.message_types.Message;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * @author Ilias Koukovinis
 *
 */
public class MessagingController extends GeneralController {

	@FXML
	private VBox messagingBox;
	
	@FXML
	private MFXScrollPane chatBoxScrollpane;
	
	@FXML
	private TextField inputField;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// This basically retrieves more messages from the conversation when it reaches the top of the conversation
		chatBoxScrollpane.setOnScroll(new EventHandler<ScrollEvent>() {

			private Instant lastTimeRequrestedMoreMessages = Instant.EPOCH;

			@Override
			public void handle(ScrollEvent event) {

				long elapsedSeconds = Instant.now().getEpochSecond() - lastTimeRequrestedMoreMessages.getEpochSecond();

				// Have a time limit since if user sends to many requests to get messages then
				// he will probably crash by the enormous amount of messages sent to him
				if (elapsedSeconds < 3) {
					return;
				}

				// When it reaches the top of the scroll pane get more written messages
				if (Double.compare(chatBoxScrollpane.getVvalue(), chatBoxScrollpane.getVmin()) == 0) {
					try {
						Client.getCommands().fetchWrittenText(RootReferences.getChatsController().getActiveChatSessionIndex());
						lastTimeRequrestedMoreMessages = Instant.now();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
				
			}
		});
	}
	
	public void addMessages(Message[] messages, int chatSessionIndex, int activeChatSessionIndex) {
		for (int i = 0; i < messages.length; i++) {
			addMessage(messages[i], chatSessionIndex, activeChatSessionIndex);
		}
	}
	
	public void addMessage(Message message, int chatSessionIndex, int activeChatSessionIndex) {
		printToMessageArea(message, chatSessionIndex, activeChatSessionIndex);
	}
	
	private HBox createClientMessage(Message message) {
		
		ContentType contentType = message.getContentType();
		
		Instant instant = Instant.ofEpochMilli(message.getTimeWritten());
		ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());

		DateTimeFormatter dateFormat = new DateTimeFormatterBuilder()
				.appendValue(HOUR_OF_DAY, 2)
				.appendLiteral(':')
				.appendValue(MINUTE_OF_HOUR, 2)
				.toFormatter();

		Label timeLabel = new Label();
		timeLabel.setText(zonedDateTime.format(dateFormat));
		
		Label messageLabel = new Label();
		
		HBox hbox = new HBox();

		int clientID = message.getClientID();
		
		// In case that this message is the user's, the message label differs
		if (clientID == Client.getClientID()) {
			messageLabel.setId("userMessageLabel");
			hbox.setAlignment(Pos.BOTTOM_RIGHT);
			hbox.getChildren().add(messageLabel);
			hbox.getChildren().add(timeLabel);
		} else {
			messageLabel.setId("otherMessageLabel");
			hbox.setAlignment(Pos.BOTTOM_LEFT);
			hbox.getChildren().add(timeLabel);
			hbox.getChildren().add(messageLabel);
		}
		
		MyContextMenuItem delete = new MyContextMenuItem("Delete");
		delete.setOnAction(e -> {
			try {
				Client.getCommands().deleteMessage(RootReferences.getChatsController().getActiveChatSessionIndex(), message.getMessageID());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		});
		
		MyContextMenuItem copy = new MyContextMenuItem("Copy");
		copy.setOnAction((e) -> {
			
			Clipboard clipboard = Clipboard.getSystemClipboard();
			ClipboardContent clipboardContent = new ClipboardContent();
			
			clipboardContent.putString(messageLabel.getText());
			clipboard.setContent(clipboardContent);
		});
		
		ContextMenusUtil.installContextMenu(messageLabel, delete, copy);
		
		switch(contentType) {
		case TEXT -> {
			messageLabel.setText(messageLabel.getText() + new String(message.getText()));
		}
		case FILE, IMAGE -> {
			
			String fileName = new String(message.getFileName());
			messageLabel.setText(messageLabel.getText() + fileName);

			JFXButton downloadButton = new JFXButton();
			downloadButton.setId("downloadFileButton");
	        downloadButton.managedProperty().bind(messageLabel.textProperty().isEmpty().not());
	        downloadButton.setFocusTraversable(false);
	        downloadButton.setPadding(new Insets(0.0, 4.0, 0.0, 4.0));
	        downloadButton.setOnAction(actionEvent -> {
				try {
					MFXDialogsUtil.showSimpleInformationDialog(getStage(), getParentRoot(), "Downloading file...");
					Client.getCommands().downloadFile(message.getMessageID(), RootReferences.getChatsController().getActiveChatSessionIndex());
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
	        });
	        ImageView downloadImage = new ImageView(Icons.DOWNLOAD);
	        downloadImage.setFitWidth(31);
	        downloadImage.setFitHeight(31);
	        downloadButton.setGraphic(downloadImage);
	        
	        messageLabel.setGraphic(downloadButton);
	        messageLabel.setContentDisplay(ContentDisplay.RIGHT);
		
		}
		}
		
		return hbox;
	}
	
	private void printDateLabelIfNeeded(Message msg) {

		class MessageDateTracker {
			
		    private static String lastMessageDate = null;

		    private MessageDateTracker() {}
		    
		    public static void updatelastMessageDate(String date) {
		        lastMessageDate = date;
		    }
		    
		    public static String getLastMessageDate() {
		        return lastMessageDate;
		    }
		}

		// Retrieve current date
		Instant instant = Instant.ofEpochMilli(msg.getTimeWritten());
		ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());

		String currentMessageDate = zonedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);

		// Add a label if it is a new day. In case the lastMessageDate is null it simply moves on
		if (!currentMessageDate.equals(MessageDateTracker.getLastMessageDate())) {

			Label labelDenotingDifferentDay = new Label();
			
			if(currentMessageDate.equals(ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))) {
				labelDenotingDifferentDay.setText("Today");
			} else {
				labelDenotingDifferentDay.setText(currentMessageDate);
			}

			
			HBox hbox = new HBox();

			hbox.setAlignment(Pos.CENTER);
			hbox.getChildren().add(labelDenotingDifferentDay);
			messagingBox.getChildren().add(hbox);
		}

		// current messageMessageDate becomes previousMessageDate for next iteration
		MessageDateTracker.updatelastMessageDate(currentMessageDate);
	}
	
	public void printToMessageArea(Message msg, int chatSessionIndex, int activeChatSessionIndex) {

		if (chatSessionIndex == activeChatSessionIndex) {
			
			boolean isUserReadingThroughOldMessages = !chatBoxScrollpane.vvalueProperty().isEqualTo(chatBoxScrollpane.vmaxProperty()).get();
			
			printDateLabelIfNeeded(msg);
			messagingBox.getChildren().add(createClientMessage(msg));

			// Scroll to the bottom unless user is reading through old messages
			if (isUserReadingThroughOldMessages) {
				return;
			}

			setVvalue(chatBoxScrollpane.getVmax());
		}
		
	}
	
	public void notifyUser(Message message, int chatSessionIndex, int activeChatSessionIndex) {
		
		/*
		 * Skip notification if the user is focused on the app and the message received
		 * originates from the chat session he is currently active in.*
		 */
		if (getStage().isFocused() && chatSessionIndex == activeChatSessionIndex) {
			return;
		}


		byte[] messageContent;
		
		if (message.getContentType() == ContentType.FILE) {
			messageContent = message.getFileName();
		} else {
			messageContent = message.getText();
		}
		
		NotificationsUtil.createMessageNotification(message.getUsername(), new String(messageContent));
	}
	
	public void clearMessages() {
		messagingBox.getChildren().clear();
		setVvalue(1.0); // Reset vValue to the newest message
	}
	
	private void setVvalue(double vvalue) {
		
		// No idea why you gotta call these two methods before changing the vValue
		chatBoxScrollpane.applyCss();
		chatBoxScrollpane.layout();
		
		chatBoxScrollpane.setVvalue(vvalue);
	}

	@FXML
	public void sendMessageFile(ActionEvent event) {

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Send file");
		File file = fileChooser.showOpenDialog(getStage());

		if (file == null) {
			return;
		}

		try {
			Client.sendFile(file, RootReferences.getChatsController().getActiveChatSessionIndex());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@FXML
	public void sendMessageTextByPressingEnter(KeyEvent event) {

		if (event.getCode() != KeyCode.ENTER) {
			return;
		}

		sendMessageText();
	}

	@FXML
	public void sendMessageTextByAction(ActionEvent event) {
		sendMessageText();
	}
	
	private void sendMessageText() {

		inputField.requestFocus();

		String message = inputField.getText();

		if (message == null || message.isBlank()) {
			return;
		}

		try {
			Client.sendMessageToClient(message, RootReferences.getChatsController().getActiveChatSessionIndex());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		inputField.setText("");
	}

}
