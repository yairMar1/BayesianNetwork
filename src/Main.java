import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        String xmlFilePath = "src/alarm net.XML";

        System.out.println("Attempting to parse XML file: " + xmlFilePath);
        NetworkXmlParser parser = new NetworkXmlParser();

        try {
            BayesianNetwork network = parser.parse(xmlFilePath);
            System.out.println("\n--- Successfully Parsed Bayesian Network ---");

            // Get the string representation of the network
            String networkOutputString = network.toString(); // Use the existing toString method

            // Define the output file path
            Path outputPath = Paths.get("alarm_net.txt");

            // Write the string to the file using Files.writeString
            Files.writeString(outputPath, networkOutputString, StandardCharsets.UTF_8);

            // Using toString() method
            //System.out.println(network);

        } catch (ParserConfigurationException e) {
            System.err.println("XML Parser Configuration Error: " + e.getMessage());
            e.printStackTrace();
        } catch (SAXException e) {
            System.err.println("XML Parsing Error: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("File I/O Error: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid XML Content or Structure: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Catch other potential runtime errors
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}