/* code based on the IST VO-Folien - Chapter. 9: Messaging with JMS
 * - and -
 * http://activemq.apache.org/how-should-i-implement-request-response-with-jms.html 
 */

package ist.ass6;

import ist.ass6.model.Booking;

import java.util.Random;

import org.apache.activemq.*;

import javax.jms.*;
import javax.jms.Message;

public class Customer implements MessageListener {
	private String user = ActiveMQConnection.DEFAULT_USER;
	private String password = ActiveMQConnection.DEFAULT_PASSWORD;

	/*
	 * we need to connect to a message broker a broker it enables the exchange of
	 * requests and responses between client and remote objects by hiding and
	 * mediating all the communication between the objects or components of the
	 * system
	 */
	private String url = ActiveMQConnection.DEFAULT_BROKER_URL;

	// this will be the subjectID of our message queue
	private String subjectBooking = "bookingF";
	private int custumerNr;

	// create an empty connection
	private Connection connection;
	private Session session;
	private MessageProducer messageProducer;
	private Destination tempBookingQueue;

	// constructor with one parameter, the custumerID
	Customer(int nr) {
		setCustomerNumber(nr);

		/*
		 * A Connection object is a client's active connection to its JMS
		 * provider. It typically allocates provider resources outside the Java
		 * virtual machine
		 * 
		 * A JMS client typically creates a connection, one or more sessions, and
		 * a number of message producers and consumers. When a connection is
		 * created, it is in stopped mode. That means that no messages are being
		 * delivered
		 * 
		 * Because the creation of a connection involves setting up authentication
		 * and communication, a connection is a relatively heavyweight object.
		 * Most clients will do all their messaging with a single connection.
		 * 
		 * It is typical to leave the connection in stopped mode until setup is
		 * complete (that is, until all message consumers have been created). At
		 * that point, the client calls the connection's start method, and
		 * messages begin arriving at the connection's consumers. This setup
		 * convention minimizes any client confusion that may result from
		 * asynchronous message delivery while the client is still in the process
		 * of setting itself up.
		 * 
		 * A connection can be started immediately, and the setup can be done
		 * afterwards. Clients that do this must be prepared to handle
		 * asynchronous message delivery while they are still in the process of
		 * setting up. A message producer can send messages while a connection is
		 * stopped.
		 */

		// set up a ConnectionFactory for creating a connection to a provider
		// through the EmbeddedBroker
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
		try {
			// create and start the connection
			connection = connectionFactory.createConnection();
			connection.start();

			// create the session for producing and consuming messages
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			Destination bookingQueue = session.createQueue(subjectBooking);
			tempBookingQueue = session.createTemporaryQueue();


			// Setup a message producer to send message to the queue the server is
			// consuming from
			this.messageProducer = session.createProducer(bookingQueue);
			this.messageProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

			/*
			 * in order to realize Request/Reply, we must make the producer
			 * (customer) also a message listener to listen to incoming responses
			 * from the agent
			 */
			MessageConsumer consumeFromAgent = session.createConsumer(tempBookingQueue);
			consumeFromAgent.setMessageListener(this);

			/*
			 * create the actual message that you want to send to the Agent this
			 * messages are generated randomly by calling the method
			 * getRandomBooking() of the class BookingGenerator this method
			 * (getRandomBooking) generates random booking orders from a set of
			 * booking instances
			 */
			String customer = "Customer " + getCustumerNumber();
			Booking fBooking = BookingGenerator.getRandomBooking(customer);
			System.out.println(fBooking.consumerMessage());

			// create the actual message as Object messages
			ObjectMessage objMessage = session.createObjectMessage(fBooking);

			/*
			 * add the JMSReplyTo field in the message, so that the agent knows
			 * where to reply the messages
			 */
			objMessage.setJMSReplyTo(tempBookingQueue);

			/*
			 * set a correlation ID, so when you get a response you know which sent
			 * message the response is for send the message to the consumer
			 */
			 String correlationID = this.createRandomString();
			 objMessage.setJMSCorrelationID(correlationID);

			this.messageProducer.send(objMessage);
		} catch (JMSException ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// start the ten clients
		for (int i = 1; i <= 10; i++) {
			new Customer(i);
		}

		/*
		 * Before an application completes, you must close any connections you
		 * have created. Failure to close a connection can cause resources not to
		 * be released by the JMS provider. Closing a connection also closes its
		 * sessions and their message producers and message consumers.
		 */
		/*finally {
		if (connection != null) {
			try {
				System.out.println("Closing connection....");
				connection.close();
			}
			catch (JMSException e) {
				System.out.println("Exception in constructor()" + e);
				e.printStackTrace();
			}
				
		}
	} */
	}

	/*
	 * Passes a message to the listener (in this case, the customer) A
	 * MessageListener object is used to receive asynchronously delivered
	 * messages It defines the actions to be taken when a message arrives
	 */
	@Override
	public void onMessage(Message receivedMessage) {
		try {
			if (receivedMessage instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) receivedMessage;
				Booking b = (Booking) objMessage.getObject();

				System.out.println("Confirmation of booking for " + b.getCustomer());
			}
		} catch (JMSException ex) {
			System.out.println("from the onMessage() method " + ex);
			ex.printStackTrace();
		}
	}

	private String createRandomString() {
		Random random = new Random(System.currentTimeMillis());
		long randomLong = random.nextLong();
		return Long.toHexString(randomLong);
	}

	public void setCustomerNumber(int nr) {
		this.custumerNr = nr;
	}

	public int getCustumerNumber() {
		return this.custumerNr;
	}
}
