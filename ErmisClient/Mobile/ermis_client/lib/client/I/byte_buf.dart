import 'dart:typed_data';

class ByteBuf {
  late Uint8List _buffer;

  int _readerIndex = 0;
  int _writerIndex = 0;
  
  int _markedReaderIndex = 0;
  int _markedWriterIndex = 0;

  ByteBuf(int capacity) {
    _buffer = Uint8List(capacity);
  }

  ByteBuf.wrap(Uint8List buffer) {
    _buffer = buffer;
  }

  // Named constructor initializing with a small buffer
  ByteBuf.smallBuffer() : _buffer = Uint8List(1024);

  Uint8List readBytes(int length) {
    if (readableBytes < length) {
      throw Exception("Not enough readable bytes");
    }
    int newReaderIndex = _readerIndex + length;
    var bytes = _buffer.sublist(_readerIndex, newReaderIndex);
    _readerIndex = newReaderIndex;
    return bytes;
  }

  void writeBytes(Uint8List bytes) {
    if (_writerIndex + bytes.length > _buffer.length) {
      throw Exception("Not enough writable space");
    }
    _buffer.setRange(_writerIndex, _writerIndex + bytes.length, bytes);
    _writerIndex += bytes.length;
  }

  void writeInt(int value) {
    var byteData = ByteData(4)..setInt32(0, value, Endian.big);
    writeBytes(byteData.buffer.asUint8List());
  }

  int readInt32() {
    var byteData =
        ByteData.sublistView(_buffer, _readerIndex, _readerIndex + 4);
    _readerIndex += 4;
    return byteData.getInt32(0, Endian.big);
  }

  int readInt64() {
    var byteData = ByteData.sublistView(_buffer, _readerIndex, _readerIndex + 8);
    _readerIndex += 8;
    return byteData.getInt64(0, Endian.big);
  }

  bool readBoolean() {
    bool value = buffer[_readerIndex] == 1;
    _readerIndex += 1;
    return value;
  }

  void markReaderIndex() => _markedReaderIndex = _readerIndex;
  void markWriterIndex() => _markedWriterIndex = _writerIndex;

  void resetReaderIndex() => _readerIndex = _markedReaderIndex;
  void resetWriterIndex() => _writerIndex = _markedWriterIndex;

  int get readableBytes => _writerIndex - _readerIndex;
  int get capacity => buffer.length;
  Uint8List get buffer => _buffer;
}
