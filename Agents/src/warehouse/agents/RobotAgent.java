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

	private boolean isIdle;
	private boolean promisedProposal;
	private AID currentShelf;

	public RobotAgent() {
		this.isIdle = true;
		this.promisedProposal = false;
		this.addBehaviour(new RobotRequestProtocol());
	}

	@Override
	protected void setup() {

		log("setup");

		// REGISTER SERVICE
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

	@Override
	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	private class TransportShelfToOrderPicker extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {

			int transportDelay = rand.nextInt(5);
			block(transportDelay * 1000);

			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			message.addReceiver(currentShelf);
			message.setProtocol("request-robot");
			myAgent.send(message);
		}

	}

	private class TransportShelfHome extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {

			int transportDelay = rand.nextInt(5);
			block(transportDelay * 1000);

			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			message.addReceiver(currentShelf);
			message.setProtocol("request-robot");
			myAgent.send(message);
			
			currentShelf = null;
			isIdle = true;
			promisedProposal = false;
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

					if (isIdle && !promisedProposal) {

						response.setPerformative(ACLMessage.PROPOSE);
						send(response);
						promisedProposal = true;

					} else {
						response.setPerformative(ACLMessage.REFUSE);
						send(response);
					}

					break;

				case ACLMessage.ACCEPT_PROPOSAL:

					// TRANSPORT THAT SHELF
					isIdle = false;
					currentShelf = message.getSender();
					myAgent.addBehaviour(new TransportShelfToOrderPicker());
					break;

				case ACLMessage.REJECT_PROPOSAL:
					promisedProposal = false;
					break;

				case ACLMessage.INFORM:

					// SHELF INFORMS US THAT IT WANTS HOME
					if (!isIdle) {
						myAgent.addBehaviour(new TransportShelfHome());
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
