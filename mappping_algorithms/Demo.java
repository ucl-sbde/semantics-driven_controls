package eu.openmetrics.kgg.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.jena.rdf.model.Model;
import eu.openmetrics.kgg.service.BIMtoRDFconverter;
import gr.tuc.ifc.IfcModel;

public class Demo {

	public static void main(String[] args) throws IOException {
		
		Long step_0;
		Long step_1;
		
		String pathName;
		String fileName;
			
		pathName = "examples/TUC";
		fileName = "TUC";
		
		System.out.println("Loading IFC-SPF...");
		step_0 = System.currentTimeMillis();
		IfcModel model = new IfcModel();
		model.readFile(new File(pathName + File.separator + fileName + ".ifc"));
		step_1 = System.currentTimeMillis();
		
		System.out.println("Detected Schema=" + model.getSchema());
		System.out.println("Loading Time Elapsed=" + (step_1 - step_0) + "ms");
		 
		BIMtoRDFconverter converter = new BIMtoRDFconverter();	
		Model rdf = converter.convert(model);
		
		FileOutputStream outputStream = new FileOutputStream(new File(pathName + File.separator + fileName + ".ttl"), false);
		rdf.write(outputStream, "ttl");
		outputStream.close();

	}	
}