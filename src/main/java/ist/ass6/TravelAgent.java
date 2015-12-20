package ist.ass6;

import java.util.HashMap;
import java.util.Iterator;

import ist.ass6.model.Booking;

import javax.jms.*;
import javax.jms.Message;

import org.apache.activemq.*;

/*
 * to consume messages asynchronously, we implement MessageListener (acts as an asynchronous event handler for messages) and ExceptionListener
 */
public class TravelAgent implements MessageListener, ExceptionListener {
	private String user = ActiveMQConnection.DEFAULT_USER;
	private String passwort = ActiveMQConnection.DEFAULT_PASSWORD;
	private String url = ActiveMQConnection.DEFAULT_BROKER_URL;
	private String subjectBooking = "bookingF";
	private String subjectConsolidator1 = "consolidator_1";
	private String subjectConsolidator2 = "consolidator_2";

	private Booking b;
	private int nrOfTicketOrders = 1;
	private HashMap hm = new HashMap();
	
	private Session session;
	// private Destination bookingQueue;
	private Destination tempConsolidatorQueue;

	// to reply to messages send by the producer (customer), we need to add a
	// message producer
	private MessageProducer replyToCustomer;

	private MessageProducer mRequestToConsolidator1;
	private MessageProducer mRequestToConsolidator2;

	public TravelAgent() {
		try {
			// set up a ConnectionFactory for creating a connection to the
			// EmbeddedBroker
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, passwort, url);
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
			this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination bookingQueue = this.session.createQueue(subjectBooking);
			System.out.println("4.1. create the session");

			/*
			 * create the second Queue - to produce messages for the
			 * consolidatorQueue1
			 */
			Destination consolidatorQueue1 = this.session.createQueue(subjectConsolidator1);
			System.out.println("4.1. create the Queue consolidatorQueue1");

			/*
			 * create the third Queue - to produce messages for the
			 * consolidatorQueue2
			 */
			Destination consolidatorQueue2 = this.session.createQueue(subjectConsolidator2);
			System.out.println("4.2. create the Queue consolidatorQueue2");

			/*
			 * Create the temporary queue for the responses from the consolidators
			 */
			tempConsolidatorQueue = session.createTemporaryQueue();

			/*
			 * setup a consumer to consume messages off from the booking queue
			 * listen to incoming messages FROM CUSTOMER
			 */
			MessageConsumer messageConsumer_fromCustomer = this.session.createConsumer(bookingQueue);
			System.out.println("5. create the booking message consumer");
			messageConsumer_fromCustomer.setMessageListener(this);
			System.out.println("6. listen to incoming messages");

			/*
			 * Setup two message producers to create requests to the Airfare
			 * Consolidators
			 */
			this.mRequestToConsolidator1 = this.session.createProducer(consolidatorQueue1);
			this.mRequestToConsolidator1.setDeliveryMode(DeliveryMode.PERSISTENT);

			this.mRequestToConsolidator2 = this.session.createProducer(consolidatorQueue2);
			this.mRequestToConsolidator2.setDeliveryMode(DeliveryMode.PERSISTENT);

			System.out.println("7. set up the two request messages from the Airfare Consolidators");
			
			/*
			 * in order to realize Request/Reply, we must make the producer (travel
			 * agent) also a message listener to listen to incoming responses from
			 * the airfair consolidators
			 */
			MessageConsumer replyFromConsolidators = this.session.createConsumer(tempConsolidatorQueue);
			System.out.println("8. create the message consumer for Consolidators");
			replyFromConsolidators.setMessageListener(this);
			System.out.println("9. listen to incoming messages from Consolidators");
			
			//Setup a message producer to respond to messages from clients, we will get the destination
         //to send to from the JMSReplyTo header field from a Message
			this.replyToCustomer = this.session.createProducer(null);
			this.replyToCustomer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			


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
			// received messages from the CUSTOMERS
			if (receivedMessage instanceof ObjectMessage) {
				ObjectMessage objReplyMsg = (ObjectMessage) receivedMessage;
				b = (Booking) objReplyMsg.getObject();
				System.out.println("Received order for " + b.getCustomer());

				produceMessageForConsolidators(objReplyMsg);
				
			}
			
			else if (receivedMessage instanceof TextMessage) {
				System.out.println("IF instanceof");
				TextMessage responseFromConsolidator = session.createTextMessage();
				String messageFromConsolidator = ((TextMessage) receivedMessage).getText();
				int orderNr = Integer.parseInt(messageFromConsolidator);
				System.out.println("Confirmation for Booking Order " + orderNr + " received");
				
				if(hm.containsKey(orderNr)) {
					ObjectMessage oM = (ObjectMessage) hm.get(orderNr);
					produceMessageForCustomer(oM);
				}
			}

			// received message from producer
			// System.out.println("Received message: " + txtMessage.getText());

			System.out.println("the end of the onMessage()-method...");
		} catch (JMSException ex) {
			System.out.println("Error from the onMessage() method: " + ex);
			ex.printStackTrace();
		}
	}

	public void produceMessageForConsolidators(ObjectMessage objReplyMsg) {
//		produce the messages for the CONSOLIDATORS
		String consolidatorName = "Consolidator 2";
		
//		split the messages for the two consolidators
		try {
		// create the message for the consolidators
			TextMessage messageToConsolidator = session.createTextMessage(nrOfTicketOrders + ": " + b.consolidatorMessage());
			
			// set the JMSCorrelationID to the JMSCorrelationID of the message received by the customer
			messageToConsolidator.setJMSReplyTo(tempConsolidatorQueue);
			messageToConsolidator.setJMSCorrelationID(objReplyMsg.getJMSCorrelationID());
	
//			hashmap
			hm.put(nrOfTicketOrders, objReplyMsg);
			
			if (b.getDestination().contains("Austria")) {
				consolidatorName = "Consolidator 1";
				mRequestToConsolidator1.send(messageToConsolidator);
			}
			else
				mRequestToConsolidator2.send(messageToConsolidator);
			
			System.out.println("Booking Order " + nrOfTicketOrders + ": " + b.consolidatorMessage() + " (forwarded to " + consolidatorName + ")");
			nrOfTicketOrders++;
		}
		catch (JMSException ex) {
			ex.printStackTrace();
		}
	}
	
	public void produceMessageForCustomer(ObjectMessage receivedMessage) {
//		create a message producer for the replies to the customer 
		try {
			MessageProducer producer = session.createProducer(receivedMessage.getJMSReplyTo());
			Booking tempBooking = (Booking) receivedMessage.getObject();
			
			ObjectMessage replyMsg = session.createObjectMessage(tempBooking);
			replyMsg.setJMSCorrelationID(receivedMessage.getJMSCorrelationID());
			
			System.out.println("JMSCorrelationID: " + replyMsg.getJMSCorrelationID());
			
			producer.send(replyMsg); 
			System.out.println("Notyfying " + tempBooking.getCustomer());
		}
		catch (JMSException ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public void onException(JMSException ex) {
		System.out.println("Error from the onException() method " + ex);
	}
}
