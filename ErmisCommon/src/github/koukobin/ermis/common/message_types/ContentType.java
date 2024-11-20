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
package github.koukobin.ermis.common.message_types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import github.koukobin.ermis.common.util.EnumIntConverter;

/**
* @author Ilias Koukovinis
*
*/
public enum ContentType {
	TEXT(0), FILE(1);
	
	private static final HashMap<Integer, ContentType> values;
	
	static {
		values = new HashMap<>(
				Arrays.stream(ContentType.values())
				.collect(Collectors.toMap(type -> type.id, type -> type))
				);
	}
	
    public final int id;

    ContentType(int id) {
        this.id = id;
    }

	public static ContentType fromId(int id) {
		return EnumIntConverter.fromId(values, id);
	}
}

