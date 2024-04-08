package uk.ac.ucl.iede.as.actor.dr;

import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.ac.ucl.iede.dt.as.actor.OMActorBase;
import uk.ac.ucl.iede.dt.as.message.OMBody;
import uk.ac.ucl.iede.dt.as.message.OMUtils;
import uk.ac.ucl.iede.dt.as.model.Entity;
import uk.ac.ucl.iede.dt.dao.ProjectRepository;
import uk.ac.ucl.iede.dt.dom.Project;
import uk.ac.ucl.iede.dt.dom.ProjectProperty;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OMActorWriteBMS extends OMActorBase {

	private static final Logger log = LoggerFactory.getLogger(OMActorWriteBMS.class);
	
	@Autowired
	private ProjectRepository projectRepository;
	
	private String url;
	private String control;
	private String value;
	
	public OMActorWriteBMS(Entity entity) {
		super(entity);
		log.info("BMS write, init=" + getSelf().path().toString());
		value = entity.getProperty("value").getValue();
		//url = entity.getProperty("url").getValue();
	}	

	@Override
	public void onReceive(Object message) throws Throwable {
		String projectId = "";
		if(message instanceof OMBody) {
			OMBody omBody = (OMBody) message;
			projectId = omBody.getProjectId();
			Optional<Project> projectOpt = projectRepository.findById(projectId);					
			if(projectOpt.isPresent()) {				
				Project project = projectOpt.get();	
				Optional<ProjectProperty> basPropertyOpt = project.getProperties().stream().filter(p -> p.getPropertyName().equalsIgnoreCase("bas.uri")).findFirst();
				if(basPropertyOpt.isPresent()) {
					ProjectProperty basProperty = basPropertyOpt.get();
					url = basProperty.getPropertyValue();
				}
				Optional<ProjectProperty> controlPropertyOpt = project.getProperties().stream().filter(p -> p.getPropertyName().equalsIgnoreCase("bas.control")).findFirst();
				if(controlPropertyOpt.isPresent()) {
					ProjectProperty basControlProperty = controlPropertyOpt.get();
					control = basControlProperty.getPropertyValue();
				}
				//
				JSONArray jsonArray = new JSONArray(omBody.getBody());
				//
				JSONObject jsonObj = new JSONObject();
				for(int i=0;i<jsonArray.length();i++) {
					JSONObject jsonPoint = jsonArray.getJSONObject(i);	
					//
					JSONObject jsonItem = new JSONObject();
					jsonItem.put("global", jsonPoint.getString("id"));
					//
					if(jsonPoint.has("value")) {
						JSONObject jsonMeasure = new JSONObject();
						jsonMeasure.put("value",Integer.valueOf(jsonPoint.getInt("value")).toString());
						//
						jsonItem.put("measure", jsonMeasure);
					}else {
						JSONObject jsonMeasure = new JSONObject();
						jsonMeasure.put("value",Integer.valueOf(value).toString());
						//
						jsonItem.put("measure", jsonMeasure);
					}
					jsonObj.append("points", jsonItem);
				}
				//
				String output = "";
				if(Boolean.valueOf(control)) {
					output = OMUtils.post1(url + "API_ENDPOINT", jsonObj.toString());
					log.info("Write >>> " + output + " pset: " + jsonObj.toString());
				}else {
					output = "offline";
					log.info("Log >>> " + output + " pset: " + jsonObj.toString());
				}
				tell(1);
			}
		}
	}
}