package rdfconverter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.springframework.stereotype.Service;

@Service
public class BAStoRDFconverter {

	private Model rdfModel;

	public static void main(String[] args) throws IOException, ParseException {
		String filePath = "src/main/java/rdfconverter/";
		Reader targetReader = new FileReader(filePath + "TUC_Points_final.csv");

		BAStoRDFconverter converter = new BAStoRDFconverter();
		Model model = converter.converter(targetReader);
		model.write(System.out, "ttl");

		// Write the model to the output file
		try (FileWriter out = new FileWriter(filePath + "TUC_fromBAS.ttl")) {
			model.write(out, "TURTLE");
		}

		System.out.println("TTL file has been created");
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
		rdfModel.setNsPrefix("quantitykind", "http://qudt.org/vocab/quantitykind/>");

		Iterable<CSVRecord> recordList = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(targetReader).getRecords();
		parseResources(recordList);

		return (rdfModel);
	}

	private void parseResources(Iterable<CSVRecord> recordIterator) throws ParseException {
		for (CSVRecord record : recordIterator) {
			Resource resEneProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "EnergyProperty");
			resEneProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Property"));
			resEneProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("quantitykind") + "Energy"));
			Resource resPowerProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "PowerProperty");
			resPowerProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Property"));
			resPowerProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("quantitykind") + "Power"));
			Resource resOccProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "OccProperty");
			resOccProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Property"));
			resOccProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("quantitykind") + "Occupancy"));
			Resource resTempProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "TempProperty");
			resTempProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Property"));
			resTempProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("quantitykind") + "Temperature"));
			Resource resHumProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "HumProperty");
			resHumProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Property"));
			resHumProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("quantitykind") + "Humidity"));
			Resource resPresProperty = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "PresProperty");
			resPresProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("saref") + "Property"));
			resPresProperty.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("quantitykind") + "Pressure"));

			// iterating over the common serial numbers to identify the system's composition
			HashSet<String> uniqueValues = new HashSet<>();
			uniqueValues.add(record.get("device_identifier"));

			// adding common properties
			Iterator<String> iterate_value = uniqueValues.iterator();

			while (iterate_value.hasNext()) {
				if (record.get("device_identifier").equals(iterate_value.next())) {
					String device_identifier = record.get("device_identifier");

					Resource resEquipment = rdfModel
							.createResource(rdfModel.getNsPrefixURI("om") + device_identifier + "_Equipment");
					resEquipment.addLiteral(
							ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4ener") + "serialNumber"),
							ResourceFactory.createStringLiteral(device_identifier));

					if (record.get("device_name").contains("thermostat")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Thermostat"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory
										.createResource(rdfModel.getNsPrefixURI("s4bldg") + "UnitaryControlElement"));

					} else if (record.get("device_name").contains("vav")
							|| (record.get("device_name").contains("variable"))
									&& (record.get("device_name").contains("air"))
									&& (record.get("device_name").contains("volume"))) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory
										.createResource(rdfModel.getNsPrefixURI("brick") + "Variable_Air_Volume_Box"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "FlowTerminal"));
					
					} else if (record.get("device_name").contains("damper")
							|| record.get("device_name").contains("dp")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Damper"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Damper"));

							} else if (record.get("device_name").contains("split")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Terminal_Unit"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "FlowTerminal"));

					} else if (record.get("device_name").contains("fan")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Fan"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Fan"));
					} else if (record.get("device_name").contains("boiler")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Boiler"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Boiler"));
					} else if (record.get("device_name").contains("chiller")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Chiller"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Chiller"));
					

					
					} else if (record.get("device_name").contains("coil")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Coil"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Coil"));
					}

					else if (record.get("device_name").contains("compressor")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Compressor"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Compressor"));
					} else if (record.get("device_name").contains("heat")
							&& record.get("device_name").contains("exchanger")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Heat_Exchanger"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "HeatExchanger"));
					}

					else if (record.get("device_name").contains("pump")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Pump"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Pump"));
					}

					else if (record.get("device_name").contains("valve")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Valve"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Valve"));
					}

					else if (record.get("device_name").contains("ahu")) {

						resEquipment.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Air_Handler_Unit"));
						resEquipment.addProperty(RDF.type,
								ResourceFactory
										.createResource(rdfModel.getNsPrefixURI("s4bldg") + "DistributionDevice"));
					}

					String data_point_identifier = record.get("data_point_identifier");

					if (record.get("data_point_name").contains("sensor")) {

						if (record.get("data_point_name").contains("occ")) {
							Resource resOccSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_OccSensingPoint");
							resOccSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Occupancy_Sensor"));
							resOccSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							resOccSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
							resOccSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resOccProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resOccSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resOccSensingPoint);
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
									resOccSensingPoint);

						} else if (record.get("data_point_name").contains("motion")) {
							Resource resMotSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_OccSensingPoint");
							resMotSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Motion_Sensor"));
							resMotSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							resMotSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
							resMotSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resOccProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resMotSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resMotSensingPoint);
							resEquipment.addProperty(
								ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
								resMotSensingPoint);
	
						} else if (record.get("data_point_name").contains("temp")
								&& record.get("data_point_name").contains("supply")) {
							Resource resTempSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_STempSensingPoint");
							resTempSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Supply_Temperature_Sensor"));
							resTempSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							resTempSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
							resTempSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resTempProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resTempSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resTempSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resTempSensingPoint);

						} else if (record.get("data_point_name").contains("temp")
								&& record.get("data_point_name").contains("return")) {
							Resource resTempSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_RTempSensingPoint");
							resTempSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Return_Temperature_Sensor"));
							resTempSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							resTempSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
							resTempSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resTempProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resTempSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resTempSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resTempSensingPoint);

						} else if (record.get("data_point_name").contains("temp")
								&& record.get("data_point_name").contains("discharge")) {
							Resource resTempSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_DTempSensingPoint");
							resTempSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Discharge_Temperature_Sensor"));
							resTempSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							resTempSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
							resTempSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resTempProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resTempSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resTempSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resTempSensingPoint);

						} else if (record.get("data_point_name").contains("temp")) {
							Resource resTempSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_TempSensingPoint");
							resTempSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Temperature_Sensor"));
							resTempSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							resTempSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
							resTempSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resTempProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resTempSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resTempSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resTempSensingPoint);

						} else if (record.get("data_point_name").contains("hum")) {
							Resource resHumSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_HumSensingPoint");
							resHumSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Humidity_Sensor"));
							resHumSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							resHumSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
							resHumSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resHumProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resHumSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resHumSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resHumSensingPoint);

						} else if (record.get("data_point_name").contains("pos")) {
							Resource resPosSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_PosSensingPoint");
							resPosSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Position_Sensor"));
							resPosSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resPosSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resPosSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resPosSensingPoint);

						} else if (record.get("data_point_name").contains("speed")) {
							Resource resSpeedSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_SpeedSensingPoint");
							resSpeedSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Speed_Sensor"));
							resSpeedSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resSpeedSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resSpeedSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resSpeedSensingPoint);

						} else if (record.get("data_point_name").contains("flow")
								&& record.get("data_point_name").contains("supply")) {
							Resource resFlowSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_SFlowSensingPoint");
							resFlowSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Supply_Air_Flow_Sensor"));
							resFlowSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSensingPoint);

						} else if (record.get("data_point_name").contains("flow")
								&& record.get("data_point_name").contains("discharge")) {
							Resource resFlowSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_DFlowSensingPoint");
							resFlowSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Discharge_Air_Flow_Sensor"));
							resFlowSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSensingPoint);

						} else if (record.get("data_point_name").contains("flow")
								&& record.get("data_point_name").contains("mixed")) {
							Resource resFlowSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_MFlowSensingPoint");
							resFlowSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Mixed_Air_Flow_Sensor"));
							resFlowSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSensingPoint);



						} else if (record.get("data_point_name").contains("flow")) {
							Resource resFlowSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_FlowSensingPoint");
							resFlowSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Flow_Sensor"));
							resFlowSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSensingPoint);

						} else if (record.get("data_point_name").contains("co2")) {
							Resource resCo2SensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_Co2SensingPoint");
							resCo2SensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "CO2_Sensor"));
							resCo2SensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resCo2SensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resCo2SensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resCo2SensingPoint);

						} else if (record.get("data_point_name").contains("power")) {
							Resource resPowerSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_PowerSensingPoint");
							resPowerSensingPoint.addProperty(RDF.type,
									ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Power_Sensor"));
							resPowerSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							resPowerSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
							resPowerSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resPowerProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resPowerSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resPowerSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resPowerSensingPoint);

						} else if (record.get("data_point_name").contains("pressure")
								&& record.get("data_point_name").contains("supply")) {
							Resource resPresSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_SPreSensingPoint");
									resPresSensingPoint.addProperty(RDF.type,
									ResourceFactory.createResource(
											rdfModel.getNsPrefixURI("brick") + "Supply_Air_Static_Pressure_Sensor"));
											resPresSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
									resPresSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
									resPresSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resPresProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resPresSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resPresSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resPresSensingPoint);

						} else if (record.get("data_point_name").contains("pressure")
								&& record.get("data_point_name").contains("exhaust")) {
							Resource resPresSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_EPreSensingPoint");
									resPresSensingPoint.addProperty(RDF.type,
									ResourceFactory.createResource(
											rdfModel.getNsPrefixURI("brick") + "Exhaust_Air_Static_Pressure_Sensor"));
											resPresSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
									resPresSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
									resPresSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resPresProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resPresSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resPresSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resPresSensingPoint);

						} else if (record.get("data_point_name").contains("pressure")
								&& record.get("data_point_name").contains("discharge")) {
							Resource resPresSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_DPreSensingPoint");
									resPresSensingPoint.addProperty(RDF.type,
									ResourceFactory.createResource(
											rdfModel.getNsPrefixURI("brick") + "Discharge_Air_Static_Pressure_Sensor"));
											resPresSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
									resPresSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
									resPresSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resPresProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resPresSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resPresSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resPresSensingPoint);

						} else if (record.get("data_point_name").contains("pressure")) {
							Resource resPresSensingPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_PreSensingPoint");
									resPresSensingPoint.addProperty(RDF.type,
									ResourceFactory
											.createResource(rdfModel.getNsPrefixURI("brick") + "Pressure_Sensor"));
											resPresSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
									resPresSensingPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Sensor"));
									resPresSensingPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "observes"),
									resPresProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resPresSensingPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resPresSensingPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resPresSensingPoint);
						} else {
						}

					} else if (record.get("data_point_name").contains("cmd")
							|| record.get("data_point_name").contains("sp")
							|| record.get("data_point_name").contains("writeble")
							|| record.get("data_point_name").contains("param")) {
						if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("temp")
								&& record.get("data_point_name").contains("supply")

								&& (record.get("data_point_name").contains("heat") == true)) {
							Resource resHeatTempSetPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_SHeatTempSetPoint");
							resHeatTempSetPoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Supply_Air_Temperature_Heating_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resHeatTempSetPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resHeatTempSetPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resHeatTempSetPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resHeatTempSetPoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("temp")
								&& record.get("data_point_name").contains("discharge")

								&& (record.get("data_point_name").contains("heat") == true)) {
							Resource resHeatTempSetPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_DHeatTempSetPoint");
							resHeatTempSetPoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Discharge_Air_Temperature_Heating_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resHeatTempSetPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resHeatTempSetPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resHeatTempSetPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resHeatTempSetPoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("temp")

								&& (record.get("data_point_name").contains("heat") == true)) {
							Resource resHeatTempSetPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_HeatTempSetPoint");
							resHeatTempSetPoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Heating_Temperature_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resHeatTempSetPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resHeatTempSetPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resHeatTempSetPoint);
									
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resHeatTempSetPoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("supply")
								&& record.get("data_point_name").contains("temp")
								&& record.get("data_point_name").contains("cool") == true) {
							Resource resCoolTempSetPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_SCoolTempSetpoint");
							resCoolTempSetPoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Supply_Air_Temperature_Cooling_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resCoolTempSetPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resCoolTempSetPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resCoolTempSetPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resCoolTempSetPoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("temp")
								&& (record.get("data_point_name").contains("cool") == true)) {
							Resource resCoolTempSetPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_CoolTempSetpoint");
							resCoolTempSetPoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Cooling_Temperature_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resCoolTempSetPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resCoolTempSetPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resCoolTempSetPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resCoolTempSetPoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("discharge")
								&& record.get("data_point_name").contains("temp")
								&& (record.get("data_point_name").contains("heat") == false
										&& record.get("data_point_name").contains("cool") == true)) {
							Resource resCoolTempSetPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_DCoolTempSetpoint");
							resCoolTempSetPoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Discharge_Air_Temperature_Cooling_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resCoolTempSetPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resCoolTempSetPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resCoolTempSetPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resCoolTempSetPoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("temp")
								&& (record.get("data_point_name").contains("high") == true
										|| record.get("data_point_name").contains("max") == true)) {
							Resource resSetpointMax = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_TempSetpointMax");
							resSetpointMax.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Max_Temperature_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resSetpointMax.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resSetpointMax.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resSetpointMax);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resSetpointMax);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("temp")
								&& (record.get("data_point_name").contains("low") == true
										&& record.get("data_point_name").contains("min") == true)) {
							Resource resSetpointMin = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_TempSetpointMin");
							resSetpointMin.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Min_Temperature_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resSetpointMin.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resSetpointMin.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resSetpointMin);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resSetpointMin);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("temp")
								&& record.get("data_point_name").contains("discharge")) {
							Resource resTempSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_DTempSetpoint");
							resTempSetpoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Discharge_Air_Temperature_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resTempSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resTempSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resTempSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resTempSetpoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("temp")
								&& record.get("data_point_name").contains("supply")) {
							Resource resTempSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_STempSetpoint");
							resTempSetpoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Supply_Air_Temperature_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resTempSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resTempSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resTempSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resTempSetpoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("temp")) {
							Resource resTempSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_TempSetpoint");
							resTempSetpoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Temperature_Setpoint"));

							resTempSetpoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Actuator"));
							resTempSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "controls"),
									resTempProperty);
							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resTempSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resTempSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resTempSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resTempSetpoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("setpoint")
								&& record.get("data_point_name").contains("cooling")
								&& record.get("data_point_name").contains("flow")
								&& record.get("data_point_name").contains("supply")) {
							Resource resFlowSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_CoolSFlowSetpoint");
							resFlowSetpoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Cooling_Supply_Air_Flow_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resFlowSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSetpoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("setpoint")
								&& record.get("data_point_name").contains("cooling")
								&& record.get("data_point_name").contains("flow")
								&& record.get("data_point_name").contains("discharge")) {
							Resource resFlowSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_CoolDFlowSetpoint");
							resFlowSetpoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Cooling_Discharge_Air_Flow_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resFlowSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSetpoint);

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("setpoint")
								&& record.get("data_point_name").contains("heating")
								&& record.get("data_point_name").contains("flow")
								&& record.get("data_point_name").contains("supply")) {
							Resource resFlowSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_HeatSFlowSetpoint");
							resFlowSetpoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Heating_Supply_Air_Flow_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resFlowSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSetpoint);


						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("setpoint")
								&& record.get("data_point_name").contains("heating")
								&& record.get("data_point_name").contains("flow")
								&& record.get("data_point_name").contains("discharge")) {
							Resource resFlowSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_HeatDFlowSetpoint");
							resFlowSetpoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Heating_Discharge_Air_Flow_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resFlowSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSetpoint);


						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("setpoint")
								&& record.get("data_point_name").contains("flow")
								&& record.get("data_point_name").contains("supply")) {
							Resource resFlowSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_SFlowSetpoint");
							resFlowSetpoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Supply_Air_Flow_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resFlowSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSetpoint);


						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("setpoint")
								&& record.get("data_point_name").contains("flow")
								&& record.get("data_point_name").contains("discharge")) {
							Resource resFlowSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_DFlowSetpoint");
							resFlowSetpoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Discharge_Air_Flow_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resFlowSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSetpoint);


					

						} else if (record.get("data_point_name").contains("sp")
								&& record.get("data_point_name").contains("setpoint")
								&& record.get("data_point_name").contains("flow")) {
							Resource resFlowSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_FlowSetpoint");
							resFlowSetpoint.addProperty(RDF.type, ResourceFactory.createResource(
									rdfModel.getNsPrefixURI("brick") + "Flow_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFlowSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resFlowSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFlowSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFlowSetpoint);


						} else if (record.get("data_point_name").contains("onoff")) {
							Resource resOnOffCommandPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_OnOffCommand");
							resOnOffCommandPoint.addProperty(RDF.type,
									ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "On_Off_Command"));

							// create a blank node
							Resource externalReferenceOn = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resOnOffCommandPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReferenceOn);
							externalReferenceOn.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resOnOffCommandPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);
							


							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resOnOffCommandPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resOnOffCommandPoint);


						} else if (record.get("data_point_name").contains("send")) {
							Resource resRunPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_Run_Request_Command");

							resRunPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Run_Request_Command"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resRunPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resRunPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent

							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resRunPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resRunPoint);

						} else if (record.get("data_point_name").contains("mode")) {
							Resource resModePoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_Mode_Command");
							resModePoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Mode_Command"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resModePoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resModePoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resModePoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resModePoint);

						} else if (record.get("data_point_name").contains("speed")) {
							Resource resSpeedSetpointPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_Speed_Setpoint");
							resSpeedSetpointPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Speed_Setpoint"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resSpeedSetpointPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resSpeedSetpointPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resSpeedSetpointPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resSpeedSetpointPoint);

						} else if (record.get("data_point_name").contains("lockout")) {
							Resource resLockoutPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_Lockout_Command");
							resLockoutPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Lockout_Command"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resLockoutPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resLockoutPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resLockoutPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resLockoutPoint);

							// creating delay parameter point linked to the controller
							// from current iteration
						

						} else if (record.get("data_point_name").contains("hum")
								&& record.get("data_point_name").contains("setpoint")
								&& record.get("data_point_name").contains("sp")) {
							Resource resHumSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_Humidity_Setpoint");
							resHumSetpoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Humidity_Setpoint"));

							resHumSetpoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Actuator"));
							resHumSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "controls"),
									resHumProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resHumSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resHumSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resHumSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resHumSetpoint);

						} else if (record.get("data_point_name").contains("pres")
								&& record.get("data_point_name").contains("setpoint")
								&& record.get("data_point_name").contains("sp")) {
							Resource resPreSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_Pressure_Setpoint");
							resPreSetpoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Pressure_Setpoint"));

							resPreSetpoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Actuator"));
							resPreSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "controls"),
									resPresProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resPreSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resPreSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resPreSetpoint);
									
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resPreSetpoint);

						} else if (record.get("data_point_name").contains("freq")
								&& record.get("data_point_name").contains("setpoint")
								&& record.get("data_point_name").contains("sp")) {
							Resource resFreqSetpoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_Frequency_Setpoint");
							resFreqSetpoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Frequency_Setpoint"));

							resFreqSetpoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("saref") + "Actuator"));
							resFreqSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "controls"),
									resPresProperty);

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resFreqSetpoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resFreqSetpoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resFreqSetpoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resFreqSetpoint);
									

						} else if (record.get("data_point_name").contains("delay")) {
							Resource resDelayPoint = rdfModel.createResource(
									rdfModel.getNsPrefixURI("om") + device_identifier + "_Delay_Parameter");
							resDelayPoint.addProperty(RDF.type, ResourceFactory
									.createResource(rdfModel.getNsPrefixURI("brick") + "Delay_Parameter"));

							// create a blank node
							Resource externalReference = rdfModel.createResource();

							// create a triple with the blank node as the subject
							resDelayPoint.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
									externalReference);
							externalReference.addProperty(ResourceFactory
									.createProperty(rdfModel.getNsPrefixURI("ref") + "hasTimeseriesId"),
									data_point_identifier);

							resDelayPoint.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isPointOf"),
									resEquipment);

							// upd parent
							resEquipment.addProperty(
									ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPoint"),
									resDelayPoint);
									resEquipment.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("saref") + "consistOf"),
										resDelayPoint);

						} else {
						}
					}
				}

			}
		}
	}
}
