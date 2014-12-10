#!/usr/bin/node
var config = require("./kiva.config.json");

var cmd = "java -classpath \"Agents/bin:Agents/lib/jade.jar:Agents/lib/common-codec-1.3.jar\" jade.Boot -gui -agents warehouse:warehouse.agents.WarehouseAgent\\;"

if(config.orders.length > 0) {
	cmd += "customer:warehouse.agents.CustomerAgentParse\"(";
	for(i = 0; i < config.orders.length; i++) {
		cmd += "begin," + config.orders[i].uid + ",";
		for(j = 0; j < config.orders[i].products.length; j++) {
			cmd += config.orders[i].products[j].name + "," + config.orders[i].products[j].quantity + ",";
		}
		cmd += "end,"
	}
	cmd = cmd.substr(0, cmd.length - 1);
	cmd += ")\"\\;"
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

console.log(cmd);
require('child_process').exec(cmd, function(err, stdout, stderr) {
	console.error(err);
	var fs = require('fs');
	fs.appendFile('stdout.log', stdout);
	fs.appendFile('stderr.log', stderr);
});
