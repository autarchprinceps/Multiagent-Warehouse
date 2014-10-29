package warehouse.agents;

import org.json.JSONObject;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * @author Bastian Mager <bastian.mager.2010w@informatik.h-brs.de>
 */
public class ShelfAgent extends Agent {

	public static final String SERVICE_NAME = "shelf";

	private String item;
	private int count;

	public ShelfAgent(String item, int count) {
		this.item = item;
		this.count = count;
		this.addBehaviour(new WaitForMessages());
	}

	@Override
	protected void setup() {
		DFAgentDescription agentDesc = new DFAgentDescription();
		agentDesc.setName(getAID());
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setName(SERVICE_NAME);
		agentDesc.addServices(serviceDesc);

		try {
			DFService.register(this, agentDesc);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	protected boolean has(String requestedItem, int requestedCount) {
		return item.equals(requestedItem) && requestedCount <= count;
	}

	private class CryForRobot extends OneShotBehaviour {

		@Override
		public void action() {
			// TODO Auto-generated method stub
		}

	}

	private class WaitForMessages extends CyclicBehaviour {

		@Override
		public void action() {

			ACLMessage message = myAgent.receive();

			MessageTemplate request = MessageTemplate
					.MatchPerformative(ACLMessage.REQUEST);
			MessageTemplate propose = MessageTemplate
					.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);

			if (message != null) {

				ACLMessage response = message.createReply();

				if (request.match(message)) {

					/*
					 * EXAMPLE { Rotor : 1 }
					 */
					String content = message.getContent();
					Pair<String, Integer> requestedObject = Pair
							.convert(new JSONObject(content));
					String requestedItem = requestedObject.getFirst();
					int requestedCount = requestedObject.getSecond();

					if (ShelfAgent.this.has(requestedItem, requestedCount)) {

						// we accept

					} else {

						// we decline

					}

				} else if (propose.match(message)) {

				}

				myAgent.send(response);

			} else {
				block();
			}

		}
	}

}
