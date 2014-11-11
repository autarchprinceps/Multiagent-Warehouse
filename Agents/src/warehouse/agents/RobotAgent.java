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

import java.util.Random;

/**
 * @author Bastian Mager <bastian.mager.2010w@informatik.h-brs.de>
 */
public class RobotAgent extends Agent {

	public static final String SERVICE_TYPE = "shelf-robot";
	private static final long serialVersionUID = 1L;
	private static Random rand = new Random();
	private boolean isBusy;

	public RobotAgent() {
		this.isBusy = false;
		this.addBehaviour(new RobotRequestProtocol());
	}

	@Override
	protected void setup() {
		
		System.out.println("init " + getName());
		
		DFAgentDescription agentDesc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType(SERVICE_TYPE);
		serviceDesc.setName(SERVICE_TYPE);
		agentDesc.setName(getAID());
		agentDesc.addServices(serviceDesc);

		try {
			DFService.register(this, agentDesc);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

	}

	private class TransportShelf extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;

		private AID shelfID;

		protected TransportShelf(AID shelfID) {
			this.shelfID = shelfID;
		}

		@Override
		public void action() {

			isBusy = true;

			int delay = rand.nextInt(3);
			block(delay * 1000);

			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			message.addReceiver(shelfID);
			message.setProtocol("request-robot");
			myAgent.send(message);

			// TODO transport the shelf back?

			isBusy = false;
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
				case ACLMessage.REQUEST:

					if (isBusy) {
						response.setPerformative(ACLMessage.REFUSE);
					} else {
						response.setPerformative(ACLMessage.PROPOSE);
					}
					break;

				case ACLMessage.ACCEPT_PROPOSAL:
					isBusy = true;
					myAgent.addBehaviour(new TransportShelf(message.getSender()));
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

}
