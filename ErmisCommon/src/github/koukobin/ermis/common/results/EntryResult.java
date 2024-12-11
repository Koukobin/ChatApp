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
package github.koukobin.ermis.common.results;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import github.koukobin.ermis.common.entry.AddedInfo;

/**
 * @author Ilias Koukovinis
 *
 */
public final class EntryResult {
	
//	public static final class Empty {
//		
//	    private static final HashMap<ResultHolder, EntryResult> values;
//
//	    static {
//			values = new HashMap<>(
//					Arrays.stream(IsPasswordValidResult.values())
//					.collect(Collectors.toMap(type -> type.id, type -> type))
//					);
//	    }
//		
//		private Empty() {}
//	}
	
	private static final Map<AddedInfo, String> emptyAddedInfo = new EnumMap<>(AddedInfo.class);
	
	private final ResultHolder resultHolder;
	private final Map<AddedInfo, String> addedInfo;

	public EntryResult(ResultHolder resultHolder) {
		this.resultHolder = resultHolder;
		this.addedInfo = emptyAddedInfo;
	}
	
	public EntryResult(ResultHolder resultHolder, Map<AddedInfo, String> addedInfo) {
		this.resultHolder = resultHolder;
		this.addedInfo = addedInfo;
	}

	public ResultHolder getResultHolder() {
		return resultHolder;
	}
	
	public String getResultMessage() {
		return resultHolder.getResultMessage();
	}
	
	public boolean isSuccessful() {
		return resultHolder.isSuccessful();
	}

	public Map<AddedInfo, String> getAddedInfo() {
		return addedInfo;
	}

	@Override
	public int hashCode() {
		return Objects.hash(addedInfo, resultHolder);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null) {
			return false;
		}
		
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		EntryResult other = (EntryResult) obj;
		return Objects.equals(addedInfo, other.addedInfo) && Objects.equals(resultHolder, other.resultHolder);
	}

	@Override
	public String toString() {
		return "EntryResult [resultHolder=" + resultHolder + ", addedInfo=" + addedInfo + "]";
	}

}
