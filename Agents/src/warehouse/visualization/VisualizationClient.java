package warehouse.visualization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class VisualizationClient {
	private final String host;
	private final int port;
	private final Socket socket;
	private final PrintWriter out;
	private final BufferedReader in;
	
	public VisualizationClient(String host, int port) throws UnknownHostException, IOException {
		this.host = host;
		this.port = port;
		socket = new Socket(host, port);
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		out.println("Kiva");
	}
	
	public VisualizationClient() throws UnknownHostException, IOException {
		this("localhost", 4711);
	}
	
	private void sendRawMessage(String message) {
		out.println(message);
	}
	
	private List<String> getRawServerOutput() throws IOException {
		ArrayList<String> result = new ArrayList<>();
		while(in.ready()) {
			result.add(in.readLine());
		}
		return result;
	}
	
	public void orderStarted(String order) {
		// customer: message -> warehouse: create
	}
	
	public void orderAssigned(String order, String picker) {
		// order -> picker
	}
	
	public void shelfAssigned(String shelf, String picker, String robot) {
		// robot -> shelf
		// robot+shelf -> picker
		// robot -> free
	}
	
	public void shelfFinished(String shelf, String robot) {
		// robot -> shelf
		// robot+shelf -> orig shelf pos
		// robot -> free
	}
	
	public void orderFinished(String order, boolean correct) {
		// order -> warehouse
		// order check
		// correct ? order -> customer : order restart
	}
	
	public void orderFailed(String order) {
		// order (marked) -> warehouse -> customer
	}
}
