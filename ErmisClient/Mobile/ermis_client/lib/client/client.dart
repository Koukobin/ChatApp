import 'dart:io';
import 'dart:async';
import 'dart:typed_data';

import 'I/input_stream.dart';
import 'I/message_handler.dart';
import 'I/output_stream.dart';

enum ServerCertificateVerification { verify, ignore }

class Client {
  static ByteBufInputStream? _inputStream;
  static ByteBufOutputStream? _outputStream;

  static SecureSocket? _sslSocket;
  static Stream<Uint8List>? broadcastStream;

  static bool _isLoggedIn = false;

  static final MessageHandler _messageHandler = MessageHandler();

  Client._() {
    throw UnsupportedError(
        "Client cannot be constructed as it is statically initialized!");
  }

  static Future<bool> initialize(InternetAddress remoteAddress, int remotePort, ServerCertificateVerification scv) async {
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

      _inputStream = ByteBufInputStream(broadcastStream: broadcastStream!);
      _outputStream = ByteBufOutputStream(secureSocket: _sslSocket!);

      return _isLoggedIn = (await _inputStream!.read()).readBoolean();
    } catch (e) {
      throw Exception("ClientInitializationException: ${e.toString()}");
    }
  }

  static void startMessageHandler() {
    if (!isLoggedIn()) {
      throw StateError(
          "User can't start writing to the server if they aren't logged in");
    }

    Client._messageHandler.setSecureSocket(_sslSocket!);
    Client._messageHandler.setByteBufInputStream(_inputStream!);
    Client._messageHandler.setByteBufOutputStream(_outputStream!);
    Client._messageHandler.startListeningToMessages();
  }

  static MessageHandler get messageHandler => Client._messageHandler;

  static bool isLoggedIn() {
    return _isLoggedIn;
  }

  static Future<void> close() async {
    if (_sslSocket != null) {
      await _sslSocket!.close();
      _sslSocket = null;
      _isLoggedIn = false;
    }
  }
}
