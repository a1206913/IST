package ist.ass6;

import ist.ass6.model.Booking;

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
		private Destination bookingQueue;
		
//		to reply to messages send by the producer (customer), we need to add a message producer
		private MessageProducer replyToProducer;

		public TravelAgent() {
			try {
//				set up the message queue for the listener (TravelAgent)
				ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, passwort, url);
				System.out.println("1. set up the message queue");
				
//				create the connection with the message queue
				Connection connection = connectionFactory.createConnection();
				System.out.println("2. create the connection");
				/*
				 * set an ExceptionListener for this connection
				 * it allows a client to be notified of a problem asynchronously
				 * it does this by calling the listener's onException method, passing it a JMSException object 
				 * describing the problem
				 */
				connection.setExceptionListener(this);
				
				connection.start();
				System.out.println("3. start the connection");
				
//				create the session
				this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				bookingQueue = this.session.createQueue(subjectAirTicket);
				System.out.println("4. create the session");
//				hier comes the second session
				
				
				/*
				 * setup a consumer to consume messages off from the booking queue 
				 * listen to incoming messages
				 */
				MessageConsumer messageConsumer = this.session.createConsumer(bookingQueue);
				System.out.println("5. create a consumer");
				messageConsumer.setMessageListener(this);
				System.out.println("6. listen to incoming messages");
				
				/* 
				 * Setup a message producer to respond to the messages from clients, we will get the destination
				 * to send to from the JMSReplyTo header field from a Message
				 */
				this.replyToProducer = this.session.createProducer(null);
				this.replyToProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
				System.out.println("7. set up a message producer to respond to the messages from clients");
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		public static void main(String[] args) {
			System.out.println("TravelAgent: in the main()-method");
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
//			ONLY Temporarily *** will be changed in OBJECTMESSAGE for the assignment
			/*TextMessage response = this.session.createTextMessage();
			if (receivedMessage instanceof TextMessage) {
				TextMessage txtMessage = (TextMessage) receivedMessage;
				String messageText = txtMessage.getText();
				response.setText("REPLY TO '" + messageText);*/
			
//			display received message
			
			if (receivedMessage instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) receivedMessage;
				Booking b = (Booking) objMessage.getObject();
				System.out.println("Received order for " + b.getCustomer());
				
				//	received message from producer
//				System.out.println("Received message: " + txtMessage.getText());
				
				/*
				 * handling the messages accordingly by setting the correlationID from the received
				 * message to be the correlationID of the response message
				 * 
				 * this lets the customer identify to which message the received response belongs to 
				 * 
				 */
//				response.setJMSCorrelationID(receivedMessage.getJMSCorrelationID());

				/*
				 * Send the response to the Destination specified by the JMSReplyTo field
				 * of the received message, this is presumably a temporary queue created by the client
				 */
//				this.replyToProducer.send(receivedMessage.getJMSReplyTo(), response);
				
//				System.out.println("[" + this + "] Send Response: '" + response.getText() + "'");
			}
			
			
		}
		catch (JMSException ex) {
			System.out.println("Error from the onMessage() method: " + ex);
			ex.printStackTrace();
		}
	}

	@Override
	public void onException(JMSException ex) {
		System.out.println("Error from the onException() method " + ex);
	}
}


