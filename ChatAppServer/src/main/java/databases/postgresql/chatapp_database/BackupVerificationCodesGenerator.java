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
package main.java.databases.postgresql.chatapp_database;

import java.security.SecureRandom;

import main.java.configs.DatabaseSettings;

/**
 * @author Ilias Koukovinis
 *
 */
final class BackupVerificationCodesGenerator {

	private static final SecureRandom secureRandom = new SecureRandom();
	
	private BackupVerificationCodesGenerator() {}
	
	public static String[] generateHashedBackupVerificationCodes(String salt) {
		
		String[] hashedBackupVerificationCodes = new String[DatabaseSettings.Client.BackupVerificationCodes.AMOUNT_OF_CODES];
		
		for (int i = 0; i < hashedBackupVerificationCodes.length; i++) {

			byte[] backupVerificationCodesByte = new byte[DatabaseSettings.Client.BackupVerificationCodes.AMOUNT_OF_CHARACTERS];
			
			secureRandom.nextBytes(backupVerificationCodesByte);

			SimpleHash hash = HashUtil.createHash(new String(backupVerificationCodesByte), salt,
					DatabaseSettings.Client.BackupVerificationCodes.Hashing.HASHING_ALGORITHM);

			String backupVerificationCode = hash.getHashString();

			hashedBackupVerificationCodes[i] = backupVerificationCode;
		}
		
		return hashedBackupVerificationCodes;
	}
}
