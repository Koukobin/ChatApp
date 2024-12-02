
import 'package:ermis_client/client/io/byte_buf.dart';

void main() {
  ByteBuf buffer = ByteBuf.smallBuffer(growable: true);
  buffer.markReaderIndex();
  buffer.writeInt(3405);
  for (var i = 0; i < 20000; i++) {
    buffer.writeInt(i);
  }
  buffer.resetReaderIndex();

}