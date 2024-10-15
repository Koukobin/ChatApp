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
package github.chatapp.server.main.java;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import github.chatapp.server.main.java.configs.LoggerSettings;
import github.chatapp.server.main.java.server.Server;

/**
 * @author Ilias Koukovinis
 */
public class ServerLauncher {

	static {
		LoggerSettings.initializeConfigurationFile();
	}
	
	public static void main(String[] args) {

        // Retrieve all GarbageCollectorMXBeans
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        System.out.println("Garbage Collectors in Use:");
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.println("Name: " + gcBean.getName());
            System.out.println(" - Collection Count: " + gcBean.getCollectionCount());
            System.out.println(" - Collection Time: " + gcBean.getCollectionTime() + " ms");
            System.out.println("-----------------------------------");
        }
		try (InputStream is = ServerLauncher.class.getResourceAsStream("/github/chatapp/server/main/resources/banner.txt")) {
			System.out.println(new String(is.readAllBytes())); // Print ChatApp Server banner
		} catch (IOException ioe) {
			ioe.printStackTrace(); // Shouldn't happen
		} finally {
			Server.start();
		}
	}
}
