package warehouse.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;

/**
 * @author Patrick Robinson
 *
 */
public class WarehouseAgent extends Agent {
	private static final long serialVersionUID = 3332937851709218564L;
	private int id_gen = 0;
	
	private class OrderRequestReceiver extends CyclicBehaviour {
		private static final long serialVersionUID = -2677509192227299216L;

		@Override
		public void action() {
			ACLMessage recMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			if(recMsg != null) {
				JSONArray json = new JSONArray(recMsg.getContent());
				Order ordr = new Order();
				ordr.id = id_gen++;
				// Confirm Order
				ACLMessage response = new ACLMessage(ACLMessage.AGREE);
				Iterator<AID> replyTo = recMsg.getAllReplyTo();
				while(replyTo.hasNext()) {
					AID tmpAID = replyTo.next();
					ordr.requestingAgents.add(tmpAID);
					response.addReceiver(tmpAID);
				}
				response.setLanguage("JSON");
				response.setContent("{id:" + ordr.id + "}");
				send(response);
				// Process Order
				for(int i = 0; i < json.length(); i++) {
					JSONObject pair = json.getJSONObject(i);
					ordr.items.add(Pair.convert(pair));
				}
				// Create Order Agent and hand over data
				String orderAgentName = "Order" + ordr.id;
				try {
					AgentController ac = getContainerController().createNewAgent(orderAgentName, "warehouse.agents.OrderAgent", new Object[]{
							ordr.items
					});
					ac.start();
				} catch(Exception ex) {
					System.err.println("Could not start OrderAgent: " + ordr.id + " " + ex.toString() + ": " + ex.getMessage());
				}
				ordr.handlingAgentAddress = new AID(orderAgentName, AID.ISLOCALNAME);
				unfinishedOrders.put(ordr.handlingAgentAddress, ordr);
			} else {
				block();
			}
		}
	}
	
	private class FinishedOrderReceiver extends CyclicBehaviour {
		private static final long serialVersionUID = -8359424326846087735L;

		@Override
		public void action() {
			ACLMessage recMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			if(recMsg != null) {
				ACLMessage response = new ACLMessage(ACLMessage.CONFIRM);
				Order ordr = unfinishedOrders.get(recMsg.getSender());
				for(AID rec : ordr.requestingAgents) {
					response.addReceiver(rec);
				}
				response.setLanguage("JSON");
				StringBuilder sb = new StringBuilder("{id:");
				sb.append(ordr.id).append(",list:[");
				for(Pair<String, Integer> p : ordr.items) {
					sb.append(p.getFirst()).append(":").append(p.getSecond()).append(",");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append("]}");
				response.setContent(sb.toString());
			} else {
				block();
			}
		}
	}
	
	private class Order {
		public AID handlingAgentAddress;
		public List<AID> requestingAgents = new ArrayList<>();
		public int id;
		public List<Pair<String, Integer>> items = new ArrayList<>();
	}
	
	private Map<AID, Order> unfinishedOrders = new HashMap<>();
	
	protected void setup() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName("WarehouseAgent");
		sd.setType("WA");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch(FIPAException ex) {
			ex.printStackTrace();
		}
		
		this.addBehaviour(new OrderRequestReceiver());
		this.addBehaviour(new FinishedOrderReceiver());
	}
	
	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch(FIPAException ex) {
			ex.printStackTrace();
		}
	}
}
