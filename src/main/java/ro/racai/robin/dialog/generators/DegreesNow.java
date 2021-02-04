package ro.racai.robin.dialog.generators;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.log4j.Logger;
import ro.racai.robin.dialog.RDResponseGenerator;


public class DegreesNow implements RDResponseGenerator {
	private static final String WEATHER_QUERY =
			"http://www.meteoromania.ro/wp-json/meteoapi/v2/starea-vremii";
	private static final String IP_QUERY =
			"http://api.ipstack.com/check?access_key=";
	// Add here your own ipstack.com API key...
	private static String confIPAPIKey = "";
	private static final String DOESNOTWORKCONST = "Pagina de internet nu funcționează.";
	private static final Logger LOGGER = Logger.getLogger(DegreesNow.class.getName());
	private String currentCity = "București";

	static {
		File apiKeyFile = new File("IP-info-API-Key.txt");

		if (apiKeyFile.exists()) {
			try {
				confIPAPIKey = Files.readAllLines(apiKeyFile.toPath()).get(0).trim();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public DegreesNow() {
		setCity();
	}

	public String normalizeCityName() {
		String city = currentCity.toLowerCase();
		
		city = city.replace("ș", "s");
		city = city.replace("ț", "t");
		city = city.replace("ş", "s");
		city = city.replace("ţ", "t");
		city = city.replace("ă", "a");
		city = city.replace("â", "a");
		city = city.replace("î", "i");

		return city;
	}

	private void setCity() {
		StringBuilder content = new StringBuilder();

		try {
			URL url = new URL(DegreesNow.IP_QUERY + DegreesNow.confIPAPIKey);
			URLConnection conn = url.openConnection();
			HttpURLConnection http = (HttpURLConnection) conn;

			http.setRequestMethod("GET");
			http.connect();

			int status = http.getResponseCode();

			if (status == 200) {
				try (BufferedReader in = new BufferedReader(
						new InputStreamReader(http.getInputStream(), StandardCharsets.UTF_8))) {
					String line = in.readLine();

					while (line != null) {
						content.append(line);
						line = in.readLine();
					}
				}
			} else {
				LOGGER.error("CITY recovery query error code " + status);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		String json = content.toString();
		JSONParser parser = new JSONParser();

		try {
			JSONObject root = (JSONObject) parser.parse(json);

			if (root.containsKey("city")) {
				String city = (String) root.get("city");

				currentCity = city.trim();
				currentCity = currentCity.replace("ş", "ș");
				currentCity = currentCity.replace("ţ", "ț");
				currentCity = currentCity.replace("Ş", "Ș");
				currentCity = currentCity.replace("Ţ", "Ț");
			} else {
				LOGGER.error("CITY recovery wasn't possible. Check your API key");
			}
		} catch (ParseException pe) {
			pe.printStackTrace();
		}
	}
	
	@Override
	public String generate() {
		StringBuilder content = new StringBuilder();

		try {
			URL url = new URL(DegreesNow.WEATHER_QUERY);
			URLConnection conn = url.openConnection();
			HttpURLConnection http = (HttpURLConnection) conn;

			http.setRequestMethod("GET");
			http.connect();

			int status = http.getResponseCode();

			if (status == 200) {
				try (BufferedReader in = new BufferedReader(
						new InputStreamReader(http.getInputStream(), StandardCharsets.UTF_8))) {
					String line = in.readLine();

					while (line != null) {
						content.append(line);
						line = in.readLine();
					}
				}
			} else {
				LOGGER.error("WEATHER recovery query error code " + status);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return DOESNOTWORKCONST;
		}

		String json = content.toString();
		JSONParser parser = new JSONParser();
		String normCrtCity = normalizeCityName();
		String response = "Informație indisponibilă.";

		try {
			JSONObject root = (JSONObject) parser.parse(json);
			JSONArray features = (JSONArray) root.get("features");

			for (Object f : features) {
				JSONObject feat = (JSONObject) f;
				JSONObject props = (JSONObject) feat.get("properties");
				String nume = (String) props.get("nume");
				String tempe = (String) props.get("tempe");

				if (nume.equalsIgnoreCase(normCrtCity)
						|| nume.toLowerCase().contains(normCrtCity)) {
					String[] parts = tempe.split("\\.");
					String minus = "";

					if (parts[0].startsWith("-")) {
						minus = "minus ";
						parts[0] = parts[0].substring(1);
					}

					if (parts[0].equals("1") && parts[1].equals("0")) {
						response = "În " + currentCity + " este " + minus + " un grad Celsius.";
					} else if (!parts[1].equals("0")) {
						response = "În " + currentCity + " sunt " + minus + parts[0] + " virgulă "
								+ parts[1] + " grade Celsius.";
					} else {
						response = "În " + currentCity + " sunt " + minus + parts[0]
								+ " grade Celsius.";
					}

					break;
				}
			}
		} catch (ParseException pe) {
			pe.printStackTrace();
		}

		return response;
	}
}
