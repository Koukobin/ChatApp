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
package main.java.controllers.chat_interface;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.filechooser.FileSystemView;

import org.chatapp.commons.ContentType;
import org.chatapp.commons.LoadedInMemoryFile;
import org.chatapp.commons.Message;
import org.chatapp.commons.ResultHolder;
import org.chatapp.commons.entry.CreateAccountInfo;
import org.chatapp.commons.entry.EntryType;
import org.chatapp.commons.entry.LoginInfo;
import org.chatapp.commons.entry.LoginInfo.PasswordType;
import org.controlsfx.control.Notifications;
import org.controlsfx.glyphfont.Glyph;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.google.common.base.Throwables;
import com.jfoenix.controls.JFXListCell;
import com.jfoenix.controls.JFXListView;

import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import main.java.application.entry.EntryInterface;
import main.java.controllers.chat_interface.dialogs.ChangeCredentialsDialog;
import main.java.controllers.chat_interface.dialogs.LogoutDialog;
import main.java.controllers.chat_interface.dialogs.SendChatRequestDialog;
import main.java.controllers.chat_interface.dialogs.decide_server_to_connect.DecideServerToConnectToDialog;
import main.java.databases.ServerInfo;
import main.java.info.GeneralAppInfo;
import main.java.info.chat_interface.ChatInterfaceInfo;
import main.java.service.client.ChatRequest;
import main.java.service.client.ChatSession;
import main.java.service.client.DonationHtmlPage;
import main.java.service.client.io_client.Client;
import main.java.service.client.io_client.MessageHandler;
import main.java.service.client.io_client.Client.BackupVerificationEntry;
import main.java.util.MemoryUtil;
import main.java.util.dialogs.Dialogs;
import main.java.util.dialogs.MFXDialogs;

/**
 * @author Ilias Koukovinis
 *
 */
public class ChatInterfaceController implements Initializable {

	@FXML
	private BorderPane rootBorderPane;

	@FXML
	private SplitPane splitPane;
	
	@FXML
	private HBox clientIDHBOX;
	@FXML
	private Label clientIDLabel;

	@FXML
	private TabPane tabPane;

	@FXML
	private JFXListView<ChatSession> chatSessionsListView;
	@FXML
	private JFXListView<ChatRequest> chatRequestsListView;
	@FXML
	private TextField searchForChatRequestTextField;

	@FXML
	private VBox chatBox;
	
	@FXML
	private ScrollPane chatBoxScrollpane;
	
	@FXML
	private TextField inputField;

	private Client client;

	private HostServices hostServices;
	private Stage stage;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		boolean retry = false;

		do {
			try {
				
				{
					DecideServerToConnectToDialog dialog = new DecideServerToConnectToDialog(null, null);
					dialog.showAndWait();
					
					if (dialog.isCanceled()) {
						System.exit(0);
					}
					
					ServerInfo serverInfo = dialog.getResult();

					Client.ServerCertificateVerification verify = dialog.getCheckServerCertificate()
							? Client.ServerCertificateVerification.VERIFY
							: Client.ServerCertificateVerification.IGNORE;

					client = new Client(serverInfo.getAddress(), serverInfo.getPort(), verify);
				}
				
				while (!client.isLoggedIn()) {

					EntryInterface entryInterface = new EntryInterface();
					entryInterface.showAndWait();

					EntryType registrationType = entryInterface.getRegistrationType();

					String email = entryInterface.getEmail();
					String password = entryInterface.getPassword();

					Map credentials = null;
					Client.Entry<?> credentialsExchangeEntry = null;
					
					switch (registrationType) {
					case LOGIN -> {

						Client.LoginEntry loginEntry = client.createNewLoginEntry();
						loginEntry.sendEntryType();

						credentials = new EnumMap<>(LoginInfo.Credential.class);
						credentials.put(LoginInfo.Credential.EMAIL, email);
						credentials.put(LoginInfo.Credential.PASSWORD, password);

						if (entryInterface.getPasswordType() != PasswordType.PASSWORD) {
							loginEntry.togglePasswordType();
						}
						
						credentialsExchangeEntry = loginEntry;
					}
					case CREATE_ACCOUNT -> {

						credentialsExchangeEntry = client.createNewCreateAccountEntry();
						credentialsExchangeEntry.sendEntryType();

						credentials = new EnumMap<>(CreateAccountInfo.Credential.class);
						credentials.put(CreateAccountInfo.Credential.EMAIL, email);
						credentials.put(CreateAccountInfo.Credential.USERNAME, entryInterface.getUsername());
						credentials.put(CreateAccountInfo.Credential.PASSWORD, password);
					}
					}
					
					credentialsExchangeEntry.sendCredentials(credentials);
					
					MemoryUtil.freeStringFromMemory(password);
					MemoryUtil.freeStringFromMemory(email);
					
					{
						ResultHolder entryResult = credentialsExchangeEntry.getCredentialsExchangeResult();
						boolean isSuccesfull = entryResult.isSuccesfull();
						String resultMessage = entryResult.getResultMessage();
						
						if (!isSuccesfull) {
							Dialogs.showErrorDialog(resultMessage);
							continue;
						}
					}

					if (registrationType == EntryType.CREATE_ACCOUNT || entryInterface.getPasswordType() == PasswordType.PASSWORD) {
						
						Client.VerificationEntry verificationEntry = client.createNewVerificationEntry();
						
						class VerificationDialog {
							
							private TextInputDialog verificationCodeDialog;
							private String verificationCode;
							
							public VerificationDialog() {
								
								String headerText = "Enter the code that was sent to your email to verify it is really you";
								
								ButtonType resendVerificationCodeButtonType = new ButtonType("Resend verification code");
								verificationCodeDialog = Dialogs.createTextInputDialog(headerText, null, "Verification Code", resendVerificationCodeButtonType, ButtonType.OK);
								
								Button resendVerificationCodeButton = (Button) verificationCodeDialog.getDialogPane().lookupButton(resendVerificationCodeButtonType);
								resendVerificationCodeButton.pressedProperty().addListener(new ChangeListener<Boolean>() {

									@Override
									public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
										try {
											verificationEntry.resendVerificationCode();
										} catch (IOException ioe) {
											ioe.printStackTrace();
										}
									}
								});
							}
							
							public void showAndWait() {
								
								Optional<String> result = verificationCodeDialog.showAndWait();
								
								if (!result.isPresent()) {
									return;
								}
								
								verificationCode = result.get();
							}
							
							public String getVerificationCode() {
								return verificationCode;
							}
						}
						
						VerificationDialog verificationDialog = new VerificationDialog();
						while (!verificationEntry.isVerificationComplete()) {
							
							verificationDialog.showAndWait();
							verificationEntry.sendVerificationCode(verificationDialog.getVerificationCode());
							
							ResultHolder entryResult = verificationEntry.getResult();
							boolean isSuccesfull = entryResult.isSuccesfull();
							String resultMessage = entryResult.getResultMessage();
							
							if (isSuccesfull) {
								Dialogs.showSuccessDialog(resultMessage);
								break;
							}
							
							Dialogs.showErrorDialog(resultMessage);
						}
					} else {
						
						BackupVerificationEntry backupVerificationEntry = client.createNewBackupVerificationEntry();
						
						ResultHolder entryResult = backupVerificationEntry.getResult();
						boolean isSuccesfull = entryResult.isSuccesfull();
						String resultMessage = entryResult.getResultMessage();
						
						if (isSuccesfull) {
							Dialogs.showSuccessDialog(resultMessage);
							break;
						}
						
						Dialogs.showErrorDialog(resultMessage);
					}
				}

				client.startMessageHandler(new MessageHandler() {
					
					@Override
					public void serverMessageReceived(String message) {
						Platform.runLater(() -> Dialogs.showInfoDialog(message));						
					}
					
					@Override
					public void messageReceived(Message message, int chatSessionIndex) {
						Platform.runLater(new Runnable() {
							
							private static final MediaPlayer notificationPlayer;
							
							static {
								Media media = new Media(ChatInterfaceInfo.NOTIFICATION_SOUND_LOCATION);
								notificationPlayer = new MediaPlayer(media);
								notificationPlayer.setCycleCount(MediaPlayer.INDEFINITE);
							}
							
							@Override
							public void run() {
								
								ChatInterfaceController.this.printToMessageArea(message, chatSessionIndex);

								if (stage.isFocused() && chatSessionIndex == getChatSessionIndex()) {
									return;
								}

								Notifications.create()
										.title(GeneralAppInfo.TITLE)
										.text(new String(message.getText()))
										.hideAfter(Duration.seconds(5)).position(Pos.TOP_RIGHT)
										.darkStyle()
										.showInformation();

								notificationPlayer.play();
							}
						});
					}

					@Override
					public void alreadyWrittenTextReceived(ChatSession chatSession) {
						Platform.runLater(() -> {
							
							chatBox.getChildren().clear();

							int chatSessionIndex = chatSession.getChatSessionIndex();
							List<Message> messages = chatSession.getMessages();
							
							for (int i = 0; i < messages.size(); i++) {
								ChatInterfaceController.this.printToMessageArea(messages.get(i), chatSessionIndex);
							}
						});
					}

					@Override
					public void fileDownloaded(LoadedInMemoryFile file) {
						try {

							String dirPathString = FileSystemView.getFileSystemView().getDefaultDirectory().getPath()
									+ "/Documents/" + GeneralAppInfo.GENERAL_NAME + "Downloads/";
							Path dirPath = Paths.get(dirPathString);

							try {
								Files.createDirectory(dirPath);
							} catch (FileAlreadyExistsException faee) {
								// Do nothing
							}

							Path filePath = Paths.get(dirPathString + File.separator + file.getFileName());
							Files.write(filePath, file.getFileBytes());

							Platform.runLater(() -> MFXDialogs.showSimpleInformationDialog(stage, rootBorderPane, "Succesfully saved file!"));
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
					
					@Override
					public void donationPageReceived(DonationHtmlPage donationPage) {
						Platform.runLater(() -> {
							try {
								String html = donationPage.getHtml();
								Path pathToCreateHtmlFile = Files.createTempFile(donationPage.getHtmlFileName(), ".html");
								Files.write(pathToCreateHtmlFile, html.getBytes());
								String htmlUrl = pathToCreateHtmlFile.toUri().toURL().toString();
								
								hostServices.showDocument(htmlUrl);
							} catch (IOException ioe) {
								ioe.printStackTrace();
							}
						});
					}
					
					@Override
					public void serverSourceCodeReceived(String serverSourceCodeURL) {
						Platform.runLater(() -> hostServices.showDocument(serverSourceCodeURL));
					}

					@Override
					public void usernameReceived(String username) {
						// Do nothing.
					}
					
					@Override
					public void clientIDReceived(int clientID) {
						Platform.runLater(() -> clientIDLabel.setText(clientIDLabel.getText().concat(String.valueOf(clientID))));
					}
					
					@Override
					public void chatSessionsReceived(List<ChatSession> chatSessions) {
						Platform.runLater(() -> {
							chatSessionsListView.getItems().clear();
							chatSessionsListView.getItems().addAll(chatSessions);
						});
					}
					
					@Override
					public void chatRequestsReceived(List<ChatRequest> chatRequests) {
						Platform.runLater(() -> {
							chatRequestsListView.getItems().clear();
							chatRequestsListView.getItems().addAll(chatRequests);
						});
					}

					@Override
					public void messageDeleted(ChatSession chatSession, int messageIDOfDeletedMessage) {
						Platform.runLater(() -> {

							List<Message> messages = chatSession.getMessages();
							for (int i = 0; i < messages.size(); i++) {

								Message message = messages.get(i);

								if (message.getMessageID() == messageIDOfDeletedMessage) {

									messages.remove(i);

									if (getChatSessionIndex() == chatSession.getChatSessionIndex()) {
										chatBox.getChildren().remove(i);
									}

									break;
								}

							}

						});
					}
				});

				{
					chatSessionsListView.setOnMouseClicked((MouseEvent e) -> {

						if (!(e.getPickResult().getIntersectedNode() instanceof JFXListCell<?> cell)) {
							return;
						}

						if (e.getButton() == MouseButton.PRIMARY /* Left Click */) {

							ChatSession chatSession = (ChatSession) cell.getItem();

							chatBox.getChildren().clear();
							if (!chatSession.haveChatMessagesBeenCached()) {
								try {
									client.getCommands().sendGetWrittenText(chatSession.getChatSessionIndex());
								} catch (IOException ioe) {
									ioe.printStackTrace();
								}
							} else {
								List<Message> messages = chatSession.getMessages();
								for (int i = 0; i < messages.size(); i++) {
									chatBox.getChildren().add(createClientMessageLabel(messages.get(i)));
								}
							}
						} else if (e.getButton() == MouseButton.SECONDARY /* Right Click */) {

							final ContextMenu contextMenu = new ContextMenu();
							MenuItem delete = new MenuItem("Delete");
							contextMenu.getItems().addAll(delete);

							delete.setOnAction(new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent event) {
									try {
										client.getCommands().deleteChatSession(getChatSessionIndex());
									} catch (IOException ioe) {
										ioe.printStackTrace();
									}
								}
							});

							contextMenu.show(cell, e.getScreenX(), e.getScreenY());
						}
					});

					chatRequestsListView.setOnMouseClicked((MouseEvent e) -> {
						if (e.getButton() == MouseButton.SECONDARY
								&& e.getPickResult().getIntersectedNode() instanceof JFXListCell<?> cell) {

							final ContextMenu contextMenu = new ContextMenu();
							MenuItem accept = new MenuItem("Accept");
							MenuItem decline = new MenuItem("Decline");
							contextMenu.getItems().addAll(accept, decline);
							
							accept.setOnAction(new EventHandler<ActionEvent>() {
							    @Override
							    public void handle(ActionEvent event) {
							    	
							    	ChatRequest user = (ChatRequest) cell.getItem();
							    	
							    	try {
										client.getCommands().acceptChatRequest(user.clientID());
									} catch (IOException ioe) {
										ioe.printStackTrace();
									}
							    }
							});
							
							decline.setOnAction(new EventHandler<ActionEvent>() {
							    @Override
							    public void handle(ActionEvent event) {
							    	
							    	ChatRequest user = (ChatRequest) cell.getItem();
							    	
							    	try {
										client.getCommands().declineChatRequest(user.clientID());
									} catch (IOException ioe) {
										ioe.printStackTrace();
									}
							    }
							});

							contextMenu.show(cell, e.getScreenX(), e.getScreenY());
						}
					});
				}
				
				// This basically retrieves more messages from the conversation when it reaches the top of the conversation
				chatBoxScrollpane.setOnScroll(new EventHandler<ScrollEvent>() {

					private Instant lastTimeRequrestedMoreMessages;

					@Override
					public void handle(ScrollEvent event) {

						if (lastTimeRequrestedMoreMessages != null) {

							long elapsedSeconds = Instant.now().getEpochSecond() - lastTimeRequrestedMoreMessages.getEpochSecond();

							// Have a time limit since if user sends to many requests to get messages then
							// he will probably crash by the enormous amount of messages sent to him
							if (elapsedSeconds < 3) {
								return;
							}
						}

						// When it reaches the top of the scroll pane get more written messages
						if (Double.compare(chatBoxScrollpane.getVvalue(), chatBoxScrollpane.getVmin()) == 0) {
							try {
								client.getCommands().sendGetWrittenText(getChatSessionIndex());
								lastTimeRequrestedMoreMessages = Instant.now();
							} catch (IOException ioe) {
								ioe.printStackTrace();
							}
						}
						
					}
				});

				{
					// this listener changes the width of the labels in chatbox whenever the width
					// of the tab pane or split pane change. This way the labels texts wrap
					// whenever hitting the edge of the stage
					ChangeListener<Number> listener = (observable, oldValue, newValue) -> {
						double prefWidth = stage.getWidth() - tabPane.getWidth();
						Iterator<Node> iterator = chatBox.getChildren().iterator();
						while (iterator.hasNext()) {
							Label label = (Label) iterator.next();
							label.setPrefWidth(prefWidth);
						}
					};
					
					tabPane.widthProperty().addListener(listener);
					splitPane.widthProperty().addListener(listener);
				}
				
				new JMetro(tabPane, Style.DARK);
				
				retry = false;
			} catch (Exception e) {
				
				ButtonType retryButton = new ButtonType("Retry");

				Alert confirmationDialog = Dialogs.createConfirmationDialog("An error occured: " + e.getMessage(),
						Throwables.getStackTraceAsString(e));

				Optional<ButtonType> result = confirmationDialog.showAndWait();

				if (!result.isPresent() || result.get() == retryButton) {
					retry = true;
					continue;
				}
				
				System.exit(0);
			}
		} while (retry);
	}

	@FXML
	public void refreshChatSessionsListView(ActionEvent event) {
		try {
			client.getCommands().sendGetChatSessions();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@FXML
	public void refreshChatRequestsListView(ActionEvent event) {
		try {
			client.getCommands().sendGetChatRequests();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@FXML
	public void sortChatRequests(KeyEvent event) {

		int num;

		try {
			num = Integer.parseInt(searchForChatRequestTextField.getText());
		} catch (NumberFormatException nfe) {
			return;
		}
		
		List<ChatRequest> items = chatRequestsListView.getItems();

		boolean differentFromBefore = true;
		while (differentFromBefore) {

			differentFromBefore = false;

			// reverse loop so sorted items appear from top to bottom and not from bottom to
			// top
			for (int i = items.size() - 1; i > 0; i--) {

				int a = Math.abs(items.get(i).clientID() - num);
				int b = Math.abs(items.get(i - 1).clientID() - num);

				if (a < b) {
					ChatRequest temp = items.get(i);
					items.set(i, items.get(i - 1));
					items.set(i - 1, temp);
					differentFromBefore = true;
				}

			}

		}
		
	}
	
	@FXML
	public void copyClientIDToClipboard(MouseEvent event) {

		Clipboard clipboard = Clipboard.getSystemClipboard();
		ClipboardContent content = new ClipboardContent();

		String clientID = clientIDLabel.getText();

		content.putString(clientID);
		clipboard.setContent(content);
		
		MFXDialogs.showSimpleInformationDialog(stage, rootBorderPane, "Succesfully copied clientID");
	}

	public Label createClientMessageLabel(Message message) {
		
		ContentType contentType = message.getContentType();
		
		Label label = new Label();
		label.setFont(Font.font(16));
		label.setTextFill(Color.WHITE);
		
		String username = message.getUsername();
		int clientID = message.getClientID();
		
		MenuItem delete = new MenuItem("Delete");
		delete.setOnAction((e) -> {
			try {
				client.getCommands().deleteMessage(getChatSessionIndex(), message.getMessageID());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		});
		
		MenuItem copy = new MenuItem("Copy");
		copy.setOnAction((e) -> {
			
			Clipboard clipboard = Clipboard.getSystemClipboard();
			ClipboardContent clipboardContent = new ClipboardContent();
			
			clipboardContent.putString(label.getText());
			clipboard.setContent(clipboardContent);
		});
		
		final ContextMenu contextMenu = new ContextMenu();
		contextMenu.getItems().addAll(delete, copy);

		Tooltip tt = new Tooltip();
		tt.setText("Right click for more actions!");
		tt.setShowDelay(Duration.millis(50));

		label.setText(username + "@" + clientID + ":");
		label.setContextMenu(contextMenu);
		label.setTooltip(tt);
		
		switch(contentType) {
		case TEXT -> {
			label.setText(label.getText() + new String(message.getText()));
		}
		case FILE ->{
			
			String fileName = new String(message.getFileName());
			label.setText(label.getText() + fileName);

			Button downloadButton = new Button();
	        downloadButton.managedProperty().bind(label.textProperty().isEmpty().not());
	        downloadButton.setFocusTraversable(false);
	        downloadButton.setPadding(new Insets(0.0, 4.0, 0.0, 4.0));
	        downloadButton.setOnAction(actionEvent -> {
				try {
					MFXDialogs.showSimpleInformationDialog(stage, rootBorderPane, "Downloading file...");
					client.getCommands().downloadFile(message.getMessageID(), getChatSessionIndex());
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
	        });
	        Glyph clipboardIcon = Glyph.create("FontAwesome|DOWNLOAD");
	        clipboardIcon.setFontSize(20.0);
	        downloadButton.setGraphic(clipboardIcon);
	        
	        label.setGraphic(downloadButton);
	        label.setContentDisplay(ContentDisplay.RIGHT);
		
		}
		}
		
		return label;
	}

	public void printToMessageArea(Message msg, int chatSessionIndex) {

		if (chatSessionIndex == getChatSessionIndex()) {

			boolean isUserCurrentlyInActiveConversationAndNotReadingThroughOldMessages = 
					chatBoxScrollpane.vvalueProperty().isEqualTo(chatBoxScrollpane.vmaxProperty()).get();
			
			chatBox.getChildren().add(createClientMessageLabel(msg));

			if (isUserCurrentlyInActiveConversationAndNotReadingThroughOldMessages) {
				
				// No idea why you gotta call these two methods before changing the vValue
				chatBoxScrollpane.applyCss();
				chatBoxScrollpane.layout();
				
				chatBoxScrollpane.setVvalue(chatBoxScrollpane.getVmax());
			}
		}
		
	}

	@FXML
	public void sendMessageFile(ActionEvent event) {

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Send file");
		File file = fileChooser.showOpenDialog(stage);

		if (file == null) {
			return;
		}

		try {
			client.sendFile(file, getChatSessionIndex());
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

		int chatSessionIndex = getChatSessionIndex();
		String message = inputField.getText();

		if (message == null || message.isBlank()) {
			return;
		}

		try {
			client.sendMessageToClient(message, chatSessionIndex);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		inputField.setText(null);
	}

	@FXML
	public void getServerHosterDonationHTMLPage(ActionEvent event) throws IOException {
		client.getCommands().sendGetDonationHTMLPage();
	}
	
	@FXML
	public void getServerSourceCodeHTMLPage(ActionEvent event) throws IOException {
		client.getCommands().sendGetServerSourceCodeHTMLPage();
	}
	
	@FXML
	public void getSourceCodeHTMLPage(ActionEvent event) {
		hostServices.showDocument(GeneralAppInfo.SOURCE_CODE_HTML_PAGE_URL);
	}
	
	@FXML
	public void sendChatRequest(ActionEvent event) throws IOException {
		
		SendChatRequestDialog dialog = new SendChatRequestDialog(stage, rootBorderPane);
		dialog.showAndWait();

		if (!dialog.isCanceled()) {
			int friendRequestClientID = dialog.getFriendRequest();
			client.getCommands().sendChatRequest(friendRequestClientID);
		}
	}
	
	@FXML
	public void changeUsername(ActionEvent event) throws IOException {
		
		ChangeCredentialsDialog dialog = new ChangeCredentialsDialog(stage, rootBorderPane,ChangeCredentialsDialog.ChangeCredentialType.USERNAME);
		dialog.showAndWait();
		
		if (!dialog.isCanceled()) {
			String newUsername = dialog.getNewCredential();
			client.getCommands().changeUsername(newUsername);
			MemoryUtil.freeStringFromMemory(newUsername);
		}
	}
	
	@FXML
	public void changePassword(ActionEvent event) throws IOException {
		
		ChangeCredentialsDialog dialog = new ChangeCredentialsDialog(stage, rootBorderPane, ChangeCredentialsDialog.ChangeCredentialType.PASSWORD);
		dialog.showAndWait();
		
		if (!dialog.isCanceled()) {
			String newPassword = dialog.getNewCredential();
			client.getCommands().changePassword(newPassword);
			MemoryUtil.freeStringFromMemory(newPassword);
		}
	}
	
	@FXML
	public void logout(ActionEvent event) throws IOException {
		
		LogoutDialog dialog = new LogoutDialog(stage, rootBorderPane);
		dialog.showAndWait();
		
		if (!dialog.isCanceled()) {
			client.getCommands().logout();
			System.exit(0);
		}
	}
	
	private int getChatSessionIndex() {
		return chatSessionsListView.getSelectionModel().getSelectedIndex();
	}
	
	public void setHostServices(HostServices hostServices) {
		this.hostServices = hostServices;
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public void closeClient() {
		try {
			client.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
}