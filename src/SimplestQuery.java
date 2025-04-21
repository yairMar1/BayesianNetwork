import java.util.HashMap;
import java.util.Map;

public class SimplestQuery {

    /**
     * This function calculates the joint probability of a given query in a Bayesian network.
     * The query is expected to be in the format P(X1=x1, X2=x2, ..., Xn=xn),
     * where Xi are the variable names and xi are their assigned values.
     * The function returns the joint probability as a string formatted to 5 decimal places.
     *
     * @param network The Bayesian network containing the definitions (CPTs).
     * @param query   The query string representing the joint probability to calculate.
     * @return A string representing the joint probability, formatted to 5 decimal places.
     */

    public static String calculateJointProbability(BayesianNetwork network, String query) {
        // Check if the query build is correct
        if (!query.startsWith("P(") || !query.endsWith(")")) {
            return "Error: Invalid query format. Expected P(...)";
        }
        // remove "P(" and ")" from the query
        String assignmentsString = query.replace("P(", "").replace(")", "");

        // Create a map to hold the variable from the query
        // The map will hold the variable names as keys and their assigned values as values
        Map<String, String> queryAssignments = new HashMap<>();
        String[] variableValuePairs = assignmentsString.split(",");

        for (String pair : variableValuePairs) {
            String[] parts = pair.trim().split("=");
            if (parts.length == 2) {
                String varName = parts[0].trim();
                String varValue = parts[1].trim();
                if (varName.isEmpty() || varValue.isEmpty()) {
                    return "Error: Invalid variable-value pair format: " + pair;
                }
                queryAssignments.put(varName, varValue); // Add the variable and its value to the map
            } else {
                return "Error: Invalid variable-value pair format: " + pair;
            }
        }

        double jointProbability = 1.0;

        // Create a map to hold the definitions (CPTs) of the network
        Map<String, Definition> definitionMap = new HashMap<>();
        for (Definition def : network.getDefinitions()) {
            definitionMap.put(def.getName(), def);
        }

        // for each variable in the query, find its definition (CPT) and calculate the joint probability
        for (String variableName : queryAssignments.keySet()) {
            String assignedValue = queryAssignments.get(variableName); // the value in the query

            // find the definition (CPT) for the variable
            Definition definition = definitionMap.get(variableName);

            // Function to find the matching entry in the CPT
            ProbabilityEntry matchingEntry = findMatchingEntry(definition, assignedValue, queryAssignments);

            if (matchingEntry != null) {
                // Multiply the joint probability by the probability of the matching entry
                jointProbability *= matchingEntry.getProbability();

//                System.out.println("Processing: " + variableName + "=" + assignedValue +
//                        ". Found P = " + matchingEntry.getProbability() +
//                        ". Current Joint = " + jointProbability);
            } else {
                System.err.println("Warning: No matching CPT entry found for " + variableName + "=" + assignedValue +
                        " given parent assignments in query.");
                jointProbability = 0.0;
                break;
            }
        }

        String answer = String.format("%.5f", jointProbability);
        return answer + ",0," + (queryAssignments.size()-1);
    }

    /** this function finds the matching entry in the CPT for a given variable
     * the parameters are:
     * 1. definition - the definition of the variable (CPT)
     * 2. assignedValue - the value assigned to the variable in the query
     * 3. queryAssignments - the map of all variable assignments in the query
     * the function returns the matching entry if found, otherwise null
     * the function checks if the definition has parents or not
     */
    private static ProbabilityEntry findMatchingEntry(Definition definition, String assignedValue, Map<String, String> queryAssignments) {
        // Iterate over the probability entries in the definition
        for (ProbabilityEntry entry : definition.getProbabilityList()) {
            //Check if the outcome of the entry matches the assigned value
            if (entry.getOutcome().equals(assignedValue)) {
                // Check if the entry has parents
                boolean parentsMatch = true;
                Map<String, String> entryParentStates = entry.getStatusParent(); // the parents status of the entry

                // We need to check if the entry has parents
                for (Map.Entry<String, String> requiredParentEntry : entryParentStates.entrySet()) {
                    String parentName = requiredParentEntry.getKey(); // the name of the parent
                    String requiredParentValue = requiredParentEntry.getValue(); // the value of the parent in the entry

                    String queryParentValue = queryAssignments.get(parentName); // the value of the parent in the query

                    if (queryParentValue == null || !queryParentValue.equals(requiredParentValue)) {
                        parentsMatch = false;
                        break;
                    }
                }
                if (parentsMatch) {return entry;}
            }
        }
        // if no matching entry is found, return null
        return null;
    }
}