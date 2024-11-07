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
package github.chatapp.client.main.java.controllers.chat_interface;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.filechooser.FileSystemView;

import github.chatapp.client.main.java.application.entry.EntryInterface;
import github.chatapp.client.main.java.controllers.chat_interface.dialogs.LogoutDialog;
import github.chatapp.client.main.java.info.GeneralAppInfo;
import github.chatapp.client.main.java.service.client.ChatRequest;
import github.chatapp.client.main.java.service.client.ChatSession;
import github.chatapp.client.main.java.service.client.DonationHtmlPage;
import github.chatapp.client.main.java.service.client.io_client.Client;
import github.chatapp.client.main.java.service.client.io_client.MessageHandler;
import github.chatapp.client.main.java.service.client.io_client.Client.BackupVerificationEntry;
import github.chatapp.client.main.java.util.HostServicesUtil;
import github.chatapp.client.main.java.util.MemoryUtil;
import github.chatapp.client.main.java.util.dialogs.CustomDialogButtonTypes;
import github.chatapp.client.main.java.util.dialogs.DialogsUtil;
import github.chatapp.client.main.java.util.dialogs.MFXDialogsUtil;
import github.chatapp.common.LoadedInMemoryFile;
import github.chatapp.common.entry.CreateAccountInfo;
import github.chatapp.common.entry.EntryType;
import github.chatapp.common.entry.LoginInfo;
import github.chatapp.common.entry.LoginInfo.PasswordType;
import github.chatapp.common.message_types.Message;
import github.chatapp.common.results.ResultHolder;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

/**
 * @author Ilias Koukovinis
 *
 */
public class ChatInterfaceController implements Initializable {

	@FXML
	private BorderPane rootBorderPane;

	@FXML
	public StackPane stackPane;
	
	private Stage stage;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		try {
			while (!Client.isLoggedIn()) {

				EntryInterface entryInterface = new EntryInterface();
				entryInterface.showAndWait();

				EntryType registrationType = entryInterface.getRegistrationType();

				String email = entryInterface.getEmail();
				String password = entryInterface.getPassword();

				Map credentials = null;
				Client.Entry<?> credentialsExchangeEntry = null;
				
				switch (registrationType) {
				case LOGIN -> {

					Client.LoginEntry loginEntry = Client.createNewLoginEntry();
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

					credentialsExchangeEntry = Client.createNewCreateAccountEntry();
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
						DialogsUtil.showErrorDialog(resultMessage);
						continue;
					}
				}

				if (registrationType == EntryType.CREATE_ACCOUNT || entryInterface.getPasswordType() == PasswordType.PASSWORD) {
					
					Client.VerificationEntry verificationEntry = Client.createNewVerificationEntry();
					
					class VerificationDialog {
						
						private TextInputDialog verificationCodeDialog;
						private String verificationCode;
						
						public VerificationDialog() {
							
							String headerText = "Enter the code that was sent to your email to verify it is really you";
							
							ButtonType resendVerificationCodeButtonType = new ButtonType("Resend verification code");
							verificationCodeDialog = DialogsUtil.createTextInputDialog(headerText, null, "Verification Code", resendVerificationCodeButtonType, ButtonType.OK);
							
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
							DialogsUtil.showSuccessDialog(resultMessage);
							break;
						}
						
						DialogsUtil.showErrorDialog(resultMessage);
					}
				} else {
					
					BackupVerificationEntry backupVerificationEntry = Client.createNewBackupVerificationEntry();
					
					ResultHolder entryResult = backupVerificationEntry.getResult();
					boolean isSuccesfull = entryResult.isSuccesfull();
					String resultMessage = entryResult.getResultMessage();
					
					if (isSuccesfull) {
						DialogsUtil.showSuccessDialog(resultMessage);
						break;
					}
					
					DialogsUtil.showErrorDialog(resultMessage);
				}
			}

			Client.startMessageHandler(new MessageHandler() {
				
				@Override
				public void serverMessageReceived(String message) {
					Platform.runLater(() -> DialogsUtil.showInfoDialog(message));						
				}
				
				@Override
				public void messageReceived(Message message, int chatSessionIndex) {
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() {

							int activeChatSessionIndex = RootReferences.getChatsController().getActiveChatSessionIndex();
							
							RootReferences.getMessagingController().printToMessageArea(
									message,
									chatSessionIndex,
									activeChatSessionIndex);

							RootReferences.getMessagingController().notifyUser(
									message,
									chatSessionIndex,
									activeChatSessionIndex);
						}
					});
				}

				@Override
				public void alreadyWrittenTextReceived(ChatSession chatSession) {
					Platform.runLater(() -> {
						
						int chatSessionIndex = chatSession.getChatSessionIndex();
						List<Message> messages = chatSession.getMessages();
						
						for (int i = 0; i < messages.size(); i++) {
							
							Message message = messages.get(i);

							RootReferences.getMessagingController().printToMessageArea(
									message, 
									chatSessionIndex, 
									RootReferences.getChatsController().getActiveChatSessionIndex());
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

						Platform.runLater(() -> MFXDialogsUtil.showSimpleInformationDialog(stage, rootBorderPane, "Succesfully saved file!"));
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
							
							HostServicesUtil.getHostServices().showDocument(htmlUrl);
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					});
				}
				
				@Override
				public void serverSourceCodeReceived(String serverSourceCodeURL) {
					Platform.runLater(() -> HostServicesUtil.getHostServices().showDocument(serverSourceCodeURL));
				}

				@Override
				public void usernameReceived(String username) {
					// Do nothing.
				}
				
				@Override
				public void clientIDReceived(int clientID) {
//					Platform.runLater(() -> clientIDLabel.setText(clientIDLabel.getText().concat(String.valueOf(clientID))));
				}
				
				@Override
				public void chatSessionsReceived(List<ChatSession> chatSessions) {
					Platform.runLater(() -> {
						RootReferences.getChatsController().clearChatSessions();
						RootReferences.getChatsController().addChatSessions(chatSessions);
					});
				}
				
				@Override
				public void chatRequestsReceived(List<ChatRequest> chatRequests) {
					Platform.runLater(() -> {
//						RootReferences.getChatRequestsController().clearChatRequests();
//						RootReferences.getChatRequestsController().addChatRequests(chatRequests);
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

								if (RootReferences.getChatsController().getActiveChatSessionIndex() == chatSession.getChatSessionIndex()) {
									RootReferences.getMessagingController().deleteMessage(i);
								}

								break;
							}

						}

					});
				}
			});

			RootReferences.initializeAll();
			
			rootBorderPane.setCenter(RootReferences.getMessageRoot());
			stackPane.getChildren().add(RootReferences.getChatsRoot());
		} catch (Exception e) {

			Optional<ButtonType> exceptionDialogResult = DialogsUtil.showExceptionDialog(e);

			if (!exceptionDialogResult.isPresent() || exceptionDialogResult.get() == CustomDialogButtonTypes.RETRY_BUTTON) {
				initialize(arg0, arg1);
			} else {
				Platform.exit();
			}
			
		}
	}

//	@FXML
//	public void copyClientIDToClipboard(MouseEvent event) {
//
//		Clipboard clipboard = Clipboard.getSystemClipboard();
//		ClipboardContent content = new ClipboardContent();
//
//		String clientID = clientIDLabel.getText();
//
//		content.putString(clientID);
//		clipboard.setContent(content);
//		
//		MFXDialogsUtil.showSimpleInformationDialog(stage, rootBorderPane, "Succesfully copied clientID");
//	}

	@FXML
	public void logout(ActionEvent event) throws IOException {
		
		LogoutDialog dialog = new LogoutDialog(stage, rootBorderPane);
		dialog.showAndWait();
		
		if (!dialog.isCanceled()) {
			Client.getCommands().logout();
			closeClient();
			Platform.exit();
		}
	}

	@FXML
	public void transitionToChats(ActionEvent event) {
		transitionBetweenOptionsBar(stackPane, RootReferences.getChatsRoot());
	}

	@FXML
	public void transitionToChatRequests(ActionEvent event) {
		transitionBetweenOptionsBar(stackPane, RootReferences.getChatRequestsRoot());
	}
	
	@FXML
	public void transitionToSettings(ActionEvent event) {
		transitionBetweenOptionsBar(stackPane, RootReferences.getSettingsRoot());
	}

	private void transitionBetweenOptionsBar(StackPane parentContainer, Node newComponent) {

		Node oldComponent = stackPane.getChildren().get(0);
		
		if (newComponent.equals(oldComponent)) {
			return;
		}

		parentContainer.getChildren().remove(oldComponent);
		parentContainer.getChildren().add(newComponent);
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public void closeClient() {
		try {
			Client.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
}