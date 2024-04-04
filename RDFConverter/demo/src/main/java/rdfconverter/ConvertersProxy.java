package rdfconverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

import org.apache.jena.rdf.model.Model;
import gr.tuc.ifc.IfcModel;

public class ConvertersProxy {

	public static void main(String[] args) throws IOException, ParseException {
		
		Long step_0;
		Long step_1;
		
		String pathName;
		String fileName;
			
		pathName = "examples/TUC/";
		fileName = "TUC";

	// Call BIMtoRDFconverter 

		System.out.println("Loading IFC-SPF...");
		step_0 = System.currentTimeMillis();
		IfcModel ifcModel = new IfcModel();
		ifcModel.readFile(new File(pathName + File.separator + fileName + ".ifc"));
		step_1 = System.currentTimeMillis();
		
		System.out.println("Detected Schema=" + ifcModel.getSchema());
		System.out.println("Loading Time Elapsed=" + (step_1 - step_0) + "ms");
		 
		BIMtoRDFconverter ifcConverter = new BIMtoRDFconverter();	
		Model rdf = ifcConverter.ifcConvert(ifcModel);
		
		FileOutputStream ifcoutputStream = new FileOutputStream(new File(pathName + File.separator + fileName + "_fromBIM.ttl"), false);
		rdf.write(ifcoutputStream, "ttl");
		ifcoutputStream.close();

	// Call BAStoRDFconverter

		Reader targetReader = new FileReader(pathName + fileName + ".csv");

		BAStoRDFconverter csvConverter = new BAStoRDFconverter();
		Model csvModel = csvConverter.csvConvert(targetReader);

		FileOutputStream csvoutputStream = new FileOutputStream(new File(pathName + File.separator + fileName + "_fromBAS.ttl"), false);
		csvModel.write(csvoutputStream, "ttl");
		csvoutputStream.close();

		System.out.println("TTL files have been created");
	

	}	
}