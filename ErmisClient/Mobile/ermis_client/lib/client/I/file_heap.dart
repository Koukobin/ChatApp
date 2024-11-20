import 'dart:typed_data';

class LoadedInMemoryFile {
  String fileName;
  Uint8List fileBytes;

  LoadedInMemoryFile(this.fileName, this.fileBytes);

  void setFileName(String fileName) => this.fileName = fileName;
  void setFileBytes(Uint8List fileBytes) => this.fileBytes = fileBytes;

  String get getFileName => fileName;
  Uint8List get getFileBytes => fileBytes;

  @override
  int get hashCode {
    return fileName.hashCode ^ fileBytes.hashCode;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! LoadedInMemoryFile) return false;
    return fileName == other.fileName && fileBytes == other.fileBytes;
  }

  @override
  String toString() {
    return 'File {fileName: $fileName, fileBytes: $fileBytes}';
  }
}