package warehouse.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

public class CustomerAgentParse extends Agent {
	private static final long serialVersionUID = 2904092632296557020L;
	
	private List<JSONObject> orders;

	protected void setup() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		try {
			DFService.register(this, dfd);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}
		
		Object[] args = getArguments();
		orders = new ArrayList<>();
		JSONObject order = null;
		for(int i = 0; i < args.length; i++) {
			if(args[i].toString().equals("begin")) {
				order = new JSONObject();
				order.put("id", Integer.parseInt(args[++i].toString()));
				JSONArray products = new JSONArray();
				while(!args[++i].toString().equals("end")) {
					JSONObject product = new JSONObject();
					product.put(args[i++].toString(), Integer.parseInt(args[i].toString()));
					products.put(product);
				}
				order.put("products", products);
				orders.add(order);
			}
		}
		orders.forEach((ordr) -> {
			System.err.println(ordr);
		});
		
		this.addBehaviour(new OneShotBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {			
				DFAgentDescription whDesc = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setName("WarehouseAgent");
				sd.setType("WA");
				whDesc.addServices(sd);
				AID wh;
				try {
					DFAgentDescription[] desc = DFService.search(
							CustomerAgentParse.this, whDesc);
					if (desc.length == 1) {
						wh = desc[0].getName();
					} else {
						if (desc.length > 1) {
							System.err
									.println("Multiple WarehouseAgents found");
						} else {
							System.err.println("No WarehouseAgent found");
						}
						return;
					}
				} catch (FIPAException ex) {
					ex.printStackTrace();
					return;
				}
				for(JSONObject order : orders) {
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					msg.addReceiver(wh);
					msg.setLanguage("JSON");
					msg.setContent(order.toString());
					msg.addReplyTo(getAID());
					send(msg);
				}
			}
		});
		this.addBehaviour(new CyclicBehaviour() {
			private static final long serialVersionUID = -1738591542396043852L;

			@Override
			public void action() {
				ACLMessage recMsg = receive(MessageTemplate.or(
						MessageTemplate.MatchPerformative(ACLMessage.FAILURE),
						MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
				if (recMsg != null) {
					System.out.println("Order finished: " + recMsg.getContent());
					System.out.println();
				} else {
					block();
				}
			}
		});
	}

	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}
	}
}
