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
    public static List<Map<String, List<ProbabilityEntry>>> classifiedVariable(String query, BayesianNetwork network) {
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

        // Create a map to hold the definitions (CPTs) of the network
        Map<String, List<ProbabilityEntry>> queryMap = new HashMap<>();
        for (Variable var : network.getVariables()) {
            for (String part : queryParts) {
                if (var.getName().contains(String.valueOf(part.charAt(0)))) {
                    for (Definition def : network.getDefinitions()) {
                        if (def.getName().equals(var.getName())) {
                            queryMap.put(var.getName(), def.getProbabilityList());
                            break;
                        }
                    }
                }
            }
        }
        //At the evidence Map, we will add the evidence variables
        //For example: P(A=T|B=T)
        //In this case, the evidence variable is B=T
        //So we will add the ProbabilityEntry of B when he is T
        //And don't add the ProbabilityEntry of B when he is F
//        Map<String, List<ProbabilityEntry>> evidenceMap = new HashMap<>();
//        for (Variable var : network.getVariables()) {
//            for (String part : evidenceParts) {
//                if (!queryMap.containsKey(var.getName())){
//                    if (var.getName().contains(String.valueOf(part.charAt(0)))) {
//                            for (Definition def : network.getDefinitions()) {
//                                if (def.getName().equals(var.getName())) {
//                                    evidenceMap.put(var.getName(), def.getProbabilityList());
//                                    break;
//                                }
//                            }
//
//                    }
//                }
//            }
//        }


        Map<String, List<ProbabilityEntry>> evidenceMap = new HashMap<>();

        // Create a temporary map to store the observed values for evidence variables
        Map<String, String> evidenceAssignments = new HashMap<>();
        // Loop through the evidenceParts array (e.g., ["J=T", "M=T"]) to build evidenceAssignments map
        for (String part : evidenceParts) {
            //System.out.println("Processing evidenceParts item for assignment: '" + part + "'"); // Debug processing each part
            String[] varValue = part.trim().split("="); // Example: "J=T" -> ["J", "T"]
            if (varValue.length == 2) {
                evidenceAssignments.put(varValue[0].trim(), varValue[1].trim());
            } else {
                System.err.println("Error: Invalid evidence part format when building assignments: " + part + ". Skipping.");
            }
        }

        // Debug print: Show what evidence assignments were parsed
        //System.out.println("Parsed evidence assignments: " + evidenceAssignments);


        // We iterate through network variables to find evidence variables and filter their CPTs
        for (Variable var : network.getVariables()) {
            // Check if this variable is one of the evidence variables based on the names in evidenceAssignments
            if (evidenceAssignments.containsKey(var.getName())) {
                // Get the observed value for this evidence variable from the query
                String observedValue = evidenceAssignments.get(var.getName());

                //System.out.println("\nProcessing potential evidence variable for filtering: " + var.getName());
                //System.out.println("Observed value from query for filtering: '" + observedValue + "'");


                // Find the Definition for this evidence variable
                Definition evidenceDef = null;
                for (Definition def : network.getDefinitions()) {
                    if (def.getName().equals(var.getName())) {
                        evidenceDef = def;
                        break;
                    }
                }

                if (evidenceDef != null) {
                    //System.out.println("Definition found for " + var.getName());
                    List<ProbabilityEntry> fullProbabilityList = evidenceDef.getProbabilityList();
                    List<ProbabilityEntry> filteredProbabilityList = new ArrayList<>();

                    //System.out.println("Full Probability List size for " + var.getName() + ": " + fullProbabilityList.size());

                    // Filter the list to include only entries where the outcome matches the observed value
                    for (ProbabilityEntry entry : fullProbabilityList) {
                        //System.out.println("  Checking entry: Outcome = '" + entry.getOutcome() + "', Probability = " + entry.getProbability() + ", Parents = " + entry.getStatusParent());
                        // Use equals() for string comparison, trim to be safe
                        if (entry.getOutcome().trim().equals(observedValue.trim())) {
                            //System.out.println("    -> Match found for outcome '" + observedValue + "'!");
                            filteredProbabilityList.add(entry);
                        }
//                        else {
//                            System.out.println("    -> Outcome '" + entry.getOutcome() + "' does not match observed value '" + observedValue + "'.");
//                        }
                    }

                    //System.out.println("Filtered Probability List size for " + var.getName() + " after filtering: " + filteredProbabilityList.size());


                    // Add the variable name and the filtered list to the evidenceMap ONLY IF the filtered list is not empty
                    // We add to the map here if it's an evidence variable that was found,
                    // even if the filtered list is empty, to indicate it was processed as evidence.
                    // The empty list might indicate a data issue (e.g., observed value not in CPT).
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

        List<Map<String, List<ProbabilityEntry>>> variables = new ArrayList<>();
        variables.add(0, queryMap);
        variables.add(1, evidenceMap);
        variables.add(2, hiddenMap);

        return variables;
    }
}
