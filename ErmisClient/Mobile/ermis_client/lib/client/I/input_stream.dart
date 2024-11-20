import 'dart:typed_data';

import 'byte_buf.dart';

class ByteBufInputStream {
  Stream<Uint8List> broadcastStream;

  ByteBufInputStream({required this.broadcastStream});

  Future<ByteBuf> read() async {
    Uint8List data = await broadcastStream.first;
    return decode(data);
  }

  static ByteBuf decode(Uint8List data) {

    Uint8List lengthOfMsgBytes = data.sublist(0, 4);
    int messageLength = ByteBuf.wrap(lengthOfMsgBytes).readInt32();

    Uint8List message = data.sublist(4, messageLength + 4);
    return ByteBuf(messageLength)..writeBytes(message);
  }

  Stream<Uint8List> get stream => broadcastStream;
}
