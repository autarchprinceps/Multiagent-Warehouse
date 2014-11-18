#!/bin/bash

java -classpath "Agents/bin:Agents/lib/jade.jar:Agents/lib/common-codec-1.3.jar" jade.Boot -gui -agents warehouse:warehouse.agents.WarehouseAgent\;picker:warehouse.agents.OrderPicker\;robot_1:warehouse.agents.RobotAgent\;shelf_1:warehouse.agents.ShelfAgent"(ROTOR,5)"\;shelf_2:warehouse.agents.ShelfAgent"(STICK,10)"
