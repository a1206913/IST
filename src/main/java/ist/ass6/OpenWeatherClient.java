package ist.ass6;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class OpenWeatherClient {

	public static void main(String[] args) throws IOException {

		try {
			URL url = new URL(
					"http://api.openweathermap.org/data/2.5/weather?q=London,uk&appid=2de143494c0b295cca9337e1e96b00e0");

			InputStream in = url.openStream();
			StringBuffer sb = new StringBuffer();
			int c;
			while ((c = in.read()) != -1) {
				sb.append((char) c);
			}

			System.out.println(sb.toString());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
