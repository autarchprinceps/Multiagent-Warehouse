package warehouse.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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
		BROADCASTED, SHELF_PROPOSED, PROPERTY
	};

	private boolean isIdle;
	private JSONArray orderList = new JSONArray();
	private JSONArray order = new JSONArray();

	private final Map<Pair<String, Integer>, PartStatus> orderStatus = new HashMap<Pair<String, Integer>, PartStatus>();

	private Behaviour idle;
	private Behaviour orderReceiver;
	private Behaviour shelfInteraction;
	private Behaviour finishOrder;

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
			case PROPERTY:
				tmp = tmp && true;
				break;
			default:
				throw new RuntimeException();
			}
		}
		return tmp;
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
						System.out.println(getLocalName() + " REQUEST received, AGREE send: " + request.getSender().getLocalName());
						OrderPicker.this.isIdle = false;
						ACLMessage agree = request.createReply();
						agree.setPerformative(ACLMessage.AGREE);
						agree.setLanguage("JSON");
						agree.setProtocol("JSON");
						agree.setContent(new JSONObject().put(getLocalName(), true).toString());
						send(agree);

						System.out.println(getLocalName() + " REQUEST items: " + request.getContent());
						OrderPicker.this.orderList = new JSONArray(request.getContent());

						OrderPicker.this.shelfInteraction = new ShelfInteraction();
						addBehaviour(OrderPicker.this.shelfInteraction);

						for (int i = 0; i < OrderPicker.this.orderList.length(); i++)
						{
							Pair<String, Integer> item = Pair.convert(OrderPicker.this.orderList.getJSONObject(i));
							Behaviour itemBroadcast = new ItemBroadcaster(item);
							addBehaviour(itemBroadcast);
						}
					}
				}
				else
				{ // isIdle == false
					System.out.println(getLocalName() + " REQUEST received, CANCEL send: " + getLocalName());
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
			System.out.println(getLocalName() + " broadcast: " + this.item.toString());

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
				switch (shelfAnswer.getPerformative())
				{
				case ACLMessage.CONFIRM:
					System.out.println(getLocalName() + " CONFIRM received from: " + shelfAnswer.getSender().getLocalName());
					if (OrderPicker.this.orderStatus.get(content) == PartStatus.BROADCASTED)
					{
						System.out.println(getLocalName() + " PROPOSE sending: " + OrderPicker.this.getLocalName());
						ACLMessage propose = shelfAnswer.createReply();
						propose.setPerformative(ACLMessage.PROPOSE);
						propose.setContent(content.toString());
						send(propose);
					}
					break;
				case ACLMessage.ACCEPT_PROPOSAL:
					System.out.println("OrderPicker ACCEPT_PROPOSAL received from: " + shelfAnswer.getSender().getLocalName());
					OrderPicker.this.orderStatus.put(content, PartStatus.SHELF_PROPOSED);
					break;
				case ACLMessage.REJECT_PROPOSAL:
					System.out.println(getLocalName() + "REJECT_PROPOSAL received from: "
							+ shelfAnswer.getSender().getLocalName());
					Behaviour itemBroadcast = new ItemBroadcaster(content);
					addBehaviour(itemBroadcast);
					break;
				case ACLMessage.INFORM:
					System.out.println(getLocalName() + "INFORM received from: " + shelfAnswer.getSender().getLocalName());
					{
						if (OrderPicker.this.orderStatus.get(content) == PartStatus.SHELF_PROPOSED)
						{
							System.out.println(getLocalName() + " INFORM send to: " + shelfAnswer.getSender().getLocalName()
									+ " (take item)");
							ACLMessage inform = shelfAnswer.createReply();
							inform.setPerformative(ACLMessage.INFORM);
							inform.setContent(content.toString());
							send(inform);
							OrderPicker.this.order.put(content);
							System.out.println("orderarray: " + OrderPicker.this.order.toString());
							OrderPicker.this.orderStatus.put(content, PartStatus.PROPERTY);
						}
					}
					if (checkOrderCompletion())
					{
						OrderPicker.this.finishOrder = new FinishOrder();
						addBehaviour(OrderPicker.this.finishOrder);
						removeBehaviour(OrderPicker.this.shelfInteraction);
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
			System.out.println(getLocalName() + ": Order complete, send INFORM to OrderAgent!");
			ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
			inform.addReceiver(new AID("OrderAgent", AID.ISLOCALNAME));
			inform.setLanguage("JSON");
			inform.setProtocol("JSON");
			inform.setContent(OrderPicker.this.order.toString());
			send(inform);

			OrderPicker.this.orderStatus.clear();
			OrderPicker.this.order = null;
			OrderPicker.this.order = new JSONArray();

			OrderPicker.this.isIdle = true;
			removeBehaviour(this);
		}
	}
}
