# Centralized Computing System (CCS)

## Overview
The **Centralized Computing System (CCS)** is a server application built using **Java** (JDK 1.8) that provides **Service Discovery** using UDP protocol, **Client Communication** using TCP protocol to perform arithmetic operations, **Statistics Reporting** to monitor server activity.
The CCS server can handle multiple clients concurrently and report both global and interval-based statistics periodically.

## Features
- **Service Discovery**
  - Listens for UDP messages on a specified port.
  - Responds to `CCS DISCOVER` messages with `CCS FOUND` to indicate the server's presence on the network.

- **Client Communication**
  - Listens for TCP connections on the same port.
  - Accepts requests from clients in the format: `<OPER> <ARG1> <ARG2>` (e.g., `ADD 3 5`).
  - Supports the following operations:
      - **ADD**: Addition
      - **SUB**: Subtraction
      - **MUL**: Multiplication
      - **DIV**: Division (error for division by 0)
  - Sends responses as:
      - The result of the operation, or
      - `ERROR` for invalid requests.

- **Concurrent Client Handling**
  - Handles multiple clients simultaneously, ensuring no blocking of connections.
  - Each client interaction is managed in a separate thread.

- **Statistics Reporting**
  - Gathers and reports the following:
      - Total number of connected clients.
      - Total number of processed requests.
      - Count of each type of operation (ADD, SUB, etc.).
      - Count of invalid requests.
      - Sum of computed values.
  - Prints global and interval statistics (last 10 seconds) to the console every 10 seconds.


## Technologies Used
- **Programming Language**: Java 8 (JDK 1.8)
- **Networking**: UDP and TCP Sockets
- **Multithreading**: Java Thread API for handling concurrent clients
- **Atomic Operations**: Java Concurrent Atomic Integer for safe numeric operations across operations

## How to use
### Client interaction
- **UDP Service Discovery**
  - A client broadcasts `CCS DISCOVER` on the specified UDP port.
  - The server responds with `CCS FOUND`.
- **TCP Communication**
  - The client connects to the server using the same port via TCP.
  -  Example Request: `ADD 10 5`
  -  Example Response: `15`
  -  Example Error: `ERROR` (e.g., invalid operation or arguments).
- **Statistics Reporting**
  - The server periodically prints global and interval-based statistics to the console.
### Example Console Output
- Example:
  ```
  ---------- Statistics report ----------
  Total connected clients: 2
  Total computed requests: 61
  Total computed values: 26219
  Total incorrect operations: 1
  Total DIV: 11
  Total ADD: 9
  Total SUB: 26
  Total MUL: 15
  ---------- Last 10 seconds statistics report ----------
  Last connected clients: 0
  Last computed requests: 38
  Last computed values: 9528
  Last incorrect operations: 1
  Last DIV: 8
  Last ADD: 4
  Last SUB: 18
  Last MUL: 8
  ```
  
## Project Structure
- `CCS.java` - Entry point of the program
- `src` - Directory with all used classes
- `README.md` - Project documentation
- `LICENSE` - Project license

## License
This project is licensed under the MIT License