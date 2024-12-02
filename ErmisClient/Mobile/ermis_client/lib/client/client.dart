/* Copyright (C) 2024 Ilias Koukovinis <ilias.koukovinis@gmail.com>
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

import 'dart:io';
import 'dart:async';
import 'dart:typed_data';

import 'package:ermis_client/client/io/byte_buf.dart';

import 'common/chat_request.dart';
import 'common/chat_session.dart';
import 'common/file_heap.dart';
import 'common/html_page.dart';
import 'io/input_stream.dart';
import 'common/message.dart';
import 'message_handler.dart';
import 'io/output_stream.dart';
import 'common/common.dart';

enum ServerCertificateVerification { verify, ignore }

class Client {

  static final Client _instance = Client._();

  static Client getInstance() {
    return _instance;
  }

  ByteBufInputStream? _inputStream;
  ByteBufOutputStream? _outputStream;

  SecureSocket? _sslSocket;
  Stream<Uint8List>? broadcastStream;

  bool _isLoggedIn = false;

  final MessageHandler _messageHandler = MessageHandler();
  
  Uri? uri;

  Client._();

  Future<bool> initialize(InternetAddress remoteAddress, int remotePort, ServerCertificateVerification scv) async {
    if (remotePort <= 0) {
      throw ArgumentError("Port cannot be below zero");
    }

    try {
      final context = SecurityContext(withTrustedRoots: false);

      _sslSocket = await SecureSocket.connect(remoteAddress, remotePort,
          context: context,
          onBadCertificate: (X509Certificate cert) =>
              scv == ServerCertificateVerification.ignore);

      broadcastStream = _sslSocket!.asBroadcastStream();

      uri = Uri(scheme: 'https', host: remoteAddress.toString(), port: remotePort);

      _inputStream = ByteBufInputStream(broadcastStream: broadcastStream!);
      _outputStream = ByteBufOutputStream(secureSocket: _sslSocket!);

      _messageHandler.setSecureSocket(_sslSocket!);
      _messageHandler.setByteBufInputStream(_inputStream!);
      _messageHandler.setByteBufOutputStream(_outputStream!);

      _outputStream!.write(ByteBuf.empty());
      return _isLoggedIn = (await _inputStream!.read()).readBoolean();
    } on HandshakeException {
      if (scv == ServerCertificateVerification.verify) {
        throw HandshakeException("Could not verify server certificate");
      }
      rethrow;
    }
  }

  void sendMessageToClient(String text, int chatSessionIndex) {
    _messageHandler.sendMessageToClient(text, chatSessionIndex);
  }

  void sendImageToClient(String fileName, Uint8List fileBytes, int chatSessionIndex) {
    _messageHandler.sendImageToClient(fileName, fileBytes, chatSessionIndex);
  }

  void sendFileToClient(String fileName, Uint8List fileContentBytes, int chatSessionIndex) {
    _messageHandler.sendFileToClient(fileName, fileContentBytes, chatSessionIndex);
  }

  Entry createNewVerificationEntry() {
    return Entry(EntryType.login, _outputStream!, _inputStream!);
  }

  Entry createNewBackupVerificationEntry() {
    return Entry(EntryType.login, _outputStream!, _inputStream!);
  }
	
	CreateAccountEntry createNewCreateAccountEntry() {
		return CreateAccountEntry(_outputStream!, _inputStream!);
	}
	
	LoginEntry createNewLoginEntry() {
		return LoginEntry(_outputStream!, _inputStream!);
	}

  void fetchUserInformation() {
    if (!isLoggedIn()) {
      throw StateError(
          "User can't start writing to the server if they aren't logged in");
    }
    
    _messageHandler.fetchUserInformation();
  }

  void startMessageHandler() {
    _messageHandler.startListeningToMessages();
  }

  void whenUsernameReceived(void Function(String username) runThis) {
    _messageHandler.whenUsernameReceived(runThis);
  }
  
  void whenMessageReceived(void Function(Message message, int chatSessionIndex) runThis) {
    _messageHandler.whenMessageReceived(runThis);
  }

  void whenAlreadyWrittenTextReceived(void Function(ChatSession chatSession) runThis) {
    _messageHandler.whenAlreadyWrittenTextReceived(runThis);
  }
  
  void whenServerMessageReceived(void Function(String message) runThis) {
    _messageHandler.whenServerMessageReceived(runThis);
  }
  
  void whenFileDownloaded(void Function(LoadedInMemoryFile file) runThis) {
    _messageHandler.whenFileDownloaded(runThis);
  }

  void whenImageDownloaded(void Function(LoadedInMemoryFile file, int messageID) runThis) {
    _messageHandler.whenImageDownloaded(runThis);
  }

  void whenDonationPageReceived(void Function(DonationHtmlPage donationPage) runThis) {
    _messageHandler.whenDonationPageReceived(runThis);
  }  

  void whenServerSourceCodeReceived(void Function(String serverSourceCodeURL) runThis) {
    _messageHandler.whenServerSourceCodeReceived(runThis);
  }

  void whenClientIDReceived(void Function(int clientID) runThis) {
    _messageHandler.whenClientIDReceived(runThis);
  }

  void whenChatRequestsReceived(void Function(List<ChatRequest> chatRequests) runThis) {
    _messageHandler.whenChatRequestsReceived(runThis);
  }

  void whenChatSessionsReceived(void Function(List<ChatSession> chatSessions) runThis) {
    _messageHandler.whenChatSessionsReceived(runThis);
  }

  void whenMessageDeleted(void Function(ChatSession chatSession, int messageIDOfDeletedMessage) runThis) {
    _messageHandler.whenMessageDeleted(runThis);
  }

  void whenProfilePhotoReceived(void Function(Uint8List iconBytes) runThis) {
    _messageHandler.whenProfilePhotoReceived(runThis);
  }

  Commands get getCommands => _messageHandler.getCommands;
  int get clientID => _messageHandler.clientID;

  bool isLoggedIn() {
    return _isLoggedIn;
  }

  Future<void> close() async {
    if (_sslSocket != null) {
      await _sslSocket!.close();
      _sslSocket = null;
      _isLoggedIn = false;
    }
  }

}

class Entry<T extends CredentialInterface> {
  final EntryType entryType;

  final ByteBufInputStream inputStream;
  final ByteBufOutputStream outputStream;

  bool isLoggedIn = false;
  bool isVerificationComplete = false;

  Entry(this.entryType, this.outputStream, this.inputStream);

  Future<ResultHolder> getCredentialsExchangeResult() async {
    ByteBuf msg = await inputStream.read();

    bool isSuccessful = msg.readBoolean();
    Uint8List resultMessageBytes = msg.readBytes(msg.readableBytes);

    return ResultHolder(isSuccessful, String.fromCharCodes(resultMessageBytes));
  }

  Future<void> sendCredentials(Map<T, String> credentials) async {
    for (var credential in credentials.entries) {
      bool isAction = false;
      int credentialInt = credential.key.id;
      String credentialValue = credential.value;

      ByteBuf payload = ByteBuf.smallBuffer();
      payload.writeBoolean(isAction);
      payload.writeInt(credentialInt);
      payload.writeBytes(Uint8List.fromList(credentialValue.codeUnits));

      outputStream.write(payload);
    }
  }

  Future<ResultHolder> getBackupVerificationCodeResult() async {
    ByteBuf payload = await inputStream.read();

    isLoggedIn = payload.readBoolean();
    Client.getInstance()._isLoggedIn = isLoggedIn;
    
    Uint8List resultMessageBytes = payload.readBytes(payload.readableBytes);

    return ResultHolder(isLoggedIn, String.fromCharCodes(resultMessageBytes));
  }

  Future<void> sendEntryType() async {
    outputStream.write(ByteBuf.smallBuffer()..writeInt(entryType.id));
  }

  Future<void> sendVerificationCode(String verificationCode) async {
    bool isAction = false;

    Uint8List verificationCodeBytes = Uint8List.fromList(verificationCode.codeUnits);
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeBoolean(isAction);
    payload.writeBytes(verificationCodeBytes);

    outputStream.write(payload);
  }

  Future<ResultHolder> getResult() async {
    ByteBuf msg = await inputStream.read();

    isVerificationComplete = msg.readBoolean();
    isLoggedIn = msg.readBoolean();

    Client.getInstance()._isLoggedIn = isLoggedIn;
    List<int> resultMessageBytes = msg.readBytes(msg.readableBytes);

    return ResultHolder(isLoggedIn, String.fromCharCodes(resultMessageBytes));
  }

  Future<void> resendVerificationCode() async {
    bool isAction = true;

    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeBoolean(isAction);
    payload.writeInt(VerificationAction.resendCode.id);

    outputStream.write(payload);
  }
}

class CreateAccountEntry extends Entry<CreateAccountCredential> {
  CreateAccountEntry(ByteBufOutputStream outputStream, ByteBufInputStream inputStream) : super(EntryType.createAccount, outputStream, inputStream);
}

class LoginEntry extends Entry<LoginCredential> {

  LoginEntry(ByteBufOutputStream outputStream, ByteBufInputStream inputStream) : super(EntryType.login, outputStream, inputStream);

  /// Switches between authenticating via password or backup verification code.
  /// This is useful for users who have lost their primary password and need an alternative method.
  Future<void> togglePasswordType() async {
    bool isAction = true;
    int actionId = LoginAction.togglePasswordType.id;

    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeBoolean(isAction);
    payload.writeInt(actionId);

    outputStream.write(payload);
  }
}

