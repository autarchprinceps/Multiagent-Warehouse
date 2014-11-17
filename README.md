# Warehouse

## Status

* Working:
	* Customer -> Warehouse -> Order -> OrderPicker -> Shelfs -> Robot
	* see wiki/architekture for the interaction design between the agents
* Need testing:
	* Shelfs -> OrderPicker -> Shelf
	* Shelf -> Robot (to and from OrderPicker)
	* Shelf -> OrderPicker
	* OrderPicker -> Order -> (checking) -> Warehouse -> Customer
* Possible issues:
	* What if nothing is free
	* What if some items are not available (is this in scope?)
	* ...

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

## TODO-List

### Patrick Robinson
* Test returning of order (once OrderPicker, Shelfs and Robots work)
* Adapt to standardised order request format
* What if QUERY_IF never returns a CONFIRM?

### Daniel Pyka
* Debug OrderPicker
* If no other agents answer (for example on item broadcasts) the whole workflow stops
* Refactor Hashmaps which use Pair<String, Integer or Boolean). References to keys break. Need to search for getFirst and getSecond to find correct key again.

### Bastian Mager
* Debug ShelfAgent
* Debug RobotAgent
* Model that a Shelf has more than one item
* Model that the Shelfs inventory decreases (?) 
* Link Robots with that simulation software (?)
## Running

The project uses Java 8 update 25!

### Eclipse project

The Eclipse project contains a launch configuration that creates all agents with the exception of the customer simulating agent, which can be added manually  in order to debug or run the warehouse without a real customer agent.

There are two shell scripts:
1. for compiling all source files (environment variable has to be set to java 8 bin directory)
2. for running jade with our agents (currently not working, -classpath parameters are incorrect)
