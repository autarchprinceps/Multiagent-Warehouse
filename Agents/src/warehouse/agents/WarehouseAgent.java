package warehouse.agents;

import java.util.Iterator;
import java.util.function.Consumer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * @author Patrick Robinson
 *
 */
public class WarehouseAgent extends Agent {
	protected void setup() {
		this.addBehaviour(new CyclicBehaviour() {
			@Override
			public void action() {
				ACLMessage recMsg = receive();
				String content = recMsg.getContent();
				if(recMsg != null) {
					switch(recMsg.getPerformative()) {
						case ACLMessage.REQUEST:
							ACLMessage response = new ACLMessage(ACLMessage.CONFIRM);
							Iterator<AID> replyTo = recMsg.getAllReplyTo();
							while(replyTo.hasNext()) {
								response.addReceiver(replyTo.next());
							}
							response.setLanguage("JSON");
							response.setContent("{id:" +  + "}");
							break;
						case ACLMessage.INFORM:
							break;
					}
				}
			}
		});
	}
}
