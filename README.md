# Warehouse

## Status
* Order is successfully completed in standard case
* Added a simple gui for the stocks of the shelfs
* Shelfs and Orderpickers support a "multi-item-broadcast"
* see wiki/architekture for the interaction design between the agents

## Work-Sharing

### Patrick Robinson
* CustomerAgentStub
* WarehouseAgent
* OrderAgent

### Daniel Pyka
* OrderPicker

### Bastian Mager
* ShelfAgent
* RobotAgent

## Agent Communication
![Alt text](/Documentation/Sequence_Diagram__Warehouse-Order-Picker__Warehouse-Order-Picker.png)

![Alt text](/Documentation/Sequence_Diagram__OrderPicker-Shelf__OrderPicker-Shelf.png)

![Alt text](/Documentation/Sequence_Diagram__Shelf-Robot__Shelf-Robot.png)

## TODO-List

### Patrick Robinson
* Debug parse config skript
* Include orders in parse config skript

### Daniel Pyka
* Maybe broadcast all items at once, speeds up whole orderpicker, shelf robot interaction

### Bastian Mager
* Link Robots with that simulation software

## Running

The project uses Java 8 update 25!

### Eclipse project

The Eclipse project contains a launch configuration that creates all agents with the exception of the customer simulating agent, which can be added manually  in order to debug or run the warehouse without a real customer agent.

There are two shell scripts:
1. for compiling all source files (environment variable has to be set to java 8 bin directory)
2. for running jade with our agents (currently not working, -classpath parameters are incorrect)
