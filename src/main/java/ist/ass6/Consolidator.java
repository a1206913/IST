package ist.ass6;

import javax.jms.*;
import javax.xml.bind.ParseConversionEvent;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

public class Consolidator implements MessageListener {
	private String user = ActiveMQConnection.DEFAULT_USER;
	private String passwort = ActiveMQConnection.DEFAULT_PASSWORD;
	private String url = ActiveMQConnection.DEFAULT_BROKER_URL;

	private String subjectConsolidator1 = "consolidator_1";
	private String subjectConsolidator2 = "consolidator_2";

	private Session session;
	private Destination tempConsolidatorQueue;
	private MessageProducer replyProducer;

	public Consolidator() {
		try {
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, passwort, url);
			Connection connection = connectionFactory.createConnection();
			connection.start();

			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			// create the first queue - for Consolidator1 and listen to the message
			// requests
			Destination consolidatorQueue1 = session.createQueue(subjectConsolidator1);
			MessageConsumer messageConsumer1FromAgent = session.createConsumer(consolidatorQueue1);
			messageConsumer1FromAgent.setMessageListener(this);
			System.out.println("where do we go from here");

			// create the second queue - for Consolidator2 and listen to the
			// message requests
			Destination consolidatorQueue2 = session.createQueue(subjectConsolidator2);
			MessageConsumer messageConsumer2FromAgent = session.createConsumer(consolidatorQueue2);
			messageConsumer2FromAgent.setMessageListener(this);
			
			//Setup a message producer to respond to messages from clients, we will get the destination
         //to send to from the JMSReplyTo header field from a Message
			this.replyProducer = this.session.createProducer(null);
			this.replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			/*
			 * Create the temporary queue for the responses from the consolidators
			 */
			tempConsolidatorQueue = session.createTemporaryQueue();

		} catch (Exception ex) {
			System.out.println("Error in the constructor" + ex);
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Consolidator();
	}

	@Override
	public void onMessage(Message receivedMessage) {
		System.out.println("in the onMessage()-Method");
		try {
			TextMessage response = session.createTextMessage();

			if (receivedMessage instanceof TextMessage) {
				String messageFromAgent = ((TextMessage) receivedMessage).getText();
				System.out.println(messageFromAgent);

				// get the useful data out of the message
				try {
					int i = 0;
					for (; i < messageFromAgent.length(); i++) {
						if (messageFromAgent.substring(i, i + 1).equals(":")) {
							break;
						}
					}
					String data = messageFromAgent.substring(0, i);
					int orderNr = Integer.parseInt(data);
					// find out which consolidator confirmed the booking
					if (receivedMessage.getJMSReplyTo().equals("consolidatorQueue1"))
						System.out.println("[Consolidator 1] Confirmation for Booking Order " + orderNr);
					else
						System.out.println("[Consolidator 2] Confirmation for Booking Order " + orderNr);

					// create the response
					response.setText("" + orderNr);
					
					response.setJMSCorrelationID(receivedMessage.getJMSCorrelationID());
					//Send the response to the Destination specified by the JMSReplyTo field of the received message,
	            //this is presumably a temporary queue created by the client
	            this.replyProducer.send(receivedMessage.getJMSReplyTo(), response);
					
				} catch (IndexOutOfBoundsException ex) {
					System.out.println("A problem with the substring " + ex);
					ex.printStackTrace();
				} catch (IllegalArgumentException ex) {
					System.out.println("Symbol cannot be parsed into an int " + ex);
					ex.printStackTrace();
				}

				/*
				 * in order to realize Request/Reply, we must make the listeners
				 * (consolidators) also message producers, to reply to the messages
				 * from the airfair consolidators
				 */
				MessageProducer replyFromConsolidators = session.createProducer(receivedMessage.getJMSReplyTo());
				response.setJMSCorrelationID(receivedMessage.getJMSMessageID());
				replyFromConsolidators.send(response);
			}
		} catch (JMSException ex) {
			System.out.println("Error from the onMessage() - method " + ex);
			ex.printStackTrace();
		}
	}
}
