# ChatApp-Server

[ilias.koukovinis@gmail.com]: https://mail.google.com/mail/u/0/?tab=rm&ogbl#search/ilias.koukovinis%40gmail.com

## Overview

**ChatApp-Server** is an open-source, Java-based server-side application that handles communication and message routing for the ChatApp platform designed for scalability and security.

## Key Features

* **Real-time communication:** Efficiently handle multiple concurrent users.
* **SSL/TLS support:** Ensure secure communication.
* **PostgreSQL integration:** Manage user data, messages and logs in a reliable relational database.

## System Requirements

* PostgreSQLv14+
* JREv17+

## Setup Instructions

To get the server running, follow these steps:

1. **Generate an SSL Certificate:** Ensure secure data transfer between clients and the server.
2. **Configure Database:**
    - Install PostgreSQL and create a new database for ChatApp.
    - The database will be autoimatically configured once the server runs.
3. **Edit Configuration Files:**
    - Navigate to `ChatApp-Server/configs/`.
    - Fill in the required fields such as database credentials, SSL details, and port numbers.
4. **Start the Server:**
	- Once configured, **use the provided scripts** to run the application directly through the terminal:

## Contributing

For contribution guidelines, please refer to the main ChatApp README file.

## Authors

* Ilias Koukovinis (2023) [ilias.koukovinis@gmail.com]*

## License

ChatApp-Server is distributed under the GNU Affero General Public License Version 3.0 which can be found in the LICENSE file.

By using this software, you agree to the terms outlined in the license agreement, which is available in the LICENSE file.