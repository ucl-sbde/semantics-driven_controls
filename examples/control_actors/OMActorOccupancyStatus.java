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
import uk.ac.ucl.iede.dt.as.model.Entity;
import uk.ac.ucl.iede.dt.dao.ProjectRepository;
import uk.ac.ucl.iede.dt.dom.Project;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OMActorOccupancyStatus extends OMActorBase {

	private static final Logger log = LoggerFactory.getLogger(OMActorOccupancyStatus.class);
	
	@Autowired
	private ProjectRepository projectRepository;
		
	public OMActorOccupancyStatus(Entity entity) {
		super(entity);
		log.info("OccupancyStatus, init=" + getSelf().path().toString());
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
				JSONArray jsonArray = new JSONArray(omBody.getBody());
				JSONArray jsonArrayOut =  new JSONArray();
				for(int i=0;i<jsonArray.length();i++) {
					JSONObject jsonObj = jsonArray.getJSONObject(i);
					if(jsonObj.has("value")) {
						String value = jsonObj.getString("value");
						if(value.equals("1")) {
							jsonArrayOut.put(jsonObj);							
						}
					}
				}
				//
				log.info("occupancy_status: " + jsonArrayOut.toString());
				//
				OMBody omBodyOut = new OMBody();
				omBodyOut.setProjectId(project.getProjectId());
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