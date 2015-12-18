package ist.ass6.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Booking implements Serializable {
	private static final long serialVersionUID = 6965250794343996545L;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm");
	
	private String customer;
	private int persons;
	private String destination;
	private Date date;
	
	public Booking(String customer, int persons, String destination, Date date) {
		super();
		this.customer = customer;
		this.persons = persons;
		this.destination = destination;
		this.date = date;
	}
	
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	public String getCustomer() {
		return customer;
	}

	public void setCustomer(String customer) {
		this.customer = customer;
	}

	public int getPersons() {
		return persons;
	}

	public void setPersons(int persons) {
		this.persons = persons;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@Override
	public String toString() {
		return "Booking [customer " + customer + ", persons=" + persons
				+ ", destination=" + destination + ", date=" + dateFormat.format(date) + "]";
	}
	
	public String consumerMessage() {
		String ticket = "ticket";
		if (getPersons() > 1)
			ticket = "tickets";
		return customer + " ordered " + getPersons() + " " + ticket + " to " + getDestination() + " at " + dateFormat.format(date);
	}
	
	public String consolidatorMessage() {
		String ticket = "ticket";
		if (getPersons() > 1)
			ticket = "tickets";
		return "Book " + getPersons() + " " + ticket + " to " + getDestination() + " at " + dateFormat.format(date);
	}
}
