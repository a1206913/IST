package ist.ass6;

import ist.ass6.model.Booking;

import javax.jms.*;
import javax.jms.Message;

import org.apache.activemq.*;

/*
 * to consume messages asynchronously, we implement MessageListener (acts as an asynchronous event handler for messages) and ExceptionListener
 */
public class TravelAgent implements MessageListener, ExceptionListener {
	private static String user = ActiveMQConnection.DEFAULT_USER;
	private static String passwort = ActiveMQConnection.DEFAULT_PASSWORD;
	private static String url = ActiveMQConnection.DEFAULT_BROKER_URL;
	private static String subjectBooking = "bookingF";
	private static String subjectConsolidator1 = "consolidator_1";
	private static String subjectConsolidator2 = "consolidator_2";

	private Session session;
	// private Destination bookingQueue;
	private Destination tempConsolidatorQueue;

	// to reply to messages send by the producer (customer), we need to add a
	// message producer
	private MessageProducer replyToProducer;

	private MessageProducer mRequestToConsolidator1;
	private MessageProducer mRequestToConsolidator2;

	public TravelAgent() {
		try {
			// set up a ConnectionFactory for creating a connection to the
			// EmbeddedBroker
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
					user, passwort, url);
			System.out.println("1. set up the message queue");

			// create the connection with the message queue
			Connection connection = connectionFactory.createConnection();
			System.out.println("2. create the connection");
			/*
			 * set an ExceptionListener for this connection it allows a client to
			 * be notified of a problem asynchronously it does this by calling the
			 * listener's onException method, passing it a JMSException object
			 * describing the problem
			 */
			connection.setExceptionListener(this);

			connection.start();
			System.out.println("3. start the connection");

			// create the session and the first queue - to consume messages from
			// the BookingQueue
			this.session = connection.createSession(false,
					Session.AUTO_ACKNOWLEDGE);
			Destination bookingQueue = this.session.createQueue(subjectBooking);
			System.out.println("4.1. create the session");

			/*
			 * create the second Queue - to produce messages for the
			 * consolidatorQueue1
			 */
			Destination consolidatorQueue1 = this.session
					.createQueue(subjectConsolidator1);
			System.out.println("4.1. create the Queue consolidatorQueue1");

			/*
			 * create the third Queue - to produce messages for the
			 * consolidatorQueue2
			 */
			Destination consolidatorQueue2 = this.session
					.createQueue(subjectConsolidator2);
			System.out.println("4.2. create the Queue consolidatorQueue2");

			/*
			 * setup a consumer to consume messages off from the booking queue
			 * listen to incoming messages
			 */
			MessageConsumer messageConsumer = this.session
					.createConsumer(bookingQueue);
			System.out.println("5. create a consumer");
			messageConsumer.setMessageListener(this);
			System.out.println("6. listen to incoming messages");

			
			/*
			 * in order to realize Request/Reply, we must make the producer (travel
			 * agent) also a message listener to listen to incoming responses from
			 * the airfair consolidators
			 */
			MessageConsumer replyFromConsolidators = session
					.createConsumer(tempConsolidatorQueue);
			System.out.println("5. create a consumer");
			replyFromConsolidators.setMessageListener(this);
			System.out.println("6. listen to incoming messages");

			/*
			 * Setup two message producers to create requests to the Airfare
			 * Consolidators
			 */
			this.mRequestToConsolidator1 = this.session
					.createProducer(consolidatorQueue1);
			this.mRequestToConsolidator1.setDeliveryMode(DeliveryMode.PERSISTENT);

			this.mRequestToConsolidator2 = this.session
					.createProducer(consolidatorQueue2);
			this.mRequestToConsolidator2.setDeliveryMode(DeliveryMode.PERSISTENT);

			System.out
					.println("7. set up the two request messages from the Airfare Consolidators");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.out.println("TravelAgent: in the main()-method");
		new TravelAgent();
	}

	/*
	 * Passes a message to the listener (in this case, the travel agent) A
	 * MessageListener object is used to receive asynchronously delivered
	 * messages In the onMessage method, you define the actions to be taken when
	 * a message arrives
	 */
	@Override
	public void onMessage(Message receivedMessage) {
		System.out.println("in the onMessage()-method");
		try {
			// received messages from the customers
			if (receivedMessage instanceof ObjectMessage) {
				ObjectMessage objReplyMsg = (ObjectMessage) receivedMessage;
				Booking b = (Booking) objReplyMsg.getObject();
				System.out.println("Received order for " + b.getCustomer());
				/*
				 * // ***************** everything between the stars goes somewhere
				 * else - this part is the reply for the customer
				 * ************************* // create a message producer for the
				 * replies to the customer MessageProducer producer =
				 * session.createProducer(receivedMessage.getJMSReplyTo()); //
				 * ObjectMessage replyMsg = session.createObjectMessage()
				 * objReplyMsg
				 * .setJMSCorrelationID(receivedMessage.getJMSMessageID());
				 * producer.send(objReplyMsg); //
				 * ***********************************
				 * *******************************
				 * ***********************************
				 * *********************************
				 */
				// create the message for the consolidators
				objReplyMsg = session.createObjectMessage(b);
				objReplyMsg.setJMSReplyTo(tempConsolidatorQueue);

				mRequestToConsolidator1.send(objReplyMsg);
				/*
				 * handling the messages accordingly by setting the correlationID
				 * from the received message to be the correlationID of the response
				 * message
				 * 
				 * this lets the customer identify to which message the received
				 * response belongs to
				 */
				// txtMessageToC1.setJMSCorrelationID(receivedMessage.getJMSCorrelationID());

				/*
				 * Send the response to the Destination specified by the JMSReplyTo
				 * field of the received message, this is presumably a temporary
				 * queue created by the client
				 */
				// this.mRequestToConsolidator1.send(receivedMessage.getJMSReplyTo(),
				// txtMessageToC1);
			}

			// received message from producer
			// System.out.println("Received message: " + txtMessage.getText());

			System.out.println("the end of the onMessage()-method...");
		} catch (JMSException ex) {
			System.out.println("Error from the onMessage() method: " + ex);
			ex.printStackTrace();
		}
	}

	@Override
	public void onException(JMSException ex) {
		System.out.println("Error from the onException() method " + ex);
	}
}
