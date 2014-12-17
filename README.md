# Multiagent-Warehouse

## Status
* Order is successfully completed in standard case
* Configuration in JSON possible
* Added a simple GUI for the stocks of the ShelfAgents
* Orderpickers use a log file (in Agents directory) for processing orders to display their internal datastructure and workflow
* ShelfAgents and Orderpickers support a "multi-item-broadcast"
* see below or wiki/architecture for the interaction design between the agents

## TODO

##### Patrick Robinson (WarehouseAgent, OrderAgent, CustomerAgentStub)
* Debug parse config skript
* Include orders in parse config skript
 
##### Daniel Pyka (Order Picker)
* Logging for simulation software

##### Bastian Mager (ShelfAgent and Robots)
* Link Robots with that simulation software

## Agent Communication
![Alt text](/Documentation/Sequence_Diagram__Warehouse-Order-Picker__Warehouse-Order-Picker.png)

![Alt text](/Documentation/Sequence_Diagram__OrderPicker-Shelf__OrderPicker-Shelf.png)

![Alt text](/Documentation/Sequence_Diagram__Shelf-Robot__Shelf-Robot.png)

## Running

The project uses Java 8 update 25!

### Eclipse project

The Eclipse project contains a launch configuration that creates all agents with the exception of the customer simulating agent, which can be added manually  in order to debug or run the warehouse without a real customer agent.

There are two shell scripts:
1. for compiling all source files (PATH environment variable has to be set to java 8 bin directory)
2. for running jade with our agents (does not work in git shell emulation but works on linux systems JAVA_HOME variable has to be set to a Java 8 version)
