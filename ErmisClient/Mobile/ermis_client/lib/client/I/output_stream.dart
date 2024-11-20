import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'byte_buf.dart';

class ByteBufOutputStream {
  SecureSocket secureSocket;

  ByteBufOutputStream({required this.secureSocket});

  void write(ByteBuf msg) async {
    int msgLength = msg.readableBytes;

    // Convert the message length to bytes (big-endian representation)
    Uint8List lengthOfMsgBytes = Uint8List(4); // Integer.BYTES is 4
    ByteData.view(lengthOfMsgBytes.buffer).setInt32(0, msgLength, Endian.big);

    // Combine the length bytes and the message bytes
    Uint8List payload = Uint8List(4 + msgLength);
    payload.setRange(0, 4, lengthOfMsgBytes); // Copy length bytes
    payload.setRange(4, 4 + msgLength, msg.buffer); // Copy message bytes

    // Add payload to output
    secureSocket.add(payload);
  }

    Future<void> sendCredentials(Map<String, String> credentials) async {
    for (var entry in credentials.entries) {
      bool isAction = false;
      var keyBytes = utf8.encode(entry.key);
      var valueBytes = utf8.encode(entry.value);

      List<int> payload = [isAction ? 1 : 0] + keyBytes + valueBytes;
      secureSocket.add(payload);
    }
  }
}
