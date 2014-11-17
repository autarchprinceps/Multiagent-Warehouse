# Warehouse

## Status
@Daniel @Bastian: bitte aktualisieren

* Working:
	* Customer -> Warehouse -> Order -> OrderPicker -> Shelfs
* Need testing:
	* Shelfs -> OrderPicker -> Shelf
	* Shelf -> Robot (to and from OrderPicker)
	* Shelf -> OrderPicker
	* OrderPicker -> Order -> (checking) -> Warehouse -> Customer
* Possible issues:
	* What if nothing is free, nothing has item, ... (Is this in scope?)

## Arbeitsaufteilung

### Patrick Robinson
* CustomerAgentStub
* WarehouseAgent
* OrderAgent

### Daniel Pyka
* OrderPicker

### Bastian Mager
* ShelfAgent
* RobotAgent

## TODO

### Patrick Robinson
* Test returning of order (once OrderPicker, Shelfs and Robots work)
* Adapt to standardised order request format
* What if QUERY_IF never returns a CONFIRM?

### Daniel Pyka
* Debug OrderPicker
* Add more details here

### Bastian Mager
* Debug ShelfAgent & RobotAgent
* Add more details here

## Running

### Eclipse project

The Eclipse project contains a launch configuration that creates all agents with the exception of the customer simulating agent, which can be added manually  in order to debug or run the warehouse without a real customer agent.
