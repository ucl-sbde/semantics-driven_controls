package uk.ac.ucl.iede.as.actor.dr;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.ac.ucl.iede.dt.as.actor.OMActorBase;
import uk.ac.ucl.iede.dt.as.message.OMBody;
import uk.ac.ucl.iede.dt.as.model.Entity;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OMActorEmission extends OMActorBase {

    private static final Logger log = LoggerFactory.getLogger(OMActorEmission.class);
   
    private String API_URL = "https://api.electricitymap.org/v3";
    private String API_KEY = "API_KEY"; 
    
	public OMActorEmission(Entity entity) {
		super(entity);
		log.info("Emission, init=" + getSelf().path().toString());
	}

    private double[] getHistoryCarbonIntensity(String zoneCode, String emision_parameter) {
        try {
            String apiUrl = API_URL + "/carbon-intensity/history?zone=" + zoneCode;
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("auth-token", API_KEY);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                	response.append(inputLine);
                }
                in.close();
                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray historyArray = jsonObject.getJSONArray("history");
                List<Double> carbonIntensityValues = new ArrayList<>();
                for(int i=0;i<historyArray.length();i++) {
                	JSONObject entry = historyArray.getJSONObject(i);
                    double carbonIntensity = entry.getDouble(emision_parameter);
                    carbonIntensityValues.add(carbonIntensity);
                }
                double[] result = new double[carbonIntensityValues.size()];
                for (int i = 0; i < carbonIntensityValues.size(); i++) {
                    result[i] = carbonIntensityValues.get(i);
                }
                return result;
            } else {
            	log.warn("Error fetching data. Response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
        	log.warn("Error fetching data: " + e.getMessage());
            return null;
        }
    }

    public double getLatestCarbonIntensity(String zoneCode, String emision_parameter) {
        try {
            String apiUrl = API_URL + "/carbon-intensity/latest?zone=" + zoneCode;
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("auth-token", API_KEY);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                //
                JSONObject jsonObject = new JSONObject(response.toString());
                double carbonIntensity = jsonObject.getDouble(emision_parameter);
                return carbonIntensity;
            } else {
                log.warn("Error fetching data. Response code: " + responseCode);
                return -1; 
            }
        } catch (IOException e) {
        	log.warn("Error fetching data: " + e.getMessage());
            return -1; 
        }
    }

	@Override
	public void onReceive(Object message) throws Throwable {
		String projectId = "";
		if(message instanceof OMBody) {
			OMBody omBody = (OMBody) message;
			projectId = omBody.getProjectId();
			String zoneCode = "GR"; 
	        String emision_parameter = "carbonIntensity";
	        Double emissionThreshold = .0;
	        Boolean DRSignal = false;
	        double[] carbonIntensityValues = getHistoryCarbonIntensity(zoneCode, emision_parameter);
	        Double currentEmission = getLatestCarbonIntensity(zoneCode, emision_parameter);
	        Arrays.sort(carbonIntensityValues);
	        int n = carbonIntensityValues.length;
	        double k = (3.0 * n - 1) / 4; 
	        if(k == Math.floor(k)) {
	        	emissionThreshold = carbonIntensityValues[(int) k - 1];
	        } else {
	        	emissionThreshold = (carbonIntensityValues[(int) k - 1] + carbonIntensityValues[(int) k]) / 2;
	        }
	        if(currentEmission > emissionThreshold) {
	            DRSignal = true;
	        } else {
	            DRSignal = false;
	        }
	        log.info("emission_threshold: " + emissionThreshold + ", current_emission: " + currentEmission);
	        OMBody omBodyOut = new OMBody();
	        omBodyOut.setCollectionId(null);
	        omBodyOut.setProjectId(projectId);
	        omBodyOut.setId(null);
	        omBodyOut.setType(null);
	        omBodyOut.setExtension(null);
	        omBodyOut.setBody(DRSignal.toString());

	        tell(omBodyOut);
		}
	}
}