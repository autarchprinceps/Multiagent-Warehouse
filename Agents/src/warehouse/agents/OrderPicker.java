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

import java.util.Properties;

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

	private boolean isIdle;
	private JSONArray orderList;
	private JSONObject order;

	private final Properties orderBCStatus = new Properties();
	private final Properties orderCompleteStatus = new Properties();

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

	private class IdleBehaviour extends CyclicBehaviour
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			ACLMessage query_if = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
			if (query_if != null && OrderPicker.this.isIdle)
			{
				ACLMessage response = query_if.createReply();
				response.setPerformative(ACLMessage.CONFIRM);
				response.setLanguage("JSON");
				response.setContent(new JSONObject().put(getLocalName(), true).toString());
				send(response);
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
					System.out.println("OrderPicker REQUEST received, AGREE send: " + getLocalName());
					if (request.getLanguage().equals("JSON"))
					{
						OrderPicker.this.isIdle = false;
						ACLMessage agree = request.createReply();
						agree.setPerformative(ACLMessage.AGREE);
						agree.setLanguage("JSON");
						agree.setProtocol("JSON");
						agree.setContent(new JSONObject().put(getLocalName(), true).toString());
						send(agree);

						System.out.println("OrderPicker REQUEST items: " + request.getContent());
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
					System.out.println("OrderPicker REQUEST received, CANCEL send: " + getLocalName());
					ACLMessage cancel = request.createReply();
					cancel.setPerformative(ACLMessage.CANCEL);
					cancel.setLanguage("JSON");
					cancel.setProtocol("JSON");
					cancel.setContent(new JSONObject().put(getLocalName(), false).toString());
					send(cancel);
				}
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

			OrderPicker.this.orderBCStatus.put(this.item, true);
			itemBroadcast.setContent(this.item.toString());
			send(itemBroadcast);

		}

	}

	private class ShelfInteraction extends CyclicBehaviour
	{
		@Override
		public void action()
		{
			ACLMessage msg = receive(MessageTemplate.MatchProtocol("request-item"));

			if (msg != null)
			{
				switch (msg.getPerformative())
				{
				case ACLMessage.CONFIRM:
					ACLMessage propose = msg.createReply();
					propose.setPerformative(ACLMessage.PROPOSE);
					propose.setContent(new JSONObject().put(getLocalName(), true).toString());
					send(propose);
					break;
				case ACLMessage.DISCONFIRM:
					// Behaviour itemBroadcast = new ItemBroadcaster(new
					// JSONArray(msg.getContent()));
					// addBehaviour(itemBroadcast);
					break;
				case ACLMessage.ACCEPT_PROPOSAL:
					// "accept" -> do nothing?
					break;
				case ACLMessage.REJECT_PROPOSAL:
					// same as disconfirm
					break;
				case ACLMessage.INFORM:
					// take item
					break;
				default:
					// ignore, do nothing
					break;
				}
			}
		}

	}

	private class FinishOrder extends OneShotBehaviour
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
			inform.addReceiver(new AID("OrderAgent", AID.ISLOCALNAME));
			inform.setLanguage("JSON");
			inform.setProtocol("JSON");
			inform.setContent(OrderPicker.this.order.toString());
			send(inform);

			OrderPicker.this.orderBCStatus.clear();
			OrderPicker.this.orderCompleteStatus.clear();
			OrderPicker.this.isIdle = true;

			removeBehaviour(OrderPicker.this.shelfInteraction);
		}

	}
}
