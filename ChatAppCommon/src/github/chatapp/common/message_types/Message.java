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
package github.chatapp.common.message_types;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Ilias Koukovinis
 *
 */
public final class Message {
	
	private String username;
	private int clientID;
	
	private int messageID;
	private int chatSessionID;

	private byte[] text;
	private byte[] fileName;
	
	private ContentType contentType;

	public Message() {}

	public Message(String username, int clientID, int messageID, int chatSessionID, byte[] text, byte[] fileName, ContentType contentType) {
		this.username = username;
		this.clientID = clientID;
		this.messageID = messageID;
		this.chatSessionID = chatSessionID;
		this.text = text;
		this.fileName = fileName;
		this.contentType = contentType;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setClientID(int clientID) {
		this.clientID = clientID;
	}

	public void setMessageID(int messageID) {
		this.messageID = messageID;
	}

	public void setChatSessionID(int chatSessionID) {
		this.chatSessionID = chatSessionID;
	}

	public void setText(byte[] text) {
		this.text = text;
	}

	public void setFileName(byte[] fileName) {
		this.fileName = fileName;
	}

	public void setContentType(ContentType contentType) {
		this.contentType = contentType;
	}
	
	public String getUsername() {
		return username;
	}

	public int getClientID() {
		return clientID;
	}

	public int getMessageID() {
		return messageID;
	}

	public int getChatSessionID() {
		return chatSessionID;
	}

	public byte[] getText() {
		return text;
	}

	public byte[] getFileName() {
		return fileName;
	}

	public ContentType getContentType() {
		return contentType;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(messageID);
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
		
		Message other = (Message) obj;
		return chatSessionID == other.chatSessionID && clientID == other.clientID && contentType == other.contentType
				&& Arrays.equals(fileName, other.fileName) && messageID == other.messageID
				&& Arrays.equals(text, other.text) && Objects.equals(username, other.username);
	}

	@Override
	public String toString() {
		return "Message [username=" + username + ", clientID=" + clientID + ", messageID=" + messageID
				+ ", chatSessionID=" + chatSessionID + ", text=" + Arrays.toString(text) + ", fileName="
				+ Arrays.toString(fileName) + ", contentType=" + contentType + "]";
	}

}
