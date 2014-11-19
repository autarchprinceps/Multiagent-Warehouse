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
public class ShelfAgent extends Agent
{

	public static final String SERVICE_NAME = "shelf";
	private static final long serialVersionUID = 1L;
	private static final int BCAST_TICKS = 3000;

	// FIELDS FOR CURRENT SHELF STATE
	private State currentState;
	private AID currentRobot;
	private AID currentOrderPicker;
	private String currentItemRequest;
	private TickerBehaviour broadcastRobots;

	// ITEMS THE SHELF CONTAINS
	private String item;
	private int quantity;

	private enum State
	{
		idle, waitForRobot, travelToOrderPicker, serveOrderPicker, travelBackHome
	}

	public ShelfAgent()
	{
		this.currentState = State.idle;
		this.addBehaviour(new ItemRequestProtocol());
		this.addBehaviour(new RobotRequestProtocol());
	}

	@Override
	protected void setup()
	{

		// INIT FILEDS WITH ARGS
		Object[] args = getArguments();
		this.item = args[0].toString();
		this.quantity = Integer.parseInt(args[1].toString());

		log("setup with item " + this.item + ": " + this.quantity);

		// REGISTER SERVICE
		DFAgentDescription agentDesc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType(SERVICE_NAME);
		serviceDesc.setName(SERVICE_NAME);
		agentDesc.setName(getAID());
		agentDesc.addServices(serviceDesc);

		try
		{
			DFService.register(this, agentDesc);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}

	}

	@Override
	protected void takeDown()
	{
		try
		{
			DFService.deregister(this);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}
	}

	protected boolean hasItem(String jsonRequest)
	{
		/*
		 * EXAMPLE IN JSON: { ROTOR : 1 }
		 */
		Pair<String, Integer> requestedObject = Pair.convert(new JSONObject(jsonRequest));
		String requestedItem = requestedObject.getFirst();
		int requestedCount = requestedObject.getSecond();
		return this.item.equals(requestedItem) && requestedCount <= this.quantity;
	}

	private class BroadcastRobots extends TickerBehaviour
	{

		public BroadcastRobots(Agent a, long period)
		{
			super(a, period);
		}

		private static final long serialVersionUID = 1L;

		@Override
		public void onTick()
		{

			ACLMessage itemBroadcast = new ACLMessage(ACLMessage.REQUEST);
			itemBroadcast.setProtocol("request-robot");

			DFAgentDescription robotDesc = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(RobotAgent.SERVICE_TYPE);
			robotDesc.addServices(sd);
			try
			{
				for (DFAgentDescription desc : DFService.search(this.myAgent, robotDesc))
				{
					itemBroadcast.addReceiver(desc.getName());
				}
			}
			catch (FIPAException e)
			{
				e.printStackTrace();
			}

			send(itemBroadcast);
		}

	}

	private class ArriveAtOrderPicker extends OneShotBehaviour
	{

		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{

			// INFORM THE ORDER PICKER
			ACLMessage informOrderPickerMessage = new ACLMessage(ACLMessage.INFORM);
			informOrderPickerMessage.setProtocol("request-item");
			informOrderPickerMessage.addReceiver(ShelfAgent.this.currentOrderPicker);
			informOrderPickerMessage.setContent(ShelfAgent.this.currentItemRequest);
			send(informOrderPickerMessage);
		}

	}

	private class TravelBackHome extends OneShotBehaviour
	{

		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			ACLMessage informRobotMessage = new ACLMessage(ACLMessage.INFORM);
			informRobotMessage.setProtocol("request-robot");
			informRobotMessage.addReceiver(ShelfAgent.this.currentRobot);
			send(informRobotMessage);
		}

	}

	private class RobotRequestProtocol extends CyclicBehaviour
	{

		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			ACLMessage message = this.myAgent.receive(MessageTemplate.MatchProtocol("request-robot"));

			if (message != null)
			{
				ACLMessage response = message.createReply();

				switch (message.getPerformative())
				{

				case ACLMessage.PROPOSE:

					if (ShelfAgent.this.currentState != State.waitForRobot)
					{

						// WE ALREADY HAVE A ROBOT
						response.setPerformative(ACLMessage.REJECT_PROPOSAL);
						send(response);

					}
					else
					{

						// GOT A ROBOT
						log("ACCEPT_PROPOSAL with " + message.getSender().getLocalName());
						response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						send(response);

						ShelfAgent.this.broadcastRobots.stop();
						removeBehaviour(ShelfAgent.this.broadcastRobots);

						// WAIT FOR ARRIVAL
						ShelfAgent.this.currentState = State.travelToOrderPicker;
						ShelfAgent.this.currentRobot = message.getSender();

					}
					break;

				case ACLMessage.INFORM:

					if (ShelfAgent.this.currentState == State.travelToOrderPicker)
					{

						// ARRIVAL AT THE ORDER PICKER
						log("INFORM arrived at " + ShelfAgent.this.currentOrderPicker.getLocalName());
						ShelfAgent.this.currentState = State.serveOrderPicker;
						addBehaviour(new ArriveAtOrderPicker());

					}
					else if (ShelfAgent.this.currentState == State.travelBackHome)
					{

						// ARRIVAL HOME
						ShelfAgent.this.currentState = State.idle;
					}
					break;
				}

			}
			else
			{
				block();
			}

		}
	}

	private class ItemRequestProtocol extends CyclicBehaviour
	{

		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{

			ACLMessage message = this.myAgent.receive(MessageTemplate.MatchProtocol("request-item"));

			if (message != null)
			{

				ACLMessage response = message.createReply();
				response.setContent(message.getContent());

				switch (message.getPerformative())
				{

				case ACLMessage.QUERY_IF:

					if (hasItem(message.getContent()))
					{

						// ANSWER WHEN THE SHELF HAS THE ITEM
						response.setPerformative(ACLMessage.CONFIRM);
						send(response);

						log("CONFIRM got items for " + message.getSender().getLocalName());

					}
					break;

				case ACLMessage.PROPOSE:

					if (ShelfAgent.this.currentState != State.idle)
					{

						// SHELF IS BUSY
						log("REJECT_PORPOSAL with " + message.getSender().getLocalName());
						response.setPerformative(ACLMessage.REJECT_PROPOSAL);
						send(response);

					}
					else
					{

						log("ACCEPT_PROPOSAL with " + message.getSender().getLocalName());

						// NOW SERVE THAT PICKER
						response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						send(response);

						// BROADCAST ROBOTS
						ShelfAgent.this.currentState = State.waitForRobot;
						ShelfAgent.this.currentItemRequest = message.getContent();
						ShelfAgent.this.currentOrderPicker = message.getSender();
						ShelfAgent.this.broadcastRobots = new BroadcastRobots(this.myAgent, BCAST_TICKS);
						addBehaviour(ShelfAgent.this.broadcastRobots);
					}
					break;

				case ACLMessage.INFORM:

					if (ShelfAgent.this.currentState == State.serveOrderPicker)
					{

						// TRAVEL BACK HOME
						ShelfAgent.this.currentState = State.travelBackHome;
						ShelfAgent.this.currentRobot = null;
						ShelfAgent.this.currentOrderPicker = null;
						ShelfAgent.this.currentItemRequest = null;
						addBehaviour(new TravelBackHome());
					}
					break;
				}

			}
			else
			{
				block();
			}

		}
	}

	private void log(String log)
	{
		System.out.println(getLocalName() + ": " + log);
	}

}
