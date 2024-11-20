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
package github.koukobin.ermis.server.main.java.databases.postgresql.ermis_database;

import java.util.Base64;

import com.password4j.Hash;
import com.password4j.HashingFunction;

/**
 * @author Ilias Koukovinis
 *
 */
public class SimpleHash {

	private static final Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
	
	private Hash hash;
	
	public SimpleHash(Hash hash) {
		this.hash = hash;
	}
	
	public SimpleHash(HashingFunction hashFunction, String result, byte[] hashBytes, String salt) {
		hash = new Hash(hashFunction, result, hashBytes, salt);
	}

	public String getResult() {
		return hash.getResult();
	}
	
	public String getSalt() {
		return hash.getSalt();
	}

	public CharSequence getPepper() {
		return hash.getPepper();
	}
	
	public String getHashString() {
		return encoder.encodeToString(hash.getBytes());
	}
	
	public byte[] getHashBytes() {
		return encoder.encode(hash.getBytes());
	}
}
