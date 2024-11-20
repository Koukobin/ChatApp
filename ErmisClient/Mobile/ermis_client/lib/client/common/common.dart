

class EnumNotFoundException implements Exception {
  final String message;
  EnumNotFoundException(this.message);

  @override
  String toString() => 'EnumNotFoundException: $message';
}


enum ClientCommandResultType {
  deleteChatMessage(1),
  downloadFile(2),
  getDisplayName(3),
  fetchAccountIcon(4),
  getClientId(5),
  getChatRequests(6),
  getChatSessions(7),
  getWrittenText(8),
  getDonationPage(9),
  getSourceCodePage(10);

  final int id;
  const ClientCommandResultType(this.id);
  
  static ClientCommandResultType fromId(int id) {
    try {
      return ClientCommandResultType.values
          .firstWhere((type) => type.id == id);
    } catch (e) {
      throw EnumNotFoundException('No ClientCommandResultType found for id $id');
    }
  }
}

enum CommandLevel { light, heavy }

enum ClientCommandType {
  // Account Management
  changeUsername(CommandLevel.heavy, 0),
  changePassword(CommandLevel.heavy, 1),
  addAccountIcon(CommandLevel.heavy, 2),
  logout(CommandLevel.light, 3),

  // User Information Requests
  fetchUsername(CommandLevel.light, 4),
  fetchClientId(CommandLevel.light, 5),
  fetchAccountIcon(CommandLevel.heavy, 6),

  // Chat Management
  fetchChatRequests(CommandLevel.light, 7),
  fetchChatSessions(CommandLevel.light, 8),
  sendChatRequest(CommandLevel.heavy, 9),
  acceptChatRequest(CommandLevel.heavy, 10),
  declineChatRequest(CommandLevel.heavy, 11),
  deleteChatSession(CommandLevel.heavy, 12),
  deleteChatMessage(CommandLevel.heavy, 13),
  fetchWrittenText(CommandLevel.heavy, 14),
  downloadFile(CommandLevel.heavy, 15),

  // External Pages
  requestDonationPage(CommandLevel.light, 16),
  requestSourceCodePage(CommandLevel.light, 17);

  final CommandLevel commandLevel;
  final int id;
  const ClientCommandType(this.commandLevel, this.id);

  // This function mimics the fromId functionality and throws an exception when no match is found.
  static ClientCommandType fromId(int id) {
    try {
      return ClientCommandType.values
          .firstWhere((type) => type.id == id);
    } catch (e) {
      throw EnumNotFoundException('No ClientCommandType found for id $id');
    }
  }

  CommandLevel getCommandLevel() => commandLevel;
}

enum ClientMessageType {
  clientContent(0),
  command(1);

  final int id;
  const ClientMessageType(this.id);

  // This function mimics the fromId functionality and throws an exception when no match is found.
  static ClientMessageType fromId(int id) {
    try {
      return ClientMessageType.values
          .firstWhere((type) => type.id == id);
    } catch (e) {
      throw EnumNotFoundException('No ClientMessageType found for id $id');
    }
  }
}

enum ContentType {
  text(0),
  file(1);

  final int id;
  const ContentType(this.id);

  // This function mimics the fromId functionality and throws an exception when no match is found.
  static ContentType fromId(int id) {
    try {
      return ContentType.values.firstWhere((type) => type.id == id);
    } catch (e) {
      throw EnumNotFoundException('No ContentType found for id $id');
    }
  }
}

class ResultHolder {
  final bool isSuccessful;
  final String message;

  const ResultHolder(this.isSuccessful, this.message);
}

enum ServerMessageType {
  clientContent(0),
  serverMessageInfo(1),
  commandResult(2);

  final int id;
  const ServerMessageType(this.id);

  // Mimics the `fromId` functionality, throwing an exception if no match is found.
  static ServerMessageType fromId(int id) {
    try {
      return ServerMessageType.values.firstWhere((type) => type.id == id);
    } catch (e) {
      throw EnumNotFoundException('No ServerMessageType found for id $id');
    }
  }
}

enum ChangePasswordResult {
  successfullyChangedPassword(
    id: 0,
    resultHolder: ResultHolder(true, "Successfully changed password!"),
  ),
  errorWhileChangingPassword(
    id: 1,
    resultHolder: ResultHolder(false, "There was an error while trying to change password!"),
  );

  final int id;
  final ResultHolder resultHolder;

  const ChangePasswordResult({
    required this.id,
    required this.resultHolder,
  });

  static ChangePasswordResult fromId(int id) {
    try {
      return ChangePasswordResult.values.firstWhere((type) => type.id == id);
    } catch (e) {
      throw EnumNotFoundException('No ChangePasswordResult found for id $id');
    }
  }
}

enum ChangeUsernameResult {
  successfullyChangedUsername(
    id: 0,
    resultHolder: ResultHolder(true, "Successfully changed username!"),
  ),
  errorWhileChangingUsername(
    id: 1,
    resultHolder: ResultHolder(false, "There was an error while trying to change username!"),
  );

  final int id;
  final ResultHolder resultHolder;

  const ChangeUsernameResult({
    required this.id,
    required this.resultHolder,
  });

  static ChangeUsernameResult fromId(int id) {
    try {
      return ChangeUsernameResult.values.firstWhere((type) => type.id == id);
    } catch (e) {
      throw EnumNotFoundException('No ChangeUsernameResult found for id $id');
    }
  }
}

enum IsPasswordValidResult {
  successfullyValidatedPassword(
    id: 1,
    resultHolder: ResultHolder(true, "Successfully validated password!"),
  ),
  requirementsNotMet(
    id: 2,
    resultHolder: ResultHolder(false, "Password requirements not met!"),
  );

  final int id;
  final ResultHolder resultHolder;

  const IsPasswordValidResult({
    required this.id,
    required this.resultHolder,
  });

  static IsPasswordValidResult fromId(int id) {
    try {
      return IsPasswordValidResult.values.firstWhere((type) => type.id == id);
    } catch (e) {
      throw EnumNotFoundException('No IsPasswordValidResult found for id $id');
    }
  }
}

enum RegenerateBackupVerificationCodesResult {
  successfullyRegeneratedBackupVerificationCodes(
    id: 1,
    resultHolder: ResultHolder(true, "Successfully regenerated backup verification codes!"),
  ),
  errorWhileChangingUsername(
    id: 2,
    resultHolder: ResultHolder(false, "There was an error while trying to change username!"),
  );

  final int id;
  final ResultHolder resultHolder;

  const RegenerateBackupVerificationCodesResult({
    required this.id,
    required this.resultHolder,
  });

  static RegenerateBackupVerificationCodesResult fromId(int id) {
    try {
      return RegenerateBackupVerificationCodesResult.values.firstWhere((type) => type.id == id);
    } catch (e) {
      throw EnumNotFoundException('No RegenerateBackupVerificationCodesResult found for id $id');
    }
  }
}