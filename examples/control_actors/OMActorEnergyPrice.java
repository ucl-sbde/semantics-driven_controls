package uk.ac.ucl.iede.as.actor.dr;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
public class OMActorEnergyPrice extends OMActorBase {

	private static final Logger log = LoggerFactory.getLogger(OMActorEnergyPrice.class);
	
	@Autowired
	private ProjectRepository projectRepository;
	
	@Autowired
	private DataFileService fileService;
	
	private String prices;
	
	public OMActorEnergyPrice(Entity entity) {
		super(entity);
		log.info("Energy Price, init=" + getSelf().path().toString());
		prices = entity.getProperty("file").getValue();
	}
	
	public static LocalTime getCurrentTime() {
		// Set the time zone to Greece
		TimeZone greekTimeZone = TimeZone.getTimeZone("Europe/Athens");
		// Create a SimpleDateFormat object with the desired format
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		dateFormat.setTimeZone(greekTimeZone);
		// Get the current date and time
		Date currentDate = new Date();
		// Format the date using the Greek time zone
		String greekTimeString = dateFormat.format(currentDate);
		// Parse the formatted time string to LocalTime
		return LocalTime.parse(greekTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
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
				Optional<ProjectProperty> propertyOpt = project.getProperties().stream().filter(p -> p.getPropertyName().equalsIgnoreCase(prices)).findFirst();
				if(propertyOpt.isPresent()) {
					String file_id = propertyOpt.get().getPropertyValue();
					String body = fileService.content(file_id);	
					//
					//
					Double priceThreshold = .0;
					Double price = .0;
					Boolean DRSignal = false;
					Double lastValidPrice = null;
					CSVFormat format = CSVFormat.DEFAULT.withFirstRecordAsHeader();
					// Create a CSV parser to read the file specified by the file path.
					CSVParser parser = new CSVParser(new StringReader(body), format);
					// Extract the column containing the prices
					double[] prices = parser.getRecords().stream()
							.mapToDouble(record -> Double.parseDouble(record.get(1)))
							.toArray();
					// Sort the prices in ascending order
					Arrays.sort(prices);
					// Calculate the index of the third quartile
					int n = prices.length;
					double k = (3.0 * n - 1) / 4;
					// Determine the price threshold as the third quartile
					if (k == Math.floor(k)) {
						priceThreshold = prices[(int) k - 1];
					} else {
						priceThreshold = (prices[(int) k - 1] + prices[(int) k]) / 2;
					}
					LocalTime currentTime = getCurrentTime();
					format = CSVFormat.DEFAULT.withFirstRecordAsHeader();
					parser = new CSVParser(new StringReader(body), format);
					for (CSVRecord record : parser) {
						String csvTimeString = record.get(0);
						LocalTime csvTime = LocalTime.parse(csvTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
						if (currentTime.isBefore(csvTime)) {
							price = (lastValidPrice != null ? lastValidPrice : Double.parseDouble(record.get(1)));
							if (price > priceThreshold) {
								DRSignal = true;
							} else
								DRSignal = false;
							break;
						}
						lastValidPrice = Double.parseDouble(record.get(1));
					}
					parser.close();
					log.info("price_threshold: " + priceThreshold + ", current_price: " + price);
					//
					OMBody omBodyOut = new OMBody();
					omBodyOut.setCollectionId(null);
					omBodyOut.setProjectId(projectId);
					omBodyOut.setId(null);
					omBodyOut.setType(null);
					omBodyOut.setExtension(null);
					omBodyOut.setBody(DRSignal.toString());
					//
					tell(omBodyOut);	
				}
			}
		}
	}
}