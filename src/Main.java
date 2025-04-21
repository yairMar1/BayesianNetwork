import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        String txtFilePath = "src/input.txt";
        System.out.println("Attempting to read text file: " + txtFilePath);

        String firstLine;
        String xmlFilePath = "src/";
        //Try to read the first line of the text file, to extract which XML file to parse
        try {
            firstLine = Files.lines(Paths.get(txtFilePath), StandardCharsets.UTF_8).findFirst()
                    .orElse("There was an error reading the file");
            xmlFilePath += firstLine;
            System.out.println("Attempting to parse XML file: " + xmlFilePath);
        } catch (IOException e) {
            System.err.println("File I/O Error: " + e.getMessage());
            e.printStackTrace();
        }

        NetworkXmlParser parser = new NetworkXmlParser();

        try {
            BayesianNetwork network = parser.parse(xmlFilePath);
            System.out.println("\n--- Successfully Parsed Bayesian Network ---");

            // Get the string representation of the network
            String networkOutputString = network.toString(); // Use the existing toString method

            // Define the output file path
            Path outputPath = Paths.get("BayesianNetwork.txt");
            Path OutPutFile = Paths.get("output.txt");

            // Write the string to the file using Files.writeString
            Files.writeString(outputPath, networkOutputString, StandardCharsets.UTF_8);

            /**
             * Here I start to read the queries from the text file

             * First option - the simplest query. A query will ask about the joint probability of all variables.
             * In this case the values of all variables will be given in the query

             * Second option - the more complex query.
             * A query will ask about the probability of getting a value for one query variable, given multiple evidence variables.
             * After the query, it is indicated which algorithm to use: 1, 2, or 3.
             */

            List<String> lines = Files.readAllLines(Paths.get(txtFilePath), StandardCharsets.UTF_8);
            System.out.println("Attempting to read queries from text file: " + txtFilePath.replace("src/", "") +
                     " and " + xmlFilePath.replace("src/", ""));

            // Skip the first line as it contains the XML file name
            for (int i = 1; i < lines.size(); i++) {
                String queryLine = lines.get(i).trim();
                if (queryLine.isEmpty()) {
                    System.out.println("Skipping empty line at index " + (i+1));
                    continue; // Skip empty lines if any
                }
                if(!queryLine.contains("|")){
                    System.out.println("----------------------- Start of query " + i +" -----------------------------------");
                    System.out.println("First option - the simplest query. " + queryLine);
                    System.out.println(SimplestQuery.calculateJointProbability(network, queryLine));
                    Files.writeString(OutPutFile, SimplestQuery.calculateJointProbability(network, queryLine), StandardCharsets.UTF_8);
                    System.out.println("----------------------- End of query " + i +" -----------------------------------");
                }
                // For now, just print the query line to confirm it's read
                System.out.println("Read query line [" + (i+1) + "]: " + queryLine);
            }


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