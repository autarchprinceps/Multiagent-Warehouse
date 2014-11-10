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

	public static final String SERVICE_NAME = "shelf-robot";
	private static final long serialVersionUID = 1L;
	private static Random rand = new Random();
	private boolean isBusy;

	public RobotAgent() {
		this.isBusy = false;
		this.addBehaviour(new WaitForShelfRequests());
	}

	@Override
	protected void setup() {
		DFAgentDescription agentDesc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setName(SERVICE_NAME);
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
			message.setSender(shelfID);
			message.setProtocol("request-robot");
			myAgent.send(message);

			delay = rand.nextInt(3);
			block(delay * 1000);

			message = new ACLMessage(ACLMessage.INFORM);
			message.setSender(shelfID);
			message.setProtocol("request-robot");
			myAgent.send(message);

			isBusy = false;
		}

	}

	private class WaitForShelfRequests extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {

			ACLMessage message = myAgent.receive(MessageTemplate
					.MatchProtocol("request-robot"));

			if (message != null) {

				ACLMessage response = message.createReply();

				switch (message.getPerformative()) {
				case ACLMessage.QUERY_IF:

					if (isBusy) {
						response.setPerformative(ACLMessage.DISCONFIRM);
					} else {
						response.setPerformative(ACLMessage.CONFIRM);
					}

				case ACLMessage.PROPOSE:

					if (isBusy) {
						response.setPerformative(ACLMessage.REJECT_PROPOSAL);
					} else {
						response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					}

					myAgent.addBehaviour(new TransportShelf(message.getSender()));

				default:
					response.setPerformative(ACLMessage.NOT_UNDERSTOOD);
				}

				myAgent.send(response);

			} else {
				block();
			}

		}

	}

}
