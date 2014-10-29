package warehouse.agents;

import jade.core.behaviours.CyclicBehaviour;

/**
 * @author Bastian Mager <bastian.mager.2010w@informatik.h-brs.de>
 */
public class ShelfAgent {

	private String item;
	
	public ShelfAgent(String item) {
		this.item = item;
	}

	
	
	private class WaitForRequests extends CyclicBehaviour {

		@Override
		public void action() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
}
