package ist.ass6;

import javax.jms.*;
import javax.jms.Message;

import org.apache.activemq.*;

/*
 * to consume messages asynchronously, we implement MessageListener and ExceptionListener
 */
public class TravelAgent implements MessageListener, ExceptionListener {
		private static String user = ActiveMQConnection.DEFAULT_USER;
		private static String passwort = ActiveMQConnection.DEFAULT_PASSWORD;
		private static String url = ActiveMQConnection.DEFAULT_BROKER_URL;
		private static String subjectAirTicket = "agent";
		
		private Session session;
		private Destination destinationQueue;
		
//		to reply to messages send by the producer (customer), we need to add a message producer
		private MessageProducer replyToProducer;

		public TravelAgent() {
			try {
//				set up the message queue for the listener (TravelAgent)
				ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, passwort, url);
				
//				create the connection with the message queue
				Connection connection = connectionFactory.createConnection();
				/*
				 * set an ExceptionListener for this connection
				 * it allows a client to be notified of a problem asynchronously
				 * it does this by calling the listener's onException method, passing it a JMSException object 
				 * describing the problem
				 */
				connection.setExceptionListener(this);
				
				connection.start();
				
//				create the session
				this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				destinationQueue = this.session.createQueue(subjectAirTicket);
//				hier comes the second session
				
				
				/*
				 * listen to incoming messages
				 * setup a consumer to consume messages off from the admin queue 
				 */
				MessageConsumer messageConsumer = this.session.createConsumer(destinationQueue);
				messageConsumer.setMessageListener(this);
				
				/* 
				 * Setup a message producer to respond to messages from clients, we will get the destination
				 * to send to from the JMSReplyTo header field from a Message
				 */
				this.replyToProducer = this.session.createProducer(null);
				this.replyToProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		public static void main(String[] args) {
			System.out.println("in the main()-method");
			new TravelAgent();
		}

	/*
	 * Passes a message to the listener (in this case, the travel agent)
	 * A MessageListener object is used to receive asynchronously delivered messages 
	 */
	@Override
	public void onMessage(Message receivedMessage) {
		System.out.println("in the onMessage()-method");
		try {
//			ONLY Temporarely *** will be changed in OBJECTMESSAGE for the assignment
			TextMessage response = this.session.createTextMessage();
			if (receivedMessage instanceof TextMessage) {
				TextMessage txtMessage = (TextMessage) receivedMessage;
				String messageText = txtMessage.getText();
				response.setText("REPLY TO '" + messageText);
				
				//	received message from producer
//				System.out.println("Received message: " + txtMessage.getText());
				
				/*
				 * handling the messages accordingly by setting the correlationID from the received
				 * message to be the correlationID of the response message
				 * 
				 * this lets the customer identify to which message the received response belongs to 
				 * 
				 */
				response.setJMSCorrelationID(receivedMessage.getJMSCorrelationID());

				/*
				 * Send the response to the Destination specified by the JMSReplyTo field
				 * of the received message, this is presumably a temporary queue created by the client
				 */
				this.replyToProducer.send(receivedMessage.getJMSReplyTo(), response);
				
//				System.out.println("[" + this + "] Send Response: '" + response.getText() + "'");
			}
			
			
		}
		catch (JMSException ex) {
			System.out.println("From the onMessage() method: " + ex);
			ex.printStackTrace();
		}
	}

	@Override
	public void onException(JMSException ex) {
		System.out.println("From the onException() method " + ex);
	}
}


