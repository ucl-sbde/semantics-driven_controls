package uk.ac.ucl.iede.as.actor.dr;

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
public class OMActorController extends OMActorBase {

	private static final Logger log = LoggerFactory.getLogger(OMActorController.class);
	
	private String projectId = null;
	
	private Double setMin;
	private Double setNormal;
	
	public OMActorController(Entity entity) {
		super(entity);
		log.info("controller, init=" + getSelf().path().toString());
		//
		setMin = Double.valueOf( entity.getProperty("min").getValue());
		setNormal = Double.valueOf( entity.getProperty("normal").getValue());
	}	
	
	public JSONObject getObj(JSONArray jsonArray, String key, String value) {
		for(int i=0;i< jsonArray.length();i++) {
			JSONObject jsonObj = jsonArray.getJSONObject(i);
			if(jsonObj.getString(key).equals(value)) {
				return jsonObj;			
			}
		}
		return null;
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if(message instanceof OMBody) {
			//
			OMBody omBody = (OMBody) message;
			projectId = omBody.getProjectId();
			write(omBody);		
			//
			if(ready()) {
				JSONArray jsonOccZones = new JSONArray(((OMBody) getEntity().getPort("input_0").getMessage()).getBody());
				JSONArray jsonMinTempSet = new JSONArray(((OMBody) getEntity().getPort("input_1").getMessage()).getBody());
				JSONArray jsonMaxTempSet = new JSONArray(((OMBody) getEntity().getPort("input_2").getMessage()).getBody());
				JSONArray jsonOutOfComfortZones = new JSONArray(((OMBody) getEntity().getPort("input_3").getMessage()).getBody());
				Boolean DRSignal = Boolean.valueOf( ((OMBody) getEntity().getPort("input_4").getMessage()).getBody());	
				//
				JSONArray jsonArrayOut = new JSONArray();
				for(int i=0;i<jsonMinTempSet.length();i++) {
					JSONObject jsonOutputObj = new JSONObject();
					JSONObject jsonObj = jsonMinTempSet.getJSONObject(i);
					String zoneUri = jsonObj.getString("zone");
					jsonOutputObj.put("zone",zoneUri);
					
					if (getObj(jsonOccZones,"zone",zoneUri) != null) {
						if(DRSignal == true) {
							if(getObj(jsonOutOfComfortZones,"zone",zoneUri) != null) {
								// set normal control operation				
								jsonOutputObj.put("setpoint", setNormal); 
							}else{
								// set DR shed control operation
								jsonOutputObj.put("setpoint", setMin);
							}
						} else {
							// set normal control operation
							jsonOutputObj.put("setpoint", setNormal);
						}
					} else {
						// set savings control operation
						jsonOutputObj.put("setpoint", 18);
					}
					jsonArrayOut.put(jsonOutputObj);
				}
				//
				String strLog = "\n";
				strLog += "** OccZones=" + jsonOccZones.toString() + "\n";
				strLog += "** MinTemp=" + jsonMinTempSet.toString() + "\n";
				strLog += "** MaxTemp=" + jsonMaxTempSet.toString() + "\n";
				strLog += "** OutOfComfort=" + jsonOutOfComfortZones.toString() + "\n";
				strLog += "** DrSignal=" + DRSignal.toString() + "\n";
				strLog += "** ControllerOutput=" + jsonArrayOut.toString() + "\n";
				//
				log.info(strLog);
				//
				OMBody omBodyOut = new OMBody();
				omBodyOut.setProjectId(projectId);
				omBodyOut.setCollectionId(null);
				omBodyOut.setId(null);
				omBodyOut.setType(null);
				omBodyOut.setExtension("JSON");
				omBodyOut.setBody(jsonArrayOut.toString());
				//
				tell(omBodyOut);
			}
		}
	}
}