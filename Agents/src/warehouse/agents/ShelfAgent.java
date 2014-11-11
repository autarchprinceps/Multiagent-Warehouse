package warehouse.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import org.json.JSONObject;

/**
 * @author Bastian Mager <bastian.mager.2010w@informatik.h-brs.de>
 */
public class ShelfAgent extends Agent {

	private static final long serialVersionUID = 1L;
	public static final String SERVICE_NAME = "shelf";

	private boolean isBusy;
	private boolean hasRobot;
	private AID currentOrderPicker;

	private String item;
	private int quantity;

	public ShelfAgent() {
		this.isBusy = false;
		this.hasRobot = false;
		this.addBehaviour(new ItemRequestProtocol());
		this.addBehaviour(new RobotRequestProtocol());
	}

	@Override
	protected void setup() {

		Object[] args = getArguments();
		this.item = args[0].toString();
		this.quantity = Integer.parseInt(args[1].toString());

		log("init with item " + item + ": " + quantity);

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

	protected boolean hasItem(String jsonRequest) {
		/*
		 * EXAMPLE { Rotor : 1 }
		 */
		Pair<String, Integer> requestedObject = Pair.convert(new JSONObject(
				jsonRequest));
		String requestedItem = requestedObject.getFirst();
		int requestedCount = requestedObject.getSecond();
		return item.equals(requestedItem) && requestedCount <= quantity;
	}

	private class TravelToOrderPicker extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;

		public TravelToOrderPicker(AID sender) {
			isBusy = true;
			hasRobot = false;
			currentOrderPicker = sender;
		}

		@Override
		public void action() {

			log("will travel soon to OrderPicker " + currentOrderPicker);

			// CRY FOR ROBOTS
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

	private class ArriveAtOrderPicker extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {

			log("arrived at order picker");

			// INFORM THE ORDER PICKER
			ACLMessage informMessage = new ACLMessage(ACLMessage.INFORM);
			informMessage.setProtocol("request-shelf");
			informMessage.addReceiver(currentOrderPicker);
			send(informMessage);

			// TODO travel back?
			isBusy = false;
			hasRobot = false;
			currentOrderPicker = null;
			log("reset to init state");
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

					if (hasRobot) {
						response.setPerformative(ACLMessage.REJECT_PROPOSAL);
					} else {
						response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						log("found a robot");
						hasRobot = true;
					}
					break;

				case ACLMessage.INFORM:

					// WE ARRIVED
					addBehaviour(new ArriveAtOrderPicker());
					break;

				default:
					response = null;
				}

				if (response != null) {
					send(response);
				}

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

					if (hasItem(message.getContent())) {
						response.setPerformative(ACLMessage.CONFIRM);
					} else {
						response.setPerformative(ACLMessage.DISCONFIRM);
					}
					break;

				case ACLMessage.PROPOSE:

					if (isBusy) {
						response.setPerformative(ACLMessage.REJECT_PROPOSAL);
					} else {

						// NOW WE SERVE THAT ORDERPICKER
						response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						addBehaviour(new TravelToOrderPicker(
								message.getSender()));
					}

					break;

				default:
					response = null;
				}

				if (response != null) {
					send(response);
				}

			} else {
				block();
			}

		}
	}

	private void log(String log) {
		System.out.println(getName() + ": " + log);
	}

}
