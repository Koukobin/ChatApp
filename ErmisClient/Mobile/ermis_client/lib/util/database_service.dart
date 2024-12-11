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

import 'package:flutter/widgets.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

class Database {
  Database._() {
    throw Exception(
        "Database cannot be constructed since it is statically initialized!");
  }

  static DBConnection createDBConnection() {
    return DBConnection._create();
  }
}

class DBConnection {

  final dynamic _database;

  DBConnection._(this._database);

  factory DBConnection._create() {
    var database = _initializeDB();
    return DBConnection._(database);
  }

  static dynamic _initializeDB() async {
    // "Avoid errors caused by flutter upgrade.
    // Importing 'package:flutter/widgets.dart' is required." by flutter documentation
    // P.S. I don't know exactly why this is necessary, but the documentation said so
    WidgetsFlutterBinding.ensureInitialized();
    return openDatabase(
      join(await getDatabasesPath(), 'ermis_sqlite_database_1.db'),
      onCreate: (db, version) {
        return db.execute(
          "CREATE TABLE IF NOT EXISTS server_info (server_url TEXT NOT NULL, "
          "email TEXT, "
          "password_hash TEXT, "
          "last_used DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
          "PRIMARY KEY(server_url));",
        );
      },
      version: 1,
    );
  }

  Future<void> setUserInformation(UserInformation userInfo, ServerInfo serverInfo) async {
    final db = await _database;

    await db.update(
      'server_info',
      {'email': userInfo.email, 
      'password_hash': userInfo.passwordHash
      },
      where: 'server_url = ?',
      whereArgs: [
        serverInfo.serverUrl.toString() // Identifier
      ],
    );
  }

  Future<UserInformation> getUserInformation(ServerInfo serverInfo) async {
    final db = await _database;

    final userInfoMap = await db.query(
      "server_info",
      columns: ["email", "password_hash"],
      where: 'server_url = ?',
      whereArgs: [serverInfo.toString()],
    );
    final String? email = userInfoMap.first['email'] as String?;
    final String? passwordHash = userInfoMap.first['password_hash'] as String?;

    return UserInformation(email: email, passwordHash: passwordHash);
  }

  Future<void> updateServerUrlLastUsed(ServerInfo serverInfo) async {
    final db = await _database;

    await db.update(
      'server_info',
      {'last_used': DateTime.now().toIso8601String()}, // Set current timestamp
      where: 'server_url = ?',
      whereArgs: [
        serverInfo.serverUrl.toString() // Identifier
      ],
    );
  }

  Future<void> insertServerInfo(ServerInfo info) async {
    final db = await _database;

    info.lastUsed = DateTime.now();
    await db.insert(
      'server_info',
      info.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> removeServerInfo(ServerInfo info) async {

    final db = await _database;

    await db.delete(
      'server_info',
      where: 'server_url = ?',
      whereArgs: [info.serverUrl.toString()]
    );
  }

  Future<List<ServerInfo>> getServerUrls() async {
    final db = await _database;

    final List<Map<String, Object?>> serverInfoMap = await db.query('server_info');

    List<ServerInfo> servers = serverInfoMap.map((record) {
      final String serverUrl = record['server_url'] as String;
      final String? lastUsed = record['last_used'] as String?;

      return ServerInfo(
        Uri.parse(serverUrl),
        DateTime.parse(lastUsed ??
            DateTime.fromMillisecondsSinceEpoch(0).toIso8601String()),
      );
    }).toList();

    servers.sort((a, b) {
      final DateTime lastUsedA = a.lastUsed!;
      final DateTime lastUsedB = b.lastUsed!;

      return lastUsedB.compareTo(lastUsedA); // Most recent first
    });

    return servers;
  }
}

class UserInformation {
  final String? email;
  final String? passwordHash;

  UserInformation({required this.email, required this.passwordHash});

  bool isValid() => email != null || passwordHash != null;
  bool isNotValid() => !isValid();
}

class InvalidServerUrlException implements Exception {

  String message;

  InvalidServerUrlException(this.message);

  @override
  String toString() {
    return message;
  }
}

class ServerInfo {
  final Uri _serverUrl;
  final InternetAddress _address;
  final int _port;
  DateTime? lastUsed;

  factory ServerInfo(Uri serverUrl, [DateTime? lastUsed]) {
    if (!serverUrl.toString().startsWith("https://")) {
      serverUrl = Uri.parse("https://$url");
    }

    // Check if url is valid
    if (!(serverUrl.hasScheme && serverUrl.hasAuthority)) {
      throw InvalidServerUrlException("Invalid server URL: $serverUrl");
    }
    
    InternetAddress address = InternetAddress(serverUrl.host);
    int port = serverUrl.port;

    return ServerInfo._(serverUrl, address, port, lastUsed);
  }

  ServerInfo._(this._serverUrl, this._address, this._port, this.lastUsed);

  Map<String, Object?> toMap() {
    return {
      'server_url': _serverUrl.toString(),
    };
  }

  Uri get serverUrl => _serverUrl;
  InternetAddress get address => _address;
  int get port => _port;

  @override
  String toString() {
    return serverUrl.toString();
  }
}
