package warehouse.agents;

import java.util.Random;

import org.json.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * @author Patrick Robinson
 *
 */
public class CustomerAgentStub extends Agent {
	private int id_gen = 0;
	private Random r = new Random();
	private String[] components = new String[]{ "Rotor", "Wing", "Body", "Engine" };
	
	protected void setup() {
		this.addBehaviour(new TickerBehaviour(this, 1000) {
			@Override
			protected void onTick() {
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				msg.addReceiver(new AID("WarehouseAgent", AID.ISLOCALNAME));
				msg.setLanguage("JSON");
				msg.setContent(generateJSON());
				msg.addReplyTo(getAID());
				send(msg);
			}
		});
		this.addBehaviour(new CyclicBehaviour() {
			@Override
			public void action() {				
				ACLMessage recMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				if(recMsg != null) {
					System.out.println("Order finished: #" + (new JSONObject(recMsg.getContent()).getInt("id")));
				}
			}
		});
	}
	
	private String generateJSON() {
		StringBuilder result = new StringBuilder("{id:");
		result.append(++id_gen);
		result.append(",list:[");
		for(String part : components) {
			result.append('{');
			result.append(part);
			result.append(':');
			result.append(r.nextInt(10));
			result.append("},");
		}
		result.deleteCharAt(result.length() - 1);
		result.append("]}");
		return result.toString();
	}
}
