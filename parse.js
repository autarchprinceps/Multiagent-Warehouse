#!/usr/bin/node
var config = require("./kiva.config.json");

var cmd = "java -classpath \"Agents/bin:Agents/lib/jade.jar:Agents/lib/common-codec-1.3.jar\" jade.Boot -gui -agents warehouse:warehouse.agents.WarehouseAgent\\;"

for(i = 0; i < config.orders.length; i++) {
	// TODO
}
for(i = 0; i < config.robots.length; i++) {
	cmd += "robot_" + config.robots[i].uid + ":warehouse.agents.RobotAgent\\;";
}
for(i = 0; i < config.pickers.length; i++) {
	cmd += "picker_" + config.pickers[i].uid + ":warehouse.agents.OrderPicker\\;";
}
for(i = 0; i < config.shelves.length; i++) {
	cmd += "shelf_" + config.shelves[i].uid + ":warehouse.agents.OrderPicker\"(";
	for(j = 0; j < config.shelves[i].products.length; j++) {
		cmd += config.shelves[i].products[j].name + "," + config.shelves[i].products[j].stock.current + ",";
	}
	cmd = cmd.substr(0, cmd.length - 1);
	cmd += ")\"\\;";
}
cmd = cmd.substr(0, cmd.length - 2);

var exec = require('child_process').exec;
exec(cmd);
