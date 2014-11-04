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

import java.util.Iterator;

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
	private JSONObject orderList;
	private JSONObject order;

	private Behaviour idle;
	private Behaviour orderReceiver;
	private Behaviour itemBroadcaster;
	private Behaviour itemConfirm;
	private Behaviour itemPick;
	private Behaviour orderSender;

	@Override
	protected void setup()
	{
		DFAgentDescription agentDescription = new DFAgentDescription();
		agentDescription.setName(getAID());
		ServiceDescription serviceDescription = new ServiceDescription();
		// TODO setType oder setName?
		serviceDescription.setType(SERVICE_NAME);
		agentDescription.addServices(serviceDescription);
		try
		{
			DFService.register(this, agentDescription);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}

		System.out.println("OrderPicker " + getAID().getName() + "available.");
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
		System.out.println("OrderPicker " + getAID().getName() + " terminating.");
	}

	private class IdleBehaviour extends CyclicBehaviour
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			ACLMessage request = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
			if (request != null && OrderPicker.this.isIdle)
			{
				ACLMessage response = new ACLMessage(ACLMessage.AGREE);
				response.addReceiver(request.getSender());
				response.setLanguage("JSON");
				response.setContent(new JSONObject().put(getAID().getName(), true).toString());
				send(response);
			}
			else
			{
				// isIdle == false, do nothing
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
			if (request != null && OrderPicker.this.isIdle)
			{
				if (request.getLanguage().equals("JSON"))
				{
					OrderPicker.this.orderList = new JSONObject(request.getContent());
					OrderPicker.this.isIdle = false;

					OrderPicker.this.itemBroadcaster = new ItemBroadcaster();
					addBehaviour(OrderPicker.this.itemBroadcaster);
					// TODO unclear
					// OrderPicker.this.itemConfirm = new ItemConfirm();
					// addBehaviour(OrderPicker.this.itemConfirm);
					// OrderPicker.this.itemPick = new ItemPick();
					// addBehaviour(OrderPicker.this.itemPick);

				}
			}
			else
			{
				// isIdle == false, do nothing
			}

		}

	}

	private class ItemBroadcaster extends OneShotBehaviour
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			for (Iterator<String> it = OrderPicker.this.orderList.keys(); it.hasNext();)
			{
				JSONObject selectedItem = new JSONObject();

				selectedItem.put(it.next(), OrderPicker.this.orderList.get(it.next()));

				ACLMessage itemBroadcast = new ACLMessage(ACLMessage.QUERY_IF);
				itemBroadcast.setLanguage("JSON");

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

				itemBroadcast.setContent(selectedItem.toString());
				send(itemBroadcast);
			}
		}

	}

	private class ItemConfirm extends CyclicBehaviour
	{

		@Override
		public void action()
		{
			// TODO Auto-generated method stub

		}

	}

	private class ItemPick extends CyclicBehaviour
	{

		@Override
		public void action()
		{
			// TODO Auto-generated method stub

		}

	}

	private class OrderSender extends OneShotBehaviour
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			ACLMessage orderParts = new ACLMessage(ACLMessage.INFORM);
			orderParts.addReceiver(new AID("OrderAgent", AID.ISLOCALNAME));
			orderParts.setLanguage("JSON");
			orderParts.setContent(OrderPicker.this.order.toString());
			send(orderParts);
		}

	}
}
