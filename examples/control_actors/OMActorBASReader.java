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
public class OMActorBASReader extends OMActorBase {

	private static final Logger log = LoggerFactory.getLogger(OMActorBASReader.class);
	
	@Autowired
	private ProjectRepository projectRepository;
	
	public OMActorBASReader(Entity entity) {
		super(entity);
		log.info("BMS read, init=" + getSelf().path().toString());
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
					JSONArray jsonArray = new JSONArray(omBody.getBody());
					for(int i=0;i<jsonArray.length();i++) {
						JSONObject jsonPointSet = new JSONObject();
						JSONObject jsonArrayObj = jsonArray.getJSONObject(i);
						String point_id = jsonArrayObj.getString("point");
						JSONObject jsonPoint = new JSONObject();
						jsonPoint.put("global", point_id);
						jsonPointSet.append("points", jsonPoint);
						String output = OMUtils.post1(basProperty.getPropertyValue() + "API_ENDPOINT", jsonPointSet.toString());
						JSONObject jsonOutput = new JSONObject(output);	
						JSONArray jsonOutputArray =  jsonOutput.getJSONArray("points");
						if(jsonOutputArray.length() > 0) {
							String value = jsonOutput.getJSONArray("points").getJSONObject(0).getJSONObject("measure").getString("value");
							jsonArrayObj.put("value", value);
						}else {
						
						}
					}
					OMBody omBodyOut = new OMBody();
					omBodyOut.setCollectionId(null);
					omBodyOut.setProjectId(projectId);
					omBodyOut.setId(null);
					omBodyOut.setType(null);
					omBodyOut.setExtension("JSON");
					omBodyOut.setBody(jsonArray.toString());
					//
					tell(omBodyOut);
				}
			}
		}
	}
}