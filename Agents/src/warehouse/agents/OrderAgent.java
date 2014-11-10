package warehouse.agents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * @author Patrick Robinson
 *
 */
public class OrderAgent extends Agent {
	private static final long serialVersionUID = -2339964650250401939L;

	private class OrderPickerQuerier extends OneShotBehaviour {
		private static final long serialVersionUID = -4536389382809513479L;

		@Override
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
			msg.setLanguage("English");
			msg.setContent("free");
			DFAgentDescription orderPickerDesc = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setName("pick");
			orderPickerDesc.addServices(sd);
			try {
				for(DFAgentDescription desc : DFService.search(OrderAgent.this, orderPickerDesc)) {
					msg.addReceiver(desc.getName());
				}
			} catch(FIPAException ex) {
				ex.printStackTrace();
			}
			send(msg);
			pick = new OrderPickerPicker();
			addBehaviour(pick);
			removeBehaviour(query);
			query = null;
		}
	}
	
	private class OrderPickerPicker extends CyclicBehaviour {
		private static final long serialVersionUID = 1757684378347650720L;

		@Override
		public void action() {
			// TODO what if no confirm is received, because no OrderPicker is free?
			ACLMessage recMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
			if(recMsg != null) {
				ACLMessage response = new ACLMessage(ACLMessage.REQUEST);
				response.setLanguage("JSON");
				response.addReceiver(response.getSender());
				
				StringBuilder sb = new StringBuilder('[');
				for(Pair<String, Integer> p : items.keySet()) {
					sb.append(p.getFirst()).append(":").append(p.getSecond()).append(",");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append(']');
				response.setContent(sb.toString());
				send(response);
				ack = new AckRecv();
				addBehaviour(ack);
				removeBehaviour(pick);
				pick = null;
			} else {
				block();
			}
		}
		
	}
	
	private class AckRecv extends CyclicBehaviour {
		@Override
		public void action() {
			ACLMessage agree = receive(MessageTemplate.MatchPerformative(ACLMessage.AGREE));
			ACLMessage cancel = receive(MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
			if(agree != null) {
				finish = new FinishChecker();
				addBehaviour(finish);
				removeBehaviour(ack);
				ack = null;
			} else {
				if(cancel != null) {
					query = new OrderPickerQuerier();
					addBehaviour(query);
					removeBehaviour(ack);
					ack = null;
				} else {
					block();
				}
			}
		}
		
	}
	
	private class FinishChecker extends CyclicBehaviour {
		private static final long serialVersionUID = -7280941638703277585L;

		private void failure() {
			removeBehaviour(finish);
			finish = null;
			System.err.println("Failed order. Retrying...");
			query = new OrderPickerQuerier();
			addBehaviour(query);
			items.forEach((Pair<String, Integer> K, Boolean V) -> {
				items.put(K, false);
			});
		}
		
		private void success() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setLanguage("English");
			msg.setContent("checked");
			DFAgentDescription whDesc = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setName("WarehouseAgent");
			whDesc.addServices(sd);
			try {
				DFAgentDescription[] desc = DFService.search(OrderAgent.this, whDesc);
				if(desc.length == 1) {
					msg.addReceiver(desc[0].getName());
				} else {
					if(desc.length > 1) {
						System.err.println("Multiple WarehouseAgents found");
					} else {
						System.err.println("No WarehouseAgent found");
					}
				}
			} catch(FIPAException ex) {
				ex.printStackTrace();
			}
			send(msg);
			removeBehaviour(finish);
			finish = null;
			done = true; // TODO does this work?
		}
		
		@Override
		public void action() {
			ACLMessage recMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			if(recMsg != null) {
				JSONArray arr = new JSONArray(recMsg.getContent());
				for(int i = 0; i < arr.length(); i++) {
					JSONObject elem = arr.getJSONObject(i);
					Pair<String, Integer> p = Pair.convert(elem);
					if(items.containsKey(p)) {
						if(items.get(p)) {
							failure();
							return;
						} else {
							items.put(p, true);
						}
					} else {
						failure();
						return;
					}
				}
				items.forEach((Pair<String, Integer> K, Boolean V) -> {
					if(!V) {
						failure();
						return;
					}
				});
				success();
			} else {
				block();
			}
		}
		
	}
	
	private Map<Pair<String, Integer>, Boolean> items;
	
	private Behaviour query;
	private Behaviour pick;
	private Behaviour ack;
	private Behaviour finish;
	
	private boolean done;
	
	protected void setup() {
		done = false;
		items = new HashMap<>();
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		try {
			DFService.register(this, dfd);
		} catch(FIPAException ex) {
			ex.printStackTrace();
		}
		
		Object[] args = this.getArguments();
		if(args != null && args.length == 1 && args[0] instanceof List<?>) {
			for(Pair<String, Integer> p : (List<Pair<String, Integer>>)args[0]) {
				items.put(p, false);
			}
		} else {
			System.err.println("[OrderAgent]: Argument Error");
		}
		query = new OrderPickerQuerier();
		
		finish = new FinishChecker();
		addBehaviour(query);
	}
	
	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch(FIPAException ex) {
			ex.printStackTrace();
		}
	}
	
	protected boolean done() {
		return done;
	}
}
