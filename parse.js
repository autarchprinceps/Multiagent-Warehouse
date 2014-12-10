#!/usr/bin/node
// var config = JSON.parse(require("fs").readFileSync("./kiva.config.json", "utf8"));
var config = require("./kiva.config.json");

var cmd = "java -classpath \"Agents/bin:Agents/lib/jade.jar:Agents/lib/common-codec-1.3.jar\" jade.Boot -gui -agents warehouse:warehouse.agents.WarehouseAgent\\;"

for(var order in config.orders) {
	// TODO
}
for(var robot in config.robots) {
	cmd += "robot_" + robot.uid + ":warehouse.agents.RobotAgent\\;";
}
for(var picker in config.pickers) {
	cmd += "picker_" + picker.uid + ":warehouse.agents.OrderPicker\\;";
}
for(var shelf in config.shelves) {
	cmd += "shelf_" + shelf.uid + ":warehouse.agents.OrderPicker\"(";
	for(var product in shelf.products) {
		cmd += product.name + "," + product.stock.current + ",";
	}
	cmd = cmd.substr(0, cmd.length - 1);
	cmd += ")\"\\;";
}
cmd = cmd.substr(0, cmd.length - 2);

require('shelljs/global');
exec(cmd);
