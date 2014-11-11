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

	private static final long serialVersionUID = 1L;
	public static final String SERVICE_NAME = "shelf";

	private boolean isBusy;
	private boolean hasRobot;
	private String item;
	private int quanity;

	public ShelfAgent() {
		this.item = "ROTOR";
		this.quanity = 1;
		this.isBusy = false;
		this.hasRobot = false;
		this.addBehaviour(new ItemRequestProtocol());
		this.addBehaviour(new RobotRequestProtocol());
		this.addBehaviour(new BroadcastRobots());
	}

	@Override
	protected void setup() {
		DFAgentDescription agentDesc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType(SERVICE_NAME);
		serviceDesc.setName(SERVICE_NAME);
		agentDesc.setName(getAID());
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

	protected boolean has(String jsonRequest) {
		/*
		 * EXAMPLE { Rotor : 1 }
		 */
		Pair<String, Integer> requestedObject = Pair.convert(new JSONObject(
				jsonRequest));
		String requestedItem = requestedObject.getFirst();
		int requestedCount = requestedObject.getSecond();
		return item.equals(requestedItem) && requestedCount <= quanity;
	}

	private class BroadcastRobots extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage itemBroadcast = new ACLMessage(ACLMessage.REQUEST);
			itemBroadcast.setProtocol("request-robot");

			DFAgentDescription robotDesc = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(RobotAgent.SERVICE_TYPE);
			robotDesc.addServices(sd);
			try {
				for (DFAgentDescription desc : DFService.search(myAgent,
						robotDesc)) {
					itemBroadcast.addReceiver(desc.getName());
				}
			} catch (FIPAException e) {
				e.printStackTrace();
			}

			send(itemBroadcast);
		}

	}

	private class RobotRequestProtocol extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage message = myAgent.receive(MessageTemplate
					.MatchProtocol("request-robot"));

			if (message != null) {
				ACLMessage response = message.createReply();
				switch (message.getPerformative()) {

				case ACLMessage.REFUSE:
					break;

				case ACLMessage.PROPOSE:

					if (ShelfAgent.this.hasRobot) {
						response.setPerformative(ACLMessage.REJECT_PROPOSAL);
					} else {
						response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						ShelfAgent.this.hasRobot = true;
					}
					break;

				case ACLMessage.INFORM:
					System.out.println("done");
					ShelfAgent.this.hasRobot = false;
					break;
				}

				send(response);

			} else {
				block();
			}

		}
	}

	private class ItemRequestProtocol extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {

			ACLMessage message = myAgent.receive(MessageTemplate
					.MatchProtocol("request-item"));

			if (message != null) {

				ACLMessage response = message.createReply();

				switch (message.getPerformative()) {
				case ACLMessage.QUERY_IF:

					if (ShelfAgent.this.has(message.getContent())) {
						response.setPerformative(ACLMessage.CONFIRM);
					} else {
						response.setPerformative(ACLMessage.DISCONFIRM);
					}
					break;

				case ACLMessage.PROPOSE:

					if (isBusy) {
						response.setPerformative(ACLMessage.REJECT_PROPOSAL);
					} else {
						response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					}

					myAgent.addBehaviour(new BroadcastRobots());
					break;

				default:
					response.setPerformative(ACLMessage.NOT_UNDERSTOOD);
				}

				send(response);

			} else {
				block();
			}

		}
	}

}
