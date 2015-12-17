package ist.ass6;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import ist.ass6.model.Booking;

public class BookingGenerator {
	private final static List<String> DESTINATIONS = Arrays.asList(
			"Vienna, Austria", "Salzburg, Austria", "Linz, Austria",
			"Athens, Greece", "Herakleion, Greece", "Rome, Italy",
			"Madrid, Spain", "Brussels, Belgium", "Copenhagen, Denmark",
			"Amsterdam, Holland", "Stockholm, Sweden", "Thessaloniki, Greece",
			"Dublin, Ireland", "London, UK");

	private final static Random RANDOM = new Random();
	private final static long BEGINTIME = Timestamp.valueOf(
			"2016-01-01 00:00:00").getTime();
	private final static long ENDTIME = Timestamp.valueOf("2016-06-30 00:00:00")
			.getTime();

	static {
		RANDOM.setSeed(System.currentTimeMillis());
	}

	private static long getRandomDateTime() {
		long diff = ENDTIME - BEGINTIME + 1;
		return BEGINTIME + (long) (Math.random() * diff);
	}

	public static Booking getRandomBooking(String customer) {
		Booking booking = new Booking(customer, RANDOM.nextInt(5) + 1,
				getRandomStringFromList(DESTINATIONS),
				new Date(getRandomDateTime()));
		return booking;
	}

	private static String getRandomStringFromList(List<String> list) {
		int idx = -1;
		while (idx < 0) {
			idx = RANDOM.nextInt(list.size() - 1);
		}
		return list.get(idx);
	}

	public static void main(String[] args) throws Exception {
		for (int i = 1; i < 11; i++)
			System.out.println(BookingGenerator.getRandomBooking("" + i));
	}
}
