import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A class that parses an XML file into a Bayesian network
 */
public class NetworkXmlParser {

    /**
     * Parses the XML file at the given path and returns a BayesianNetwork object.
     *
     * @param filePath Path to the XML file.
     * @return The parsed BayesianNetwork object.
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created.
     * @throws IOException                  If any IO errors occur.
     * @throws SAXException                 If any parse errors occur.
     * @throws IllegalArgumentException     If the XML structure or content is invalid.
     */
    public BayesianNetwork parse(String filePath) throws ParserConfigurationException, IOException, SAXException {
        // 1. Setup DOM Parser
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // 2. Parse XML file into Document object
        File xmlFile = new File(filePath);
        if (!xmlFile.exists()) {
            throw new IOException("XML file not found: " + filePath);
        }
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();

        // 3. Get Root Element (<NETWORK>)
        Element networkElement = document.getDocumentElement();
        if (!networkElement.getNodeName().equals("NETWORK")) {
            throw new IllegalArgumentException("Root element must be <NETWORK>");
        }

        // 4. Initialize lists and map for variables and definitions
        List<Variable> variables = new ArrayList<>();
        List<Definition> definitions = new ArrayList<>();
        Map<String, Variable> variableMap = new HashMap<>(); // Helper map to find Variables by name quickly

        // 5. Parse <VARIABLE> tags
        NodeList variableNodes = networkElement.getElementsByTagName("VARIABLE");
        for (int i = 0; i < variableNodes.getLength(); i++) {
            Node node = variableNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element variableElement = (Element) node;
                Variable variable = parseVariableNode(variableElement);
                if (variableMap.containsKey(variable.getName())) {
                    throw new IllegalArgumentException("Duplicate variable name found: " + variable.getName());
                }
                variables.add(variable);
                variableMap.put(variable.getName(), variable);
            }
        }
        System.out.println("Parsed " + variables.size() + " variables.");

        // 6. Parse <DEFINITION> tags
        NodeList definitionNodes = networkElement.getElementsByTagName("DEFINITION");
        for (int i = 0; i < definitionNodes.getLength(); i++) {
            Node node = definitionNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element definitionElement = (Element) node;
                // Pass the variableMap to find parent/child Variable objects
                Definition definition = parseDefinitionNode(definitionElement, variableMap);
                definitions.add(definition);
            }
        }
        System.out.println("Parsed " + definitions.size() + " definitions.");

        // 7. Create the BayesianNetwork object
        // Use the filename (without extension for example: . or XML) as the network name
        String networkName = xmlFile.getName().replaceFirst("[.][^.]+$", "");

        // Build the BayesianNetwork object
        BayesianNetwork network = new BayesianNetwork(networkName, definitions, variables);

        return network;
    }

    /**
     * Parses a <VARIABLE> element and returns a Variable object.
     */
    private Variable parseVariableNode(Element variableElement) {
        // Get the name of the variable
        String name = getElementTextContent(variableElement, "NAME");
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("<VARIABLE> tag must contain a non-empty <NAME> tag");
        }

        List<String> outcomes = new ArrayList<>();
        // Get the outcomes of the variable
        NodeList outcomeNodes = variableElement.getElementsByTagName("OUTCOME");
        if (outcomeNodes.getLength() == 0) {
            throw new IllegalArgumentException("Variable '" + name + "' must have at least one <OUTCOME>");
        }
        for (int i = 0; i < outcomeNodes.getLength(); i++) {
            String outcome = outcomeNodes.item(i).getTextContent();
            if (outcome == null || outcome.trim().isEmpty()) {
                throw new IllegalArgumentException("Variable '" + name + "' has an empty <OUTCOME> tag");
            }
            // Add the outcome to the String list (of outcomes)
            outcomes.add(outcome.trim());
        }

        // Use Variable constructor to create a new Variable object
        return new Variable(name, outcomes);
    }

    /**
     * Parses a <DEFINITION> element and returns a Definition object.
     */
    private Definition parseDefinitionNode(Element definitionElement, Map<String, Variable> variableMap) {
        // Get the name of the variable
        String forName = getElementTextContent(definitionElement, "FOR");
        if (forName == null || forName.isEmpty()) {
            throw new IllegalArgumentException("<DEFINITION> tag must contain a non-empty <FOR> tag");
        }
        Variable childVar = variableMap.get(forName);
        // Check if the variable is defined
        if (childVar == null) {
            throw new IllegalArgumentException("Variable '" + forName + "' mentioned in <FOR> tag was not defined in a <VARIABLE> tag.");
        }

        List<String> parentNames = new ArrayList<>();
        List<Variable> parentVars = new ArrayList<>();
        NodeList givenNodes = definitionElement.getElementsByTagName("GIVEN");
        for (int i = 0; i < givenNodes.getLength(); i++) {
            String parentName = givenNodes.item(i).getTextContent();
            // Check if the parent name is empty
            if (parentName == null || parentName.trim().isEmpty()) {
                throw new IllegalArgumentException("<DEFINITION> for '"+forName+"' has an empty <GIVEN> tag");
            }
            parentName = parentName.trim();
            Variable parentVar = variableMap.get(parentName);
            // Check if the parent variable is defined
            if (parentVar == null) {
                throw new IllegalArgumentException("Variable '" + parentName + "' mentioned in <GIVEN> tag for '" + forName + "' was not defined in a <VARIABLE> tag.");
            }
            // Add the parent name and variable to the String lists
            parentNames.add(parentName);
            parentVars.add(parentVar);
        }

        String tableString = getElementTextContent(definitionElement, "TABLE");
        // Check if the table string is empty
        if (tableString == null || tableString.isEmpty()) {
            throw new IllegalArgumentException("<DEFINITION> tag for variable '" + forName + "' must contain a non-empty <TABLE> tag");
        }

        // *** Parse the probability table string ***
        List<ProbabilityEntry> probabilityEntries = parseTableString(tableString, childVar, parentVars);

        // Use Definition constructor
        // Pass the collected parent names (can be empty) and the generated probability entries
        return new Definition(forName, parentNames, probabilityEntries);
    }

    /**
     * Parses the probability string from a <TABLE> tag and returns a list of ProbabilityEntry objects.
     * This implements the logic to match probabilities to state combinations based on standard ordering.
     *
     * @param tableString The string containing space-separated probabilities.
     * @param childVar    The child Variable object.
     * @param parentVars  The list of parent Variable objects, in the order they appeared in <GIVEN> tags.
     * @return A list of ProbabilityEntry objects.
     */
    private List<ProbabilityEntry> parseTableString(String tableString, Variable childVar, List<Variable> parentVars) {
        List<ProbabilityEntry> entries = new ArrayList<>();
        // Split the table string by whitespace to get individual probability strings
        String[] probabilityStrings = tableString.trim().split("\\s+");
        List<Double> probabilities = new ArrayList<>();
        for (String s : probabilityStrings) {
            if (s.isEmpty()) continue; // Skip empty strings
            try {
                // Parse the probability string to a double
                probabilities.add(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number format in <TABLE> for variable " + childVar.getName() + ": '" + s + "'", e);
            }
        }

        // Validate the number of probabilities against the expected size (from the <TABLE> tag)
        int expectedNumberOfProbabilities = 1;
        // Calculate expected size: product of domain sizes of parents * domain size of child
        for (Variable parent : parentVars) {
            expectedNumberOfProbabilities *= parent.getOutcomes().size();
        }
        expectedNumberOfProbabilities *= childVar.getOutcomes().size();

        if (probabilities.size() != expectedNumberOfProbabilities) {
            throw new IllegalArgumentException("Number of probabilities in <TABLE> (" + probabilities.size() +
                    ") does not match expected number based on parent/child outcomes (" + expectedNumberOfProbabilities +
                    ") for variable '" + childVar.getName() + "'");
        }

        // Use the recursive helper to generate entries based on the standard order
        RecursiveEntryGenerator generator = new RecursiveEntryGenerator(probabilities, childVar, parentVars, entries);
        generator.generate(0, new HashMap<>()); // Start recursion for the first parent (index 0)

        // Final check to ensure the number of generated entries matches the expected size
        if (entries.size() != expectedNumberOfProbabilities) {
            System.err.println("WARNING: Generated entries count (" + entries.size() +
                    ") mismatch after generation for variable '" + childVar.getName() +
                    "'. Expected: " + expectedNumberOfProbabilities);
        }

        return entries;
    }

    /**
     * Helper class to recursively generate ProbabilityEntry objects by iterating
     * through parent and child state combinations in the correct order.
     * Assumes standard CPT ordering: child iterates fastest, then last parent, etc.
     */
    private static class RecursiveEntryGenerator {
        private final List<Double> _probabilities;
        private final Variable _childVar;
        private final List<Variable> _parentVars; // Parents in the order they appear in <GIVEN>
        private final List<ProbabilityEntry> _targetEntryList;
        private int _probabilityIndex;

        RecursiveEntryGenerator(List<Double> probabilities, Variable childVar, List<Variable> parentVars, List<ProbabilityEntry> targetEntryList) {
            _probabilities = Objects.requireNonNull(probabilities);
            _childVar = Objects.requireNonNull(childVar);
            _parentVars = Objects.requireNonNull(parentVars); // Order matters!
            _targetEntryList = Objects.requireNonNull(targetEntryList);
            _probabilityIndex = 0;
        }

        /**
         * Recursively generates entries.
         * @param parentIndex The index of the current parent being processed.
         * @param currentParentStates Map holding the state assignments for parents processed so far.
         * @explain: The logic in this function is the core of inserting values into the CPT table of a variable.
         * The process stores the values of the parents of each variable in the MAP.
         * Insert the first parent that appears in the XML with its first value, and insert the parent after it in the
         * list in this way until we reach all the parents. After that, we insert all the values of the child variable.
         * Then we remove the last parent from the MAP, insert it with a change in its value,
         * and return to insert all the values of the child.
         * This way, each time we will go back and change the value of the last parent and execute the CPT table responsibly.
         */
        void generate(int parentIndex, Map<String, String> currentParentStates) {
            // Base Case: All parents have been assigned a state
            if (parentIndex == _parentVars.size()) {
                // Now, iterate through all outcomes of the child variable
                for (String childOutcome : _childVar.getOutcomes()) {
                    if (_probabilityIndex >= _probabilities.size()) {
                        throw new IndexOutOfBoundsException("Probability index exceeded bounds while processing child outcomes for " + _childVar.getName() +
                                ". Check CPT table size and ordering logic.");
                    }
                    double probability = _probabilities.get(_probabilityIndex++);
                    // Create the entry for this specific combination
                    // Use Map.copyOf to ensure each entry gets its own immutable map instance
                    ProbabilityEntry entry = new ProbabilityEntry(Map.copyOf(currentParentStates), probability, childOutcome);
                    _targetEntryList.add(entry);
                }
                return; // Finished with this combination of parent states
            }

            // Recursive Step: Iterate through outcomes of the current parent
            Variable currentParent = _parentVars.get(parentIndex);
            for (String parentOutcome : currentParent.getOutcomes()) {
                // Assign the current outcome to the current parent
                currentParentStates.put(currentParent.getName(), parentOutcome);
                // Recurse for the next parent
                generate(parentIndex + 1, currentParentStates);
                // Backtrack: Remove the assignment for the current parent so the loop can continue
                // This is necessary because we modify the map in place before the recursive call.
                currentParentStates.remove(currentParent.getName());
            }
        }
    }


    /**
     * Helper method to get the trimmed text content of the first child element with a specific tag name.
     * Returns null if the tag is not found or has no text content.

     * Example:
     * <VARIABLE>
     *     <NAME>E</NAME>
     * <VARIABLE>
     * Calling getElementTextContent(variableElement, "NAME") will return "E".
     * If the <NAME> tag is missing, the method will return null.
     */
    private String getElementTextContent(Element parentElement, String childTagName) {
        NodeList nodeList = parentElement.getElementsByTagName(childTagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null) {
                String text = node.getTextContent();
                return (text != null) ? text.trim() : null;
            }
        }
        return null;
    }
}
