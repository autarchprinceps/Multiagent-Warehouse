package warehouse.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Random;

/**
 * @author Patrick Robinson
 *
 */
public class CustomerAgentStub extends Agent {
	private static final long serialVersionUID = 1018982190165117345L;
	private Random r = new Random();
	private String[] components = new String[] { "ROTOR", "CIRCUIT", "SCREW",
			"BATTERY", "STABILISER", "TUNER", "CHARGER", "ENGINE", "CASE",
			"CAMERA", "SPOTLIGHT" };

	protected void setup() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		try {
			DFService.register(this, dfd);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}

		this.addBehaviour(new TickerBehaviour(this, 10000) {
			private static final long serialVersionUID = -5570642512218060415L;

			@Override
			protected void onTick() {
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				DFAgentDescription whDesc = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setName("WarehouseAgent");
				sd.setType("WA");
				whDesc.addServices(sd);
				try {
					DFAgentDescription[] desc = DFService.search(
							CustomerAgentStub.this, whDesc);
					if (desc.length == 1) {
						msg.addReceiver(desc[0].getName());
					} else {
						if (desc.length > 1) {
							System.err
									.println("Multiple WarehouseAgents found");
						} else {
							System.err.println("No WarehouseAgent found");
						}
					}
				} catch (FIPAException ex) {
					ex.printStackTrace();
				}
				msg.setLanguage("JSON");
				msg.setContent(generateJSON());
				msg.addReplyTo(getAID());
				send(msg);
			}
		});
		this.addBehaviour(new CyclicBehaviour() {
			private static final long serialVersionUID = -1738591542396043852L;

			@Override
			public void action() {
				ACLMessage recMsg = receive(MessageTemplate.or(
						MessageTemplate.MatchPerformative(ACLMessage.FAILURE),
						MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
				if (recMsg != null) {
					System.out.println("Order finished: " + recMsg.getContent());
					System.out.println();
				} else {
					block();
				}
			}
		});
	}

	private String generateJSON() {
		StringBuilder result = new StringBuilder("[");
		for (String part : components) {
			result.append('{');
			result.append(part);
			result.append(':');
			result.append(r.nextInt(10) + 1);
			result.append("},");
		}
		result.deleteCharAt(result.length() - 1);
		result.append("]");
		return result.toString();
	}

	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}
	}
}
