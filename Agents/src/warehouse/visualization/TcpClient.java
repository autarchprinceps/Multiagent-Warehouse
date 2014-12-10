package warehouse.visualization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class TcpClient {
	private final String host;
	private final int port;
	private final Socket socket;
	private final PrintWriter out;
	private final BufferedReader in;
	
	public TcpClient(String host, int port) throws UnknownHostException, IOException {
		this.host = host;
		this.port = port;
		socket = new Socket(host, port);
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		out.println("Kiva");
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
}
