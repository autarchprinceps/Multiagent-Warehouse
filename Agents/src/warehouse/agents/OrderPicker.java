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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

	PrintWriter writer = null;
	File logFile = null;

	private static enum ItemStatus
	{
		BROADCASTED, SHELF_PROPOSED, SHELF_ACCEPTED, PROPERTY
	};

	private boolean isIdle;
	private AID currentOrderAgent;
	private JSONArray orderIncoming = new JSONArray();
	private JSONArray orderOutgoing = new JSONArray();

	private final Map<Pair<String, Integer>, ItemStatus> itemStatus = new HashMap<Pair<String, Integer>, ItemStatus>();
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

		log("available.");
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
		log("terminating.");
	}

	private boolean checkOrderCompletion()
	{
		boolean tmp = true;
		for (Pair<String, Integer> key : this.itemStatus.keySet())
		{
			switch (this.itemStatus.get(key))
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
		JSONArray missingItems = new JSONArray();
		for (Pair<String, Integer> item : this.itemStatus.keySet())
		{
			switch (this.itemStatus.get(item))
			{
			case BROADCASTED:
				//TODO format problem ???
				missingItems.put(new JSONObject(item));
				OrderPicker.this.itemStatus.put(item, ItemStatus.BROADCASTED);
				break;
			case SHELF_PROPOSED:
				break;
			case SHELF_ACCEPTED:
				break;
			case PROPERTY:
				break;
			default:
				throw new RuntimeException("Invalid value in orderStatus HashMap!");
			}
		}
		Behaviour requiredItems = new Broadcaster(missingItems);
		addBehaviour(requiredItems);
	}

	private void initLogFile(AID currentOrderAgent)
	{
		this.logFile = new File(getLocalName() + "_" + currentOrderAgent.getLocalName() + ".txt");
		if (this.logFile.exists())
		{
			this.logFile.delete();
		}
		try
		{
			this.writer = new PrintWriter(new BufferedWriter(new FileWriter(this.logFile, true)));
			this.writer.println("logFile created!");
			this.writer.println();
			this.writer.flush();
			System.out.println("log file created: " + this.logFile.getCanonicalPath());
		}
		catch (IOException e)
		{
			System.out.println("creating log file failed!");
		}
	}

	private void logFile(String line)
	{
		this.writer.println(line);
		this.writer.flush();
	}

	private void logFileItemStatus()
	{
		logFile("---itemStatus---");
		for (Pair<String, Integer> key : OrderPicker.this.itemStatus.keySet())
		{
			logFile(key + " : " + OrderPicker.this.itemStatus.get(key));
		}
		logFile("");
	}

	private void log(String log)
	{
		// System.out.println(getLocalName() + ": " + log);
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
				log("QUERY_IF received, send CONFIRM to " + query_if.getSender().getLocalName());
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
						log("REQUEST received, AGREE send to: " + request.getSender().getLocalName());
						OrderPicker.this.isIdle = false;
						OrderPicker.this.currentOrderAgent = request.getSender();
						ACLMessage agree = request.createReply();
						agree.setPerformative(ACLMessage.AGREE);
						agree.setLanguage("JSON");
						agree.setProtocol("JSON");
						agree.setContent(new JSONObject().put(getLocalName(), true).toString());
						send(agree);

						log("REQUEST items: " + request.getContent());
						OrderPicker.this.orderIncoming = new JSONArray(request.getContent());

						OrderPicker.this.shelfInteraction = new ShelfInteraction();
						addBehaviour(OrderPicker.this.shelfInteraction);

						initLogFile(OrderPicker.this.currentOrderAgent);
						for (int i = 0; i < OrderPicker.this.orderIncoming.length(); i++)
						{
							Pair<String, Integer> item = Pair.convert(OrderPicker.this.orderIncoming.getJSONObject(i));
							OrderPicker.this.itemStatus.put(item, ItemStatus.BROADCASTED);
						}
						logFileItemStatus();

						Behaviour incomingOrderBC = new Broadcaster(OrderPicker.this.orderIncoming);
						addBehaviour(incomingOrderBC);

						OrderPicker.this.abortOrder = new AbortOrder(this.myAgent);
						addBehaviour(OrderPicker.this.abortOrder);
					}
				}
				else
				{ // isIdle == false
					log("REQUEST received, CANCEL send to: " + request.getSender().getLocalName());
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

	private class Broadcaster extends OneShotBehaviour
	{
		private static final long serialVersionUID = 1L;

		private final JSONArray requiredItems;

		public Broadcaster(JSONArray orderList)
		{
			this.requiredItems = orderList;
		}

		@Override
		public void action()
		{
			ACLMessage broadcast = new ACLMessage(ACLMessage.QUERY_IF);
			broadcast.setProtocol("request-item");

			DFAgentDescription orderPickerDesc = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("shelf");
			orderPickerDesc.addServices(sd);
			try
			{
				for (DFAgentDescription desc : DFService.search(OrderPicker.this, orderPickerDesc))
				{
					broadcast.addReceiver(desc.getName());
				}
			}
			catch (FIPAException e)
			{
				e.printStackTrace();
			}

			logFile("---new broadcast requiredItems---");
			logFile("broadcast: " + this.requiredItems);
			logFile("");
			broadcast.setContent(this.requiredItems.toString());
			send(broadcast);
			removeBehaviour(this);
		}
	}

	private class ShelfInteraction extends CyclicBehaviour
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void action()
		{
			ACLMessage shelfMsg = receive(MessageTemplate.MatchProtocol("request-item"));

			if (shelfMsg != null)
			{
				JSONArray shelfMsgContent = new JSONArray(shelfMsg.getContent());
				JSONArray opMsgContent = new JSONArray();

				logFile("---shelfInteraction---");
				logFile("from: " + shelfMsg.getSender().getLocalName());
				logFile("aclmessage: " + ACLMessage.getPerformative(shelfMsg.getPerformative()));
				logFile("");

				switch (shelfMsg.getPerformative())
				{
				case ACLMessage.CONFIRM:
					for (int i = 0; i < shelfMsgContent.length(); i++)
					{
						JSONObject itemJSON = shelfMsgContent.getJSONObject(i);
						Pair<String, Integer> item = Pair.convert(itemJSON);
						if (OrderPicker.this.itemStatus.get(item) == ItemStatus.BROADCASTED
								&& !OrderPicker.this.shelfInfo.containsKey(item))
						{
							OrderPicker.this.itemStatus.put(item, ItemStatus.SHELF_PROPOSED);
							OrderPicker.this.shelfInfo.put(item, shelfMsg.getSender().getLocalName());
							opMsgContent.put(itemJSON);
						}
					}
					log("PROPOSE send to : " + shelfMsg.getSender().getLocalName());
					ACLMessage propose = shelfMsg.createReply();
					propose.setPerformative(ACLMessage.PROPOSE);
					propose.setContent(opMsgContent.toString());
					send(propose);
					break;
				case ACLMessage.ACCEPT_PROPOSAL:
					for (int i = 0; i < shelfMsgContent.length(); i++)
					{
						Pair<String, Integer> item = Pair.convert(shelfMsgContent.getJSONObject(i));
						if (OrderPicker.this.itemStatus.get(item) == ItemStatus.SHELF_PROPOSED
								&& OrderPicker.this.shelfInfo.get(item).equals(shelfMsg.getSender().getLocalName()))
						{
							OrderPicker.this.itemStatus.put(item, ItemStatus.SHELF_ACCEPTED);
						}
					}
					break;
				case ACLMessage.REJECT_PROPOSAL:
					for (int i = 0; i < shelfMsgContent.length(); i++)
					{
						JSONObject itemJSON = shelfMsgContent.getJSONObject(i);
						Pair<String, Integer> item = Pair.convert(itemJSON);
						if (OrderPicker.this.itemStatus.get(item) == ItemStatus.SHELF_PROPOSED
								&& OrderPicker.this.shelfInfo.get(item).equals(shelfMsg.getSender().getLocalName()))
						{
							OrderPicker.this.itemStatus.put(item, ItemStatus.BROADCASTED);
							OrderPicker.this.shelfInfo.remove(item);
							opMsgContent.put(itemJSON);
						}
					}
					Behaviour rebroadcast = new Broadcaster(opMsgContent);
					addBehaviour(rebroadcast);
					break;
				case ACLMessage.INFORM:
					for (int i = 0; i < shelfMsgContent.length(); i++)
					{
						JSONObject itemJSON = shelfMsgContent.getJSONObject(i);
						Pair<String, Integer> item = Pair.convert(itemJSON);
						if (OrderPicker.this.itemStatus.get(item) == ItemStatus.SHELF_ACCEPTED
								&& OrderPicker.this.shelfInfo.get(item).equals(shelfMsg.getSender().getLocalName()))
						{
							opMsgContent.put(itemJSON);
							OrderPicker.this.orderOutgoing.put(item);
							OrderPicker.this.itemStatus.put(item, ItemStatus.PROPERTY);
						}
					}
					log("INFORM send to " + shelfMsg.getSender().getLocalName() + " (take item)");
					ACLMessage inform = shelfMsg.createReply();
					inform.setPerformative(ACLMessage.INFORM);
					inform.setContent(opMsgContent.toString());
					send(inform);
					if (checkOrderCompletion())
					{
						OrderPicker.this.finishOrder = new FinishOrder();
						addBehaviour(OrderPicker.this.finishOrder);
					}
					break;
				default:
					// ignore, do nothing
					break;
				}
				logFileItemStatus();
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

			log("Order complete, send INFORM to " + OrderPicker.this.currentOrderAgent.getLocalName() + "!");

			ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
			inform.addReceiver(OrderPicker.this.currentOrderAgent);
			inform.setLanguage("JSON");
			inform.setProtocol("JSON");
			inform.setContent(OrderPicker.this.orderOutgoing.toString());
			send(inform);

			OrderPicker.this.itemStatus.clear();
			OrderPicker.this.shelfInfo.clear();
			OrderPicker.this.orderOutgoing = null;
			OrderPicker.this.orderOutgoing = new JSONArray();

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
			super(a, 2000);
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
				if (this.i == OrderPicker.this.orderIncoming.length() * 5)
				{
					removeBehaviour(OrderPicker.this.shelfInteraction);

					log("Could not complete in time window, send FAILURE and partial complete order to "
							+ OrderPicker.this.currentOrderAgent.getLocalName() + "!");

					ACLMessage inform = new ACLMessage(ACLMessage.FAILURE);
					inform.addReceiver(OrderPicker.this.currentOrderAgent);
					inform.setLanguage("JSON");
					inform.setProtocol("JSON");
					inform.setContent(OrderPicker.this.orderOutgoing.toString());
					send(inform);

					OrderPicker.this.itemStatus.clear();
					OrderPicker.this.shelfInfo.clear();
					OrderPicker.this.orderOutgoing = null;
					OrderPicker.this.orderOutgoing = new JSONArray();

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
