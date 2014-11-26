package warehouse.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Daniel Pyka
 *
 */
public class OrderPicker extends Agent
{
	private static final long serialVersionUID = 1L;
	public static final String SERVICE_NAME = "pick";

	private static enum PartStatus
	{
		BROADCASTED, SHELF_PROPOSED, SHELF_ACCEPTED, PROPERTY
	};

	private boolean isIdle;
	private AID currentOrderAgent;
	private JSONArray orderList = new JSONArray();
	private JSONArray order = new JSONArray();

	private final Map<Pair<String, Integer>, PartStatus> orderStatus = new HashMap<Pair<String, Integer>, PartStatus>();
	private final Map<Pair<String, Integer>, String> shelfInfo = new HashMap<Pair<String, Integer>, String>();

	private Behaviour idle;
	private Behaviour orderReceiver;
	private Behaviour shelfInteraction;
	private Behaviour finishOrder;
	private Behaviour abortOrder;

	@Override
	protected void setup()
	{
		DFAgentDescription agentDescription = new DFAgentDescription();
		ServiceDescription serviceDescription = new ServiceDescription();
		serviceDescription.setName(SERVICE_NAME);
		serviceDescription.setType(SERVICE_NAME);
		agentDescription.setName(getAID());
		agentDescription.addServices(serviceDescription);
		try
		{
			DFService.register(this, agentDescription);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}

		System.out.println(getLocalName() + ": available.");
		this.isIdle = true;

		this.idle = new IdleBehaviour();
		this.orderReceiver = new OrderReceiver();
		addBehaviour(this.idle);
		addBehaviour(this.orderReceiver);
	}

	@Override
	protected void takeDown()
	{
		try
		{
			DFService.deregister(this);
		}
		catch (FIPAException fe)
		{
			fe.printStackTrace();
		}
		System.out.println(getLocalName() + ": terminating.");
	}

	private boolean checkOrderCompletion()
	{
		boolean tmp = true;
		for (Pair<String, Integer> key : this.orderStatus.keySet())
		{
			switch (this.orderStatus.get(key))
			{
			case BROADCASTED:
				tmp = tmp && false;
				break;
			case SHELF_PROPOSED:
				tmp = tmp && false;
				break;
			case SHELF_ACCEPTED:
				tmp = tmp && false;
				break;
			case PROPERTY:
				tmp = tmp && true;
				break;
			default:
				throw new RuntimeException("Invalid value in orderStatus HashMap!");
			}
		}
		return tmp;
	}

	private void rebroadcast()
	{
		for (Pair<String, Integer> key : this.orderStatus.keySet())
		{
			switch (this.orderStatus.get(key))
			{
			case BROADCASTED:
				Behaviour itemBroadcast = new ItemBroadcaster(key);
				addBehaviour(itemBroadcast);
				break;
			case SHELF_PROPOSED:
				// nothing
				break;
			case SHELF_ACCEPTED:
				// nothing
				break;
			case PROPERTY:
				// nothing
				break;
			default:
				throw new RuntimeException("Invalid value in orderStatus HashMap!");
			}
		}
	}

	private class IdleBehaviour extends CyclicBehaviour
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			ACLMessage query_if = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
			if (query_if != null && OrderPicker.this.isIdle)
			{
				System.out.println(getLocalName() + ": QUERY_IF received, send CONFIRM to "
						+ query_if.getSender().getLocalName());
				ACLMessage response = query_if.createReply();
				response.setPerformative(ACLMessage.CONFIRM);
				response.setLanguage("JSON");
				response.setContent(new JSONObject().put(getLocalName(), true).toString());
				send(response);
			}
			else
			{
				block();
			}
		}

	}

	private class OrderReceiver extends CyclicBehaviour
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			ACLMessage request = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			if (request != null)
			{
				if (OrderPicker.this.isIdle)
				{
					if (request.getLanguage().equals("JSON"))
					{
						System.out.println(getLocalName() + ": REQUEST received, AGREE send to: "
								+ request.getSender().getLocalName());
						OrderPicker.this.isIdle = false;
						OrderPicker.this.currentOrderAgent = request.getSender();
						ACLMessage agree = request.createReply();
						agree.setPerformative(ACLMessage.AGREE);
						agree.setLanguage("JSON");
						agree.setProtocol("JSON");
						agree.setContent(new JSONObject().put(getLocalName(), true).toString());
						send(agree);

						System.out.println(getLocalName() + ": REQUEST items: " + request.getContent());
						OrderPicker.this.orderList = new JSONArray(request.getContent());

						OrderPicker.this.shelfInteraction = new ShelfInteraction();
						addBehaviour(OrderPicker.this.shelfInteraction);

						for (int i = 0; i < OrderPicker.this.orderList.length(); i++)
						{
							Pair<String, Integer> item = Pair.convert(OrderPicker.this.orderList.getJSONObject(i));
							Behaviour itemBroadcast = new ItemBroadcaster(item);
							addBehaviour(itemBroadcast);
						}

						OrderPicker.this.abortOrder = new AbortOrder(this.myAgent);
						addBehaviour(OrderPicker.this.abortOrder);
					}
				}
				else
				{ // isIdle == false
					System.out.println(getLocalName() + ": REQUEST received, CANCEL send to: " + getLocalName());
					ACLMessage cancel = request.createReply();
					cancel.setPerformative(ACLMessage.CANCEL);
					cancel.setLanguage("JSON");
					cancel.setProtocol("JSON");
					cancel.setContent(new JSONObject().put(getLocalName(), false).toString());
					send(cancel);
				}
			}
			else
			{
				block();
			}
		}

	}

	private class ItemBroadcaster extends OneShotBehaviour
	{
		private static final long serialVersionUID = 1L;

		private final Pair<String, Integer> item;

		public ItemBroadcaster(Pair<String, Integer> item)
		{
			this.item = item;
		}

		@Override
		public void action()
		{
			ACLMessage itemBroadcast = new ACLMessage(ACLMessage.QUERY_IF);
			itemBroadcast.setProtocol("request-item");

			DFAgentDescription orderPickerDesc = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("shelf");
			orderPickerDesc.addServices(sd);
			try
			{
				for (DFAgentDescription desc : DFService.search(OrderPicker.this, orderPickerDesc))
				{
					itemBroadcast.addReceiver(desc.getName());
				}
			}
			catch (FIPAException e)
			{
				e.printStackTrace();
			}

			OrderPicker.this.orderStatus.put(this.item, PartStatus.BROADCASTED);
			System.out.println(getLocalName() + ": broadcast: " + this.item.toString());

			itemBroadcast.setContent(this.item.toString());
			send(itemBroadcast);
			removeBehaviour(this);
		}
	}

	private class ShelfInteraction extends CyclicBehaviour
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			ACLMessage shelfAnswer = receive(MessageTemplate.MatchProtocol("request-item"));

			if (shelfAnswer != null)
			{
				Pair<String, Integer> content = Pair.convert(new JSONObject(shelfAnswer.getContent()));

				System.out.println(getLocalName() + " got msg from " + shelfAnswer.getSender().getLocalName() + " "
						+ ACLMessage.getPerformative(shelfAnswer.getPerformative()) + " content: " + content);

				switch (shelfAnswer.getPerformative())
				{
				case ACLMessage.CONFIRM:
					// first shelf which answers to the broadcast will get the propose
					if (OrderPicker.this.orderStatus.get(content) == PartStatus.BROADCASTED)
					{
						System.out.println(getLocalName() + ": PROPOSE send to : " + shelfAnswer.getSender().getLocalName());
						ACLMessage propose = shelfAnswer.createReply();
						propose.setPerformative(ACLMessage.PROPOSE);
						propose.setContent(content.toString());
						OrderPicker.this.orderStatus.put(content, PartStatus.SHELF_PROPOSED);
						send(propose);
					}
					break;
				case ACLMessage.ACCEPT_PROPOSAL:
					// flag item to not being rebroadcasted again
					// store information which shelf brings the item to check later
					if (OrderPicker.this.orderStatus.get(content) == PartStatus.SHELF_PROPOSED)
					{
						OrderPicker.this.orderStatus.put(content, PartStatus.SHELF_ACCEPTED);
						OrderPicker.this.shelfInfo.put(content, shelfAnswer.getSender().getLocalName());
					}
					break;
				case ACLMessage.REJECT_PROPOSAL:
					// only effective if there are some more shelfs which contain the
					// specific item
					// and the system does not wait for robots too long
					Behaviour itemBroadcast = new ItemBroadcaster(content);
					addBehaviour(itemBroadcast);
					break;
				case ACLMessage.INFORM:
					// orderpicker checks if the item was proposed by some shelf
					// check if the shelf is the correct one (the same as proposed)
					if (OrderPicker.this.orderStatus.get(content) == PartStatus.SHELF_ACCEPTED
							&& OrderPicker.this.shelfInfo.get(content).equals(shelfAnswer.getSender().getLocalName()))
					{
						System.out.println(getLocalName() + ": INFORM send to: " + shelfAnswer.getSender().getLocalName()
								+ " (take item)");
						ACLMessage inform = shelfAnswer.createReply();
						inform.setPerformative(ACLMessage.INFORM);
						inform.setContent(content.toString());
						send(inform);
						OrderPicker.this.order.put(content);
						OrderPicker.this.orderStatus.put(content, PartStatus.PROPERTY);

						if (checkOrderCompletion())
						{
							OrderPicker.this.finishOrder = new FinishOrder();
							addBehaviour(OrderPicker.this.finishOrder);
						}
					}
					break;
				default:
					// ignore, do nothing
					break;
				}
			}
			else
			{
				block();
			}
		}
	}

	private class FinishOrder extends OneShotBehaviour
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			removeBehaviour(OrderPicker.this.abortOrder);
			removeBehaviour(OrderPicker.this.shelfInteraction);

			System.out.println(getLocalName() + ": Order complete, send INFORM to "
					+ OrderPicker.this.currentOrderAgent.getLocalName() + "!");

			ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
			inform.addReceiver(OrderPicker.this.currentOrderAgent);
			inform.setLanguage("JSON");
			inform.setProtocol("JSON");
			inform.setContent(OrderPicker.this.order.toString());
			send(inform);

			OrderPicker.this.orderStatus.clear();
			OrderPicker.this.shelfInfo.clear();
			OrderPicker.this.order = null;
			OrderPicker.this.order = new JSONArray();

			OrderPicker.this.isIdle = true;

			removeBehaviour(this);
		}
	}

	private class AbortOrder extends TickerBehaviour
	{
		private int i = 0;

		public AbortOrder(Agent a)
		{
			// check every 5s
			super(a, 5000);
		}

		private static final long serialVersionUID = 1L;

		@Override
		public void onTick()
		{
			if (checkOrderCompletion())
			{
				removeBehaviour(this);
			}
			else
			{
				if (this.i == OrderPicker.this.orderList.length())
				{
					removeBehaviour(OrderPicker.this.shelfInteraction);

					System.out.println(getLocalName()
							+ ": Could not complete in time window, send FAILURE and partial complete order to "
							+ OrderPicker.this.currentOrderAgent.getLocalName() + "!");

					ACLMessage inform = new ACLMessage(ACLMessage.FAILURE);
					inform.addReceiver(OrderPicker.this.currentOrderAgent);
					inform.setLanguage("JSON");
					inform.setProtocol("JSON");
					inform.setContent(OrderPicker.this.order.toString());
					send(inform);

					OrderPicker.this.orderStatus.clear();
					OrderPicker.this.shelfInfo.clear();
					OrderPicker.this.order = null;
					OrderPicker.this.order = new JSONArray();

					OrderPicker.this.isIdle = true;

					removeBehaviour(this);
				}
				else
				{
					rebroadcast();
					this.i++;
				}
			}

		}
	}
}
