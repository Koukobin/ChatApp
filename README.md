# ChatApp

[ilias.koukovinis@gmail.com]: https://mail.google.com/mail/u/0/?tab=rm&ogbl#search/ilias.koukovinis%40gmail.com

[![GitHub Stars](https://img.shields.io/github/stars/Koukobin/Chatapp?style=social)](https://github.com/Koukobin/ChatApp/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/Koukobin/Chatapp?style=social)](https://github.com/Koukobin/ChatApp/network/members)
[![GitHub Issues](https://img.shields.io/github/issues/Koukobin/Chatapp)](https://github.com/Koukobin/ChatApp/issues)
[![GitHub Pull Requests](https://img.shields.io/github/issues-pr/Koukobin/Chatapp)](https://github.com/Koukobin/ChatApp/pulls)
[![License](https://img.shields.io/github/license/Koukobin/Chatapp)](https://github.com/Koukobin/ChatApp/blob/master/LICENSE)

ChatApp is a suite of applications designed for secure and seamless communication. The project consists of the following components:

- **ChatApp-Client**: The user-facing client application for communication.
- **ChatApp-Server**: The backend server that handles everything related to server-side.
- **ChatApp-Common**: The shared library containing common functionalities and utilities used by both the client and server.

## Vision

My vision while creating ChatApp was to develop a server-side application that anyone can deploy with ease and efficiency to ensure it can run on even the oldest hardware. Alongside this, I aimed to build a client-side application - that boasts an intuitive and user-friendly interface - with a solid performance to complement the server. With a primary focus on security, the ChatApp platform employs robust encryption methods to ensure the protection of user data and privacy. By prioritizing security measures, I aimed to create a platform where users can communicate freely and openly, knowing their information is safe from prying eyes. The ultimate goal is to create a platform that anyone can host and communicate with.

## Challenges Encountered

I have dedicated an immense amount of my life to building and refining this suite of applications. Finally seeing it emerge is cathartic, to say the least. The journey has been tough; I’ve genuinely lost count of how many times I’ve gone mad at a software bug or error. I have encountered hundreds of issues and problems, and resolving them took more time than I would like to admit.

Since this is my first project of such enormous magnitude, I faced challenges particularly related to maintainability and readability — aspects of code I had not dealt with before. I have lost track of the number of times I had to redo entire sections of the application upon discovering a more efficient approach. For instance, I initially started with a UDP server but realized that for a chatting application, a TCP server was more suitable.

I also began utilizing IO socket channels and much later down the line realized that NIO socket channels are best suited for such servers. Then I attempted to build my own NIO socket channel communication system. Ultimately, however, my endeavors were in vain and after some quick research, I realized there are frameworks specifically dedicated to these kinds of purposes. In the end, for the initial version of ChatApp, I settled on Netty, a non-blocking I/O client-server framework.

I encountered similar back-and-forths with the database - alternating between MySQL, PostgreSQL, MariaDB and maybe even considering Cassandra at some point. Ultimately, I settled on PostgreSQL.

## Features

* **Real-time communication:** Efficiently handle multiple concurrent users.
- **Media Sharing**: Easily share images, videos, and documents within your chats.
- **Secure Connections**: Integrated with end-to-end encryption to ensure privacy and security.
* **PostgreSQL integration:** Manage user data, messages, and logs in a reliable relational database.

## Future Enhancements

Moving forward, I do not have any particular plans regarding ChatApp's development in mind right now. As a general idea I would like to:

- **Enhance Scalability**: Further optimize the server-side application to handle even larger loads.
- **Improve Code Readability**: Refactor the codebase to improve maintainability and readability.
- **Expand Features**: Introduce new functionalities in order to enrich user experience.

## Screenshots

### Login Screen
<img width="1000" alt="Login Screen" src="https://github.com/Koukobin/ChatApp/blob/master/Images/login-screen.png"/>

### Create Account Screen
<img width="1000" alt="Create Account screen" src="https://github.com/Koukobin/ChatApp/blob/master/Images/create_account-screen.png"/>

### Messaging Interface
<img width="1000" alt="Messaging Interface" src="https://github.com/Koukobin/ChatApp/blob/master/Images/messaging-interface.png"/>



## Getting Started

To set up the project locally, follow the instructions for each component:

1. [ChatApp-Client](./chatapp-client/README.md)
2. [ChatApp-Server](./chatapp-server/README.md)
3. [ChatApp-Common](./chatapp-common/README.md)

## Potential Questions

* Q: Is there a mobile version of ChatApp?
	- A: Currently, ChatApp-Client is designed for desktop platforms only.
* Q: Is there a macOS version of ChatApp?
	- A: At the moment, ChatApp-Client is designed for Windows and Linux. A macOS version is not available at this time, but future updates may include support for the macOS platform as well.
* Q: Is ChatApp available for all Linux distributions?
	- A: Unfortunately, at present, ChatApp is available exclusively for Debian-based distributions (such as Ubuntu, Linux Mint, Kali Linux). In the future, I hope to include support for Red Hat-based distributions (like Fedora and CentOS) and other distros, such as Arch Linux.
* Q: Is my data secure while using ChatApp?
    - A: Yes, ChatApp employs robust encryption methods to ensure the protection of user data and privacy.
    
## Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Ensure your code adheres to the project's style and conventions.
4. Submit a pull request with a detailed description of your changes.

## Report an issue

To report bugs, security vulnerabilities, or any other issues for that matter, please use the GitHub Issues tab to report them. Alternatively, you can contact me directly via email here: [ilias.koukovinis@gmail.com]

When reporting an issue, please include:
- A clear and descriptive title.
- Steps to reproduce the issue.
- Logs or error messages if available.
- Your environment details (OS, version, etc.).

## Authors

* Ilias Koukovinis (2023) [ilias.koukovinis@gmail.com]

## License

ChatApp is distributed under the GNU Affero General Public License Version 3.0 which can be found in the `LICENSE` file.

By using this software, you agree to the terms outlined in the license agreement, which is available in the LICENSE file.
