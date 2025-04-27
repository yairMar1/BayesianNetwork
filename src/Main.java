import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) {

        String txtFilePath = "input.txt";
        System.out.println("Attempting to read text file: " + txtFilePath);

        String firstLine;
        String xmlFilePath = "";
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
            StringBuilder ans = new StringBuilder();

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
                    ans.append(SimplestQuery.calculateJointProbability(network, queryLine)).append("\n");
                    System.out.println("----------------------- End of query " + i +" -----------------------------------");
                }
                if (queryLine.contains("|")) {
                    System.out.println("----------------------- Start of query " + i +" -----------------------------------");
                    System.out.println("Second option - the more complex query. " + queryLine);


                    /**
                     * Here I send the query line to the function that will classify the variables
                     * The function will return a list of maps.
                     * The first map will contain the query variable and his value we are looking for.
                     * The second map will contain the query variable and his list<ProbabilityEntry>.
                     * The third map will contain the evidence variables and his list<ProbabilityEntry>.
                     * The fourth map will contain the hidden variables and his list<ProbabilityEntry>.
                     */
                    List<Object> vars = QueryAnalysis.classifiedVariable(queryLine, network);
                    System.out.println("Classified variables: " + vars.get(0)); // The value we need to take after the normalization
                    System.out.println("Query variable: " + vars.get(1));
                    System.out.println("Evidence variables: " + vars.get(2)); // Here we have the evidence outcome that observed
                    System.out.println("Hidden variables: " + vars.get(3));

                    if (vars.size() == 4) {
                        Map<String, String> requestedQueryAssignment = (Map<String, String>) vars.get(0);
                        Map<String, List<ProbabilityEntry>> queryMap = (Map<String, List<ProbabilityEntry>>) vars.get(1);
                        Map<String, List<ProbabilityEntry>> evidenceMap = (Map<String, List<ProbabilityEntry>>) vars.get(2);
                        Map<String, List<ProbabilityEntry>> hiddenMap = (Map<String, List<ProbabilityEntry>>) vars.get(3);

                        String algorithm = queryLine.substring(queryLine.lastIndexOf(",") + 1).trim();

                        switch (algorithm) {
                            case "1":
                                System.out.println("Using Algorithm 1");
                                String result = Algorithm1.calculateProbability(requestedQueryAssignment, queryMap, evidenceMap, hiddenMap, network);
                                System.out.println("Result: " + result);
                                ans.append(result).append("\n");
                                break;
                            case "2":
                                System.out.println("Using Algorithm 2");
                                String result2 = Algorithm2.calculateProbability(requestedQueryAssignment, queryMap, evidenceMap, hiddenMap, network);
                                ans.append(result2).append("\n");
                                break;
                            case "3":
                                System.out.println("Using Algorithm 3");
                                //String result3 = Algorithm3.calculateProbability(requestedQueryAssignment, queryMap, evidenceMap, hiddenMap, network);
                                break;
                            default:
                                System.out.println("Invalid algorithm specified: " + algorithm);
                                break;
                        }
                    }else{
                        System.err.println("Error: classifiedVariable did not return the expected number of components for query: " + queryLine);
                        return;
                    }
                    System.out.println("----------------------- End of query " + i +" -----------------------------------");
                }
                // Just print the query line to confirm it's read
                System.out.println("Read query line [" + (i+1) + "]: " + queryLine);
            }

            if (!ans.isEmpty()) {
                ans.setLength(ans.length() - 1);// Remove the last newline character
            } else {
                System.out.println("No results to write to output file.");
            }

            // Write the results to the output file
            Files.writeString(OutPutFile, ans, StandardCharsets.UTF_8);

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