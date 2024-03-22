package uk.ac.ucl.iede;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.rulesys.RDFSRuleReasonerFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.springframework.stereotype.Service;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.graph.Graph;
import java.io.FileReader;
import org.apache.commons.csv.*;

@Service
public class BAStoRDFconverter {

	private Model rdfModel;

	public static void main(String[] args) throws IOException, ParseException {
		Reader targetReader = new FileReader("TUC_Points_final.csv");
		BAStoRDFconverter converter = new BAStoRDFconverter();
		Model model = converter.converter(targetReader);
		model.write(System.out, "ttl");
	}

	private Model converter(Reader targetReader) throws IOException, ParseException {

		rdfModel = ModelFactory.createDefaultModel();
		rdfModel.setNsPrefix("owl", OWL.getURI());
		rdfModel.setNsPrefix("rdf", RDF.getURI());
		rdfModel.setNsPrefix("xsd", XSD.getURI());
		rdfModel.setNsPrefix("rdfs", RDFS.getURI());
		rdfModel.setNsPrefix("schema", "http://schema.org#");
		rdfModel.setNsPrefix("brick", "https://brickschema.org/schema/Brick#");
		rdfModel.setNsPrefix("om", "http://openmetrics.eu/openmetrics#");
		rdfModel.setNsPrefix("saref", "https://saref.etsi.org/core#");
		rdfModel.setNsPrefix("s4ener", "https://saref.etsi.org/saref4ener#");
		rdfModel.setNsPrefix("s4bldg", "https://saref.etsi.org/saref4bldg#");
		rdfModel.setNsPrefix("ref", "https://brickschema.org/schema/Brick/ref#");

		Iterable<CSVRecord> recordList = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(targetReader).getRecords();
		parseResources(recordList);


		return (rdfModel);
	}

	private void parseResources(Iterable<CSVRecord> recordIterator) throws ParseException {
		for (CSVRecord record : recordIterator) {
			HashSet<String> uniqueValues = new HashSet<>();
			uniqueValues.add(record.get("resourceSerialNumber"));

			// adding common properties
			Iterator<String> iterate_value = uniqueValues.iterator();
			Resource resEneProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "_EnergyProperty");
			resEneProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Energy"));
			Resource resPowerProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "_PowerProperty");
			resPowerProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Power"));
			Resource resOccProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "_OccProperty");
			resOccProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Occupancy"));
			Resource resTempProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "_TempProperty");
			resTempProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Temperature"));
			Resource resHumProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "_HumProperty");
			resHumProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Humidity"));
			Resource resPresProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "_PresProperty");
			resPresProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Pressure"));
			Resource resPriceProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "_PriceProperty");
			resPriceProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Price"));

			// iterating over the common serial numbers to identify the system's composition
			while (iterate_value.hasNext()) {
				if (record.get("resourceSerialNumber").equals(iterate_value.next())) {
					String resourceSerialNumber = record.get("resourceSerialNumber");
					Resource resController = rdfModel
							.createResource(rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_Controller");
					resController.addProperty(RDF.type,
							ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Thermostat"));
					resController.addProperty(RDF.type,
							ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "UnitaryControlElement"));
					resController.addLiteral(
							ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4ener") + "serialNumber"),
							ResourceFactory.createStringLiteral(resourceSerialNumber));
					String datapoint = record.get("datapoint");

					if (record.get("tag").contains("sensor")) {

						// creating occupancy function and point linked to the controller from current
						// iteration
						if (record.get("tag").contains("occ") && record.get("tag").contains("sensor")) {
							Resource resOccSensingFunction = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_OccSensingFunction");
							resOccSensingFunction.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "SensingFunction"));
							Resource resOccSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_OccSensingPoint");
							resOccSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "GetSensingDataCommand"));
							resOccSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Occupancy_Sensor"));
							resOccSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);
							resOccSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "isCommandOf"),
									resOccSensingFunction);

							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resOccSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasFunction"),
									resOccSensingFunction);
							resOccSensingFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasSensorType"),
									resOccProperty);
							resOccSensingFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasCommand"),
									resOccSensingPoint);
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resOccSensingPoint);

							// creating temperature sensor function and point linked to the controller from
							// current iteration
						} else if (record.get("tag").contains("temp") && record.get("tag").contains("sensor")) {
							Resource resTempSensingFunction = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_TempSensingFunction");
							resTempSensingFunction.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "SensingFunction"));
							Resource resTempSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_TempSensingPoint");
							resTempSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "GetSensingDataCommand"));
							resTempSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Zone_Air_Temperature_Sensor"));
							resTempSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);
							resTempSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "isCommandOf"),
									resTempSensingFunction);

							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resTempSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasFunction"),
									resTempSensingFunction);
							resTempSensingFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasSensorType"),
									resTempProperty);
							resTempSensingFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasCommand"),
									resTempSensingPoint);
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resTempSensingPoint);

							// creating humidity sensor function and point linked to the controller from
							// current iteration
						} else if (record.get("tag").contains("hum") && record.get("tag").contains("sensor")) {
							Resource resHumSensingFunction = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_TempSensingFunction");
							resHumSensingFunction.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "SensingFunction"));
							Resource resHumSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_TempSensingPoint");
							resHumSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "GetSensingDataCommand"));
							resHumSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Temperature_Sensor"));
							resHumSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);
							resHumSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "isCommandOf"),
									resHumSensingFunction);

							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resHumSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasFunction"),
									resHumSensingFunction);
							resHumSensingFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasSensorType"),
									resTempProperty);
							resHumSensingFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasCommand"),
									resHumSensingPoint);
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resHumSensingPoint);

							// creating power sensor function and point linked to the controller from
							// current iteration
						} else if (record.get("tag").contains("power") && record.get("tag").contains("sensor")) {
							Resource resPowerSensingFunction = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_PowerSensingFunction");
							resPowerSensingFunction.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "SensingFunction"));
							Resource resPowerSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_PowerSensingPoint");
							resPowerSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "GetSensingDataCommand"));
							resPowerSensingPoint.addProperty(RDF.type,
									ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Power_Sensor"));
							resPowerSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);
							resPowerSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "isCommandOf"),
									resPowerSensingFunction);

							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resPowerSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasFunction"),
									resPowerSensingFunction);
							resPowerSensingFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasSensorType"),
									resPowerProperty);
							resPowerSensingFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasCommand"),
									resPowerSensingPoint);
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resPowerSensingPoint);

						} else {
						}

					} else if (record.get("tag").contains("cmd") || record.get("tag").contains("sp")
							|| record.get("tag").contains("writeble") || record.get("tag").contains("param")) {
						// creating zone temperature setpoint function and point linked to the
						// controller from current iteration
						if (record.get("tag").contains("sp")
								&& (record.get("tag").contains("high") == false
										&& record.get("tag").contains("max") == false)
								&& (record.get("tag").contains("low") == false
										&& record.get("tag").contains("min") == false)) {
							Resource resSetPointFunction = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_SetPointFunction");
							resSetPointFunction.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "ActuatingFunction"));
							resSetPointFunction.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "LevelControlFunction"));
							Resource resZoneSetPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_ZoneSetPoint");
							resZoneSetPoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Zone_Air_Temperature_Setpoint"));
							resZoneSetPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "setLevelCommand"));

							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resZoneSetPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
					        resZoneSetPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);
							resZoneSetPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "isCommandOf"),
									resSetPointFunction);

							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasFunction"),
									resSetPointFunction);
							resSetPointFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasCommand"),
									resZoneSetPoint);
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resZoneSetPoint);

							// creating max temperature setpoint function and point linked to the controller
							// from current iteration
						} else if (record.get("tag").contains("sp")
								&& (record.get("tag").contains("high") == true
										|| record.get("tag").contains("max") == true)
								&& (record.get("tag").contains("low") == false
										&& record.get("tag").contains("min") == false)) {
							Resource resSetpointMax = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_SetpointMax");
							resSetpointMax.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Max_Temperature_Setpoint_Limit"));

							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resSetpointMax.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
					        resSetpointMax.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);
							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resSetpointMax);

							// creating min temperature setpoint function and point linked to the controller
							// from current iteration
						} else if (record.get("tag").contains("sp")
								&& (record.get("tag").contains("high") == false
										&& record.get("tag").contains("max") == false)
								&& (record.get("tag").contains("low") == true
										|| record.get("tag").contains("min") == true)) {
							Resource resSetpointMin = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_SetpointMin");
							resSetpointMin.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Min_Temperature_Setpoint_Limit"));

							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resSetpointMin.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
					        resSetpointMin.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);
							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resSetpointMin);

							// creating on and off command functions and points (for a single on/off
							// datapoint) linked to the controller from current iteration
						} else if (record.get("tag").contains("onoff")) {
							Resource resOnOffFunction = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_OnOffFunction");
							resOnOffFunction.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "ActuatingFunction"));
							resOnOffFunction.addProperty(RDF.type,
									ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "OnOffFunction"));
							Resource resOnCommandPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_OnCommand");
							resOnCommandPoint.addProperty(RDF.type,
									ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "On_Command"));
							resOnCommandPoint.addProperty(RDF.type,
									ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "OnCommand"));

							// create a blank node
					        Resource externalReferenceOn = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resOnCommandPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReferenceOn);
					        externalReferenceOn.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
					        resOnCommandPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "isCommandOf"),
									resOnOffFunction);
							resOnCommandPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);
							Resource resOffCommandPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_OffCommand");
							resOffCommandPoint.addProperty(RDF.type,
									ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Off_Command"));
							resOffCommandPoint.addProperty(RDF.type,
									ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "OffCommand"));

							// create a blank node
					        Resource externalReferenceOff = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resOffCommandPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReferenceOff);
					        externalReferenceOff.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
					        resOffCommandPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "isCommandOf"),
									resOnOffFunction);
							resOffCommandPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);

							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasFunction"),
									resOnOffFunction);
							resOnOffFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasCommand"),
									resOnCommandPoint);
							resOnOffFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasCommand"),
									resOffCommandPoint);
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resOnCommandPoint);
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resOffCommandPoint);

							// creating send run_request command function and point linked to the controller
							// from current iteration
						} else if (record.get("tag").contains("send")) {
							Resource resRunFunction = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_StartStopFunction");
							resRunFunction.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "ActuatingFunction"));
							resRunFunction.addProperty(RDF.type,
									ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "StartStopFunction"));
							
							Resource resRunPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_Run_Request_Command");
							
							resRunPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Run_Request_Command"));
							resRunPoint.addProperty(RDF.type,
									ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "StartCommand"));

							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resRunPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
					        resRunPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "isCommandOf"),
									resRunFunction);
							
					        resRunPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);

							// upd parent
					        resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasFunction"),
									resRunFunction);
					        resRunFunction.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "hasCommand"),
									resRunPoint);
							
					        resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resRunPoint);
							
							// creating mode_command point linked to the controller
							// from current iteration
						} else if (record.get("tag").contains("mode")) {
							Resource resModePoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_Mode_Command");
							resModePoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Mode_Command"));
							
							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resModePoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
							resModePoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);

							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resModePoint);
							
							// creating fan speed_setpoint function and point linked to the controller
							// from current iteration
						} else if (record.get("tag").contains("fan")) {
							Resource resSpeedSetpointPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_Speed_Setpoint_Command");
							resSpeedSetpointPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Speed_Setpoint"));
							
							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resSpeedSetpointPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);
					        
					        resSpeedSetpointPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);

							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resSpeedSetpointPoint);
	
							
							// creating lockout_command function and point linked to the controller
							// from current iteration
						} else if (record.get("tag").contains("lockout")) {
							Resource resLockoutPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_Lockout_Command");
							resLockoutPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Lockout_Command"));
							
							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resLockoutPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);

							resLockoutPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);
							
							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resLockoutPoint);
							
							
							// creating delay parameter point linked to the controller
							// from current iteration
						} else if (record.get("tag").contains("delay")) {
							Resource resDelayPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + resourceSerialNumber + "_Delay_Parameter");
							resDelayPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Delay_Parameter"));
							
							// create a blank node
					        Resource externalReference = rdfModel.createResource();
					        
					        // create a triple with the blank node as the subject
					        resDelayPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), externalReference);
					        externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"), datapoint);

					        resDelayPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resController);
							
							// upd parent
							resController.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resDelayPoint);
	
						} else {}
					}
				}
			}
		}
	   // rdfModel.write(System.out, "TURTLE");
	}
}
