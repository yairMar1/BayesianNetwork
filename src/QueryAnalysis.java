import java.util.*;

/**
 * This class is used to analyze the query string
 * and classify the variables into query, evidence, and hidden variables.
 * It takes a query string and a Bayesian network as input.
 * returns a list of maps containing the classified variables.

 * The first map contains the query variable.
 * The second map contains the evidence variables.
 * The third map contains the hidden variables.
 */

public class QueryAnalysis {
    public static List<Object> classifiedVariable(String query, BayesianNetwork network) {
        // remove the "P(" from the beginning of the query line
        String newLine = query.replace("P(","");

        String[] parts = newLine.split("\\),");
        //System.out.println(Arrays.toString(parts));

        // Extract the query variable
        // Split the first part by "|"
        // the firsts elements of the array are the query variable
        // the last element of the array is the evidence variable
        String[] AllQueryParts = parts[0].split("\\|");
        //System.out.println("All the query" + Arrays.toString(AllQueryParts));

        String[] evidenceParts = AllQueryParts[1].split(",");
        //System.out.println("Evidence " + Arrays.toString(evidenceParts));

        String[] queryParts = AllQueryParts[0].split(",");
        //System.out.println("Query " + Arrays.toString(queryParts));


        // Assuming only one assignment in the query part for this algorithm type P(Var=Value | ...)
        // I assume it, because all the queries are like P(One Variable|?)
        Map<String, String> requestedQueryAssignment = new HashMap<>();
        if (queryParts.length != 1) {
            System.err.println("Warning: Expected exactly one variable assignment in the query part, but found " + queryParts.length + ". Using the first one: " + queryParts[0]);
        }
        String queryAssignmentPart = queryParts[0]; // Take the first part as the query assignment

        String[] varValue = queryAssignmentPart.trim().split("=");
        if (varValue.length == 2) {
            requestedQueryAssignment.put(varValue[0].trim(), varValue[1].trim());
        } else {
            System.err.println("Error: Invalid query part format when building requestedQueryAssignment: " + queryAssignmentPart + ". Expected 'Var=Value'.");
            return new ArrayList<>(); // Return empty list to indicate error
        }

        // Create a map to hold the definitions (CPTs) of the network
        Map<String, List<ProbabilityEntry>> queryMap = new HashMap<>();
        if (queryParts.length != 1) {
            System.err.println("Error: QueryAnalysis expects exactly one query assignment like Var=Val.");
            return new ArrayList<>();
        }

        String queryVarName = varValue[0].trim(); // Extract the name (e.g. "B0")

        // Find the Definition (CPT) specifically for this query variable name
        Definition queryDef = network.getDefinitions().stream()
                .filter(def -> def.getName().equals(queryVarName))
                .findFirst()
                .orElse(null);

        if (queryDef != null) {
            // Put only the CPT of the actual query variable into the map
            queryMap.put(queryVarName, queryDef.getProbabilityList());
        } else {
            System.err.println("Error: Definition not found for query variable: " + queryVarName);
            return new ArrayList<>(); // Cannot proceed without query variable definition
        }


        Map<String, List<ProbabilityEntry>> evidenceMap = new HashMap<>();

        // Create a temporary map to store the observed values for evidence variables
        Map<String, String> evidenceAssignments = new HashMap<>();
        // Loop through the evidenceParts array (e.g., ["J=T", "M=T"]) to build evidenceAssignments map
        for (String part : evidenceParts) {
            //System.out.println("Processing evidenceParts item for assignment: '" + part + "'"); // Debug processing each part
            String[] varValue1 = part.trim().split("="); // Example: "J=T" -> ["J", "T"]
            if (varValue1.length == 2) {
                evidenceAssignments.put(varValue1[0].trim(), varValue1[1].trim());
            } else {
                System.err.println("Error: Invalid evidence part format when building assignments: " + part + ". Skipping.");
            }
        }

        // We iterate through network variables to find evidence variables and filter their CPTs
        for (Variable var : network.getVariables()) {
            // Check if this variable is one of the evidence variables based on the names in 'evidenceAssignments'
            if (evidenceAssignments.containsKey(var.getName())) {
                String observedValue = evidenceAssignments.get(var.getName()); // The value from the map 'evidenceAssignments'
                //System.out.println("\nProcessing potential evidence variable for filtering: " + var.getName());
                //System.out.println("Observed value from query for filtering: '" + observedValue + "'");

                // Find the Definition for this evidence variable
                Definition evidenceDef = null;
                for (Definition def : network.getDefinitions()) {
                    if (def.getName().equals(var.getName())) {
                        evidenceDef = def;
                        break;
                    }
                }// Until here we have the definition of the variable

                if (evidenceDef != null) {
                    //System.out.println("Definition found for " + var.getName());
                    List<ProbabilityEntry> fullProbabilityList = evidenceDef.getProbabilityList();
                    List<ProbabilityEntry> filteredProbabilityList = new ArrayList<>(); // List to hold filtered entries
                    //System.out.println("Full Probability List size for " + var.getName() + ": " + fullProbabilityList.size());

                    // Filter the list to include only entries where the outcome matches the observed value
                    for (ProbabilityEntry entry : fullProbabilityList) {
                        //System.out.println("  Checking entry: Outcome = '" + entry.getOutcome() + "', Probability = " + entry.getProbability() + ", Parents = " + entry.getStatusParent());
                        // Use equals() for string comparison, trim to be safe
                        if (entry.getOutcome().trim().equals(observedValue.trim())) {
                            //System.out.println("    -> Match found for outcome '" + observedValue + "'!");
                            filteredProbabilityList.add(entry);
                        }//else {
//                            System.out.println("    -> Outcome '" + entry.getOutcome() + "' does not match observed value '" + observedValue + "'.");
//                        }
                    }
                    //System.out.println("Filtered Probability List size for " + var.getName() + " after filtering: " + filteredProbabilityList.size());

                    evidenceMap.put(var.getName(), filteredProbabilityList); // Add even if empty for now for clarity
                    //System.out.println(var.getName() + " added to evidenceMap with " + filteredProbabilityList.size() + " entries.");
                } else {
                    System.err.println("Error: Definition not found for evidence variable: " + var.getName());
                }
            }
        }

        Map<String, List<ProbabilityEntry>> hiddenMap = new HashMap<>();
        for (Variable var : network.getVariables()) {
            if(!queryMap.containsKey(var.getName()) && !evidenceMap.containsKey(var.getName())) {
                for (Definition def : network.getDefinitions()) {
                    if (def.getName().equals(var.getName())) {
                        hiddenMap.put(var.getName(), def.getProbabilityList());
                        break;
                    }
                }
            }
        }

        List<Object> variablesInfo = new ArrayList<>();
        variablesInfo.add(0, requestedQueryAssignment);
        variablesInfo.add(1, queryMap);
        variablesInfo.add(2, evidenceMap);
        variablesInfo.add(3, hiddenMap);

        return variablesInfo;
    }
}