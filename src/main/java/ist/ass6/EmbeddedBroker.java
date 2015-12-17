package ist.ass6;

import org.apache.activemq.broker.BrokerService;

public class EmbeddedBroker {
	public final static String MESSAGE_BROKER_URL = "tcp://localhost:61616";
	
	public static void main(String[] args) {
		try {
			BrokerService broker = new BrokerService();
			broker.setPersistent(false);
			broker.setUseJmx(false);
			broker.addConnector(MESSAGE_BROKER_URL);
			broker.start(true);
			broker.waitUntilStopped();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
