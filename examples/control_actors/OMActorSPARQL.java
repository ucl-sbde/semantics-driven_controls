package uk.ac.ucl.iede.as.actor.dr;

import java.util.Optional;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
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
import uk.ac.ucl.iede.dt.dom.ProjectProperty;
import uk.ac.ucl.iede.dt.service.DataFileService;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OMActorSPARQL extends OMActorBase {

	private static final Logger log = LoggerFactory.getLogger(OMActorSPARQL.class);
	
	@Autowired
	private ProjectRepository projectRepository;
	
	@Autowired
	private DataFileService fileService;
	
	private String file_code;
	
	public OMActorSPARQL(Entity entity) {
		super(entity);
		log.info("SPARQL, init=" + getSelf().path().toString());
		file_code = entity.getProperty("file").getValue();
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		String projectId = "";
		if(message instanceof OMBody) {
			OMBody omBody = (OMBody) message;
			projectId = omBody.getProjectId();
			//
			Optional<Project> projectOpt = projectRepository.findById(projectId);					
			if(projectOpt.isPresent()) {				
				Project project = projectOpt.get();
				Optional<ProjectProperty> graphPropertyOpt = project.getProperties().stream().filter(p -> p.getPropertyName().equalsIgnoreCase("graph.url")).findFirst();
				if(graphPropertyOpt.isPresent()) {
					ProjectProperty graphProperty = graphPropertyOpt.get();
					Optional<ProjectProperty> propertyOpt = project.getProperties().stream().filter(p -> p.getPropertyName().equalsIgnoreCase(file_code)).findFirst();
					if(propertyOpt.isPresent()) {
						String file_id = propertyOpt.get().getPropertyValue();
						String SPARQL = fileService.content(file_id);
						DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(graphProperty.getPropertyValue());
						Model model = accessor.getModel();
						Query query = QueryFactory.create(SPARQL);
						QueryExecution qexec = QueryExecutionFactory.create(query, model);
						ResultSet result = qexec.execSelect();
						JSONArray jsonArray = new JSONArray();
						while(result.hasNext()) {
							JSONObject jsonObject = new JSONObject();
							QuerySolution querySolution = result.nextSolution();
							String zone = querySolution.getLiteral("ZoneID").toString();
							String point = querySolution.getLiteral("point").getString();
							jsonObject.put("zone", zone);
							jsonObject.put("point", point);
							jsonArray.put(jsonObject);
							log.info("query_id: " + file_code + ", zone_id: " + zone +  " point_id: " + point);
						}
						qexec.close();
						//
						OMBody omBodyOut = new OMBody();
						omBodyOut.setProjectId(projectId);
						omBodyOut.setCollectionId(null);
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
}