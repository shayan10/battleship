# Navigate, Strategize, Dominate: Rule the Seas!

A multiplayer Battleship game implementation with advanced features.

## Table of Contents
1. [Overview](#overview)
2. [Features](#features)
3. [Technologies Used](#technologies-used)
4. [Prerequisites](#prerequisites)
5. [Installation](#installation)
6. [Usage](#usage)
7. [Team Members](#team-members)
8. [Acknowledgements](#acknowledgements)

## Overview

This project is a networked implementation of the classic Battleship game, featuring both player-vs-player and player-vs-AI modes. It includes advanced features such as an Elo ranking system, asynchronous AI processing, and a player-to-player chat system.

![Screenshot from 2024-07-15 18-32-14](https://github.com/user-attachments/assets/9ac086c1-d00a-4106-82c5-4e50d4e77f4a)


## Features

- Multiplayer gameplay (PvP and PvAI)
- Elo ranking system for skill-based matchmaking
- Asynchronous AI processing for smooth gameplay
- In-game chat system for player communication
- Background music for enhanced user experience
- User-friendly GUI with intuitive boat placement and attack mechanics

## Technologies Used

- Java 8
- JavaFX 19.0.2.1
- Maven
- SQLite
- HikariCP (Connection Pooling)
- JUnit 5 (for testing)

## Prerequisites

Before you begin, ensure you have the following installed:
- Java Development Kit (JDK) 8 or later
- Maven 3.6.0 or later
- Git (for cloning the repository)

## Installation

1. Clone the repository:
   ```
   git clone https://github.com/your-username/battleship-game.git
   cd battleship-game
   ```

3. Run the server:
   ```
   cd server
   mvn clean compile exec:java
   ```

4. Run the client (in a separate terminal):
   ```
   cd client
   mvn clean compile exec:java
   ```

Note: Ensure that you have the necessary permissions to execute Maven commands and that your system's PATH includes the directory containing the `mvn` executable.

## Usage

1. Start the server:
   Run the server application using the command provided in the Installation section.

2. Launch the client:
   Start the client application for each player who wants to join the game.

3. Log in:
   - Enter a unique username when prompted.
   - The server will validate the username to ensure only one user is logged in with that name.

4. Matchmaking:
   - The game uses an Elo ranking system for matchmaking.
   - New players start with a ranking of 1300.
   - The server will wait until 4 players have joined and then match them based on their Elo rankings.

5. Boat Placement:
   - Once matched, you'll be presented with a grid to place your boats.
   - Click on the grid to select a position for each boat.
   - Choose the orientation of the boat (horizontal or vertical).
   - The game will highlight invalid placements and prevent overlapping boats.

6. Gameplay:
   - Take turns guessing the location of your opponent's boats by clicking on the opponent's grid.
   - The game will indicate hits and misses.
   - Use the in-game chat system to communicate with your opponent.

7. AI Opponent:
   - If playing against an AI, the computer will make its moves automatically.
   - The AI uses a combination of random guessing and strategic decision-making.

8. Winning the Game:
   - The first player to sink all of their opponent's ships wins.
   - Your Elo ranking will be updated based on the game outcome.

9. Post-Game:
   - After the game ends, you can choose to play again or exit.
   - Your updated Elo ranking will be displayed.

Note: The game features background music that changes with different scenes to enhance the gaming experience. However, this feature may not work on macOS due to media dependency issues.

Tips:
- Hover over grid cells to preview boat placement or potential attack positions.
- Use the chat system to engage with your opponent and add a social element to the game.
- Pay attention to your Elo ranking as it will affect your future matchmaking.

## Team Members

- Mohammad Shayan Khan: Backend development, game logic, and server-side implementation
- Anupam Sai Sistla: Backend development, client-server integration, and project reporting
- Nathan Trinh: Frontend development, wireframing, and GUI design
- Rayyan Athar: Frontend implementation, GUI components, and game mechanics
