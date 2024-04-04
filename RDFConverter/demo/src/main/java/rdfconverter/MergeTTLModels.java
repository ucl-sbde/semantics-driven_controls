package rdfconverter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
public class MergeTTLModels {

    public static void main(String[] args) {
        // Create an empty Jena Model
        Model mergedModel = ModelFactory.createDefaultModel();

        String pathName;			
		pathName = "examples/TUC/";

        readTTLFile(pathName + "TUC_fromBAS.ttl", mergedModel);
        readTTLFile(pathName + "TUC_fromBIM.ttl", mergedModel);

        writeTTLFile(pathName + "TUC_merged.ttl", mergedModel);
    }

    private static void readTTLFile(String filename, Model model) {
        try {
            FileInputStream input = new FileInputStream(filename);
            model.read(input, null, "TURTLE");
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeTTLFile(String filename, Model model) {
        try {
            FileOutputStream output = new FileOutputStream(filename);
            model.write(output, "TURTLE");
            output.close();
            System.out.println("Merged TTL file saved as: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


