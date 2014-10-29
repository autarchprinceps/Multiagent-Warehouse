package warehouse.agents;

import jade.core.Agent;

import org.json.JSONObject;

/**
 * @author Daniel Pyka
 * 
 */
public class OrderPicker extends Agent
{
	private final boolean isBusy = false;
	private JSONObject orderList;
	private JSONObject partList;

	@Override
	protected void setup()
	{

	}

	@Override
	protected void takeDown()
	{

	}
}
