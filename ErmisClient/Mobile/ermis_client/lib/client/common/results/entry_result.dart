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

import '../entry/added_info.dart';
import 'ResultHolder.dart';

class EntryResult {
  final ResultHolder resultHolder;
	final Map<AddedInfo, String> addedInfo;

  const EntryResult(this.resultHolder, this.addedInfo);

  bool get isSuccessful => resultHolder.isSuccessful;
  String get message => resultHolder.message;
}