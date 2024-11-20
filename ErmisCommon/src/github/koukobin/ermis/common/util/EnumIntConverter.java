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
package github.koukobin.ermis.common.util;

import java.util.Map;

/**
 * @author Ilias Koukovinis
 *
 */
public final class EnumIntConverter {
	
	private EnumIntConverter() {}
	
	/**
	 * Retrieves an enum constant from a map by its ID.
	 *
	 * @param <T> the type of enum
	 * @param map the map containing enum values, with the ID as the key
	 * @param id the ID to look up
	 * @return the enum constant corresponding to the provided ID
	 * @throws IllegalArgumentException if the ID does not exist in the map
	 */
	public static <T extends Enum<T>> T fromId(Map<Integer, T> map, int id) {
		
		T result = map.get(id);

		if (result == null) {
			throw new IllegalArgumentException("No enum constant with ID " + id + " exists for " 
		            + map.getClass().getTypeName());
		}

		return result;
	}

	/**
	 * 
	 * @deprecatedThis This method will be removed in a future release. It is
	 *                 currently used in some cases, primarily to retrieve the IDs
	 *                 of enums in `ermis.common.entry`. It will be updated once the
	 *                 remaining dependencies are refactored.
	 */
	@Deprecated(since = "1.0-rc", forRemoval = true)
	public static int getEnumAsInt(Enum<?> enumm) {
		return enumm.ordinal();
	}

	/**
	 * 
	 * @param <T>
	 * @param enumIndex
	 * @param clazz
	 * @return
	 * 
	 * @throws IndexOutOfBoundsException if enum index doesn't exist in enum.
	 * 
	 * @deprecated This This method will be removed in a future release. It is
	 *             currently used in some cases, primarily to retrieve the IDs of
	 *             enums in `ermis.common.entry`. It will be updated once the
	 *             remaining dependencies are refactored.
	 */
	@Deprecated(since = "1.0-rc", forRemoval = true)
	public static <T extends Enum<T>> T getIntAsEnum(int enumIndex, Class<T> clazz) {
		return clazz.getEnumConstants()[enumIndex];
	}
}
