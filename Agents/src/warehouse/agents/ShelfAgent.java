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

import org.json.JSONObject;

/**
 * @author Bastian Mager <bastian.mager.2010w@informatik.h-brs.de>
 */
public class ShelfAgent extends Agent {

	public static final String SERVICE_NAME = "shelf";
	private static final long serialVersionUID = 1L;
	private static final int BCAST_TICKS = 3000;

	// FIELDS FOR CURRENT SHELF STATE
	private State currentState;
	private AID currentRobot;
	private AID currentOrderPicker;
	private String currentOrderPickerRequest;
	private TickerBehaviour broadcastRobots;

	// ITEMS THE SHELF CONTAINS
	private String item;
	private int quantity;

	private enum State {
		idle, waitForRobot, travelToOrderPicker, serveOrderPicker, travelBackHome
	}

	public ShelfAgent() {
		this.currentState = State.idle;
		this.addBehaviour(new ItemRequestProtocol());
		this.addBehaviour(new RobotRequestProtocol());
	}

	@Override
	protected void setup() {

		// INIT FILEDS WITH ARGS
		Object[] args = getArguments();
		this.item = args[0].toString();
		this.quantity = Integer.parseInt(args[1].toString());

		log("setup with item " + item + ": " + quantity);

		// REGISTER SERVICE
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
		 * EXAMPLE IN JSON: { ROTOR : 1 }
		 */
		Pair<String, Integer> requestedObject = Pair.convert(new JSONObject(
				jsonRequest));
		String requestedItem = requestedObject.getFirst();
		int requestedCount = requestedObject.getSecond();
		return item.equals(requestedItem) && requestedCount <= quantity;
	}

	private class BroadcastRobots extends TickerBehaviour {

		public BroadcastRobots(Agent a, long period) {
			super(a, period);
		}

		private static final long serialVersionUID = 1L;

		@Override
		public void onTick() {

			log("broadcast robots");

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
			ACLMessage informOrderPickerMessage = new ACLMessage(
					ACLMessage.INFORM);
			informOrderPickerMessage.setProtocol("request-shelf");
			informOrderPickerMessage.addReceiver(currentOrderPicker);
			informOrderPickerMessage.setContent(currentOrderPickerRequest);
			send(informOrderPickerMessage);
		}

	}

	private class TravelBackHome extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {

			// INFORM THE ORDER PICKER
			ACLMessage informRobotMessage = new ACLMessage(ACLMessage.INFORM);
			informRobotMessage.setProtocol("request-robot");
			informRobotMessage.addReceiver(currentRobot);
			send(informRobotMessage);
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

				case ACLMessage.PROPOSE:

					if (currentState != State.waitForRobot) {

						// WE ALREADY HAVE A ROBOT
						response.setPerformative(ACLMessage.REJECT_PROPOSAL);
						send(response);

					} else {

						// GOT A ROBOT
						response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						send(response);

						broadcastRobots.stop();
						removeBehaviour(broadcastRobots);

						// WAIT FOR ARRIVAL
						currentState = State.travelToOrderPicker;
						currentRobot = message.getSender();

					}
					break;

				case ACLMessage.INFORM:

					if (currentState == State.travelToOrderPicker) {

						// ARRIVAL AT THE ORDER PICKER
						currentState = State.serveOrderPicker;
						addBehaviour(new ArriveAtOrderPicker());

					} else if (currentState == State.travelBackHome) {

						// ARRIVAL HOME
						currentState = State.idle;
					}
					break;
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
				response.setContent(message.getContent());

				switch (message.getPerformative()) {

				case ACLMessage.QUERY_IF:

					if (hasItem(message.getContent())) {

						// ANSWER WHEN THE SHELF HAS THE ITEM
						response.setPerformative(ACLMessage.CONFIRM);
						send(response);

						log("I have items for OrderPicker: "
								+ message.getSender().getLocalName());

					}
					break;

				case ACLMessage.PROPOSE:

					if (currentState != State.idle) {

						// SHELF IS BUSY
						response.setPerformative(ACLMessage.REJECT_PROPOSAL);
						send(response);

					} else {

						log("serving now " + message.getSender().getLocalName());

						// NOW SERVE THAT PICKER
						response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						send(response);

						// BROADCAST ROBOTS
						currentState = State.waitForRobot;
						currentOrderPickerRequest = message.getContent();
						currentOrderPicker = message.getSender();
						broadcastRobots = new BroadcastRobots(myAgent,
								BCAST_TICKS);
						addBehaviour(broadcastRobots);
					}
					break;

				case ACLMessage.INFORM:

					if (currentState == State.serveOrderPicker) {
						// TRAVEL BACK HOME
						currentState = State.travelBackHome;
						currentRobot = null;
						currentOrderPicker = null;
						currentOrderPickerRequest = null;
						addBehaviour(new TravelBackHome());
					}
					break;
				}

			} else {
				block();
			}

		}
	}

	private void log(String log) {
		System.out.println(getLocalName() + ": " + log);
	}

}
