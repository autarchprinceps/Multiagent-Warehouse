package warehouse.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * @author Bastian Mager <bastian.mager.2010w@informatik.h-brs.de>
 */
public class ShelfAgent extends Agent {

	public static final String SERVICE_NAME = "shelf";
	private static final int BCAST_FREQ = 3000;
	private static final int WAIT_LIMIT = 5000;
	private static final long serialVersionUID = 1L;

	// FIELDS FOR CURRENT SHELF STATE
	private State currentState;
	private AID currentRobot;
	private AID currentOrderPicker;
	private String currentItemRequest;

	private TickerBehaviour broadcastRobots;
	private PickupWait currentPickupWaitBehaviour;

	// INVENTORY
	private Map<String, Integer> inventory = new HashMap<String, Integer>();

	private static enum State {
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

		for (int argIndex = 0; argIndex < args.length; argIndex += 2) {

			String newItem = args[argIndex].toString();
			Integer newQuantity = Integer.parseInt(args[argIndex + 1]
					.toString());

			inventory.put(newItem, newQuantity);
		}

		log("setup with items " + getInventoryString());

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

	private String getInventoryString() {
		String inventoryString = "{ ";
		for (String item : inventory.keySet()) {
			inventoryString += item += ":" + inventory.get(item) + " ";
		}
		inventoryString += "}";
		return inventoryString;
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
		int requestedQuantity = requestedObject.getSecond();

		if (inventory.containsKey(requestedItem)) {
			if (inventory.get(requestedItem) >= requestedQuantity) {
				return true;
			}
		}

		return false;
	}

	protected void decreaseInventory(String jsonRequest) {
		Pair<String, Integer> requestedObject = Pair.convert(new JSONObject(
				jsonRequest));
		String requestedItem = requestedObject.getFirst();
		int requestedQuantity = requestedObject.getSecond();

		Integer oldQuantity = inventory.get(requestedItem);
		inventory.put(requestedItem, oldQuantity - requestedQuantity);
	}

	private class BroadcastRobots extends TickerBehaviour {

		public BroadcastRobots(Agent a, long period) {
			super(a, period);
		}

		private static final long serialVersionUID = 1L;

		@Override
		public void onTick() {

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

			// INFORM THE ORDER PICKER
			ACLMessage informOrderPickerMessage = new ACLMessage(
					ACLMessage.INFORM);
			informOrderPickerMessage.setProtocol("request-item");
			informOrderPickerMessage.addReceiver(currentOrderPicker);
			informOrderPickerMessage.setContent(currentItemRequest);
			send(informOrderPickerMessage);

			currentPickupWaitBehaviour = new PickupWait(myAgent, WAIT_LIMIT);
			myAgent.addBehaviour(currentPickupWaitBehaviour);
		}

	}

	private class PickupWait extends WakerBehaviour {

		private static final long serialVersionUID = 1L;

		public PickupWait(Agent a, long timeout) {
			super(a, timeout);
		}

		@Override
		protected void handleElapsedTimeout() {
			currentState = State.travelBackHome;
			log("ABORT: " + currentOrderPicker.getLocalName()
					+ " is not interested anymore...");
			myAgent.addBehaviour(new TravelBackHome());
		}

	}

	private class TravelBackHome extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
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
						log("ACCEPT_PROPOSAL with "
								+ message.getSender().getLocalName());
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
						log("INFORM arrived at "
								+ currentOrderPicker.getLocalName());
						currentState = State.serveOrderPicker;
						addBehaviour(new ArriveAtOrderPicker());

					} else if (currentState == State.travelBackHome) {

						// ARRIVAL HOME
						currentPickupWaitBehaviour.stop();
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

					if (currentState == State.idle
							&& hasItem(message.getContent())) {

						// ANSWER WHEN THE SHELF HAS THE ITEM
						response.setPerformative(ACLMessage.CONFIRM);
						send(response);

						log("CONFIRM got items for "
								+ message.getSender().getLocalName());

					}
					break;

				case ACLMessage.PROPOSE:

					if (currentState != State.idle) {

						// SHELF IS BUSY
						log("REJECT_PORPOSAL with "
								+ message.getSender().getLocalName());
						response.setPerformative(ACLMessage.REJECT_PROPOSAL);
						send(response);

					} else {

						log("ACCEPT_PROPOSAL with "
								+ message.getSender().getLocalName());

						// NOW SERVE THAT PICKER
						response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						send(response);

						// BROADCAST ROBOTS
						currentState = State.waitForRobot;
						currentItemRequest = message.getContent();
						currentOrderPicker = message.getSender();
						broadcastRobots = new BroadcastRobots(myAgent,
								BCAST_FREQ);
						addBehaviour(broadcastRobots);
					}
					break;

				case ACLMessage.INFORM:

					if (currentState == State.serveOrderPicker) {

						// check again to be sure its the right item
						if (!hasItem(message.getContent())) {
							log("something went wrong: delivered wrong item!");
						} else {
							// decrease inventory
							decreaseInventory(message.getContent());
							log("inventory now: " + getInventoryString());
						}

						// TRAVEL BACK HOME
						currentState = State.travelBackHome;
						currentOrderPicker = null;
						currentItemRequest = null;
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
