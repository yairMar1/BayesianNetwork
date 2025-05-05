import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements the first inference algorithm:
 * Calculation of P(Query | Evidence) by summing out hidden variables.
 * P(Q|E) = α * Σ_hidden P(Q, E, hidden)
 * where the joint probability P(Q, E, hidden) is calculated using the chain rule.
 */
public class Algorithm1 {

    private static int _additions = 0;
    private static int _multiplications = 0;

    // Reset counters for each query
    public static void resetCounters() {
        _additions = 0;
        _multiplications = 0;
    }

    /**
     * @param requestedQueryAssignment Map containing the single requested assignment for the query variable (e.g., {"B": "T"}).
     * @param queryMap                 Map of Query Variable Name to its full List<ProbabilityEntry>.
     * @param evidenceMap              Map of Evidence Variable Name to its filtered List<ProbabilityEntry>.
     * @param hiddenMap                Map of Hidden Variable Name to its full List<ProbabilityEntry>.
     * @param network                  The full Bayesian Network object.
     * @return A string representing the calculated conditional probability and the operation counts.
     */
    public static String calculateProbability(Map<String, String> requestedQueryAssignment, Map<String, List<ProbabilityEntry>> queryMap, Map<String, List<ProbabilityEntry>> evidenceMap, Map<String, List<ProbabilityEntry>> hiddenMap, BayesianNetwork network) {
        resetCounters();

        // Extract query variable name and the specific requested outcome
        String queryVarName = requestedQueryAssignment.keySet().iterator().next(); // Get name from the requested assignment
        String requestedQueryOutcome = requestedQueryAssignment.get(queryVarName); // Get the requested outcome

        if (!queryMap.containsKey(queryVarName)) {return "Error: Requested query variable '" + queryVarName + "' not found in queryMap.";}

        // Map to store variable
        // the map looks like: {varName, Variable(the object)}
        String queryVariableName = queryMap.keySet().iterator().next();
        Map<String, Variable> variableMap = network.getVariables().stream()
                .collect(Collectors.toMap(Variable::getName, v -> v));

        // Map to store the evidence
        // the map looks like: {varName, outcome}
        final Map<String, String> evidenceAssignments = evidenceMap.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(0).getOutcome()));


        // Store the query variable in the Definition object
        Definition queryDefinition = network.getDefinitions().stream()
                .filter(def -> def.getName().equals(queryVariableName))
                .findFirst()
                .orElse(null);

        if (queryDefinition != null) {
            // Set of parent names for the query variable
            Set<String> parentNames = new HashSet<>(queryDefinition.getParents());

            Set<String> evidenceVarNames = evidenceAssignments.keySet();

            // If the value is equal,we can go directly in the query variable's CPT.
            if (parentNames.equals(evidenceVarNames)) {
                System.out.println("Optimization: Evidence matches parents for " + queryVariableName + ". Attempting direct CPT lookup.");

                for (ProbabilityEntry entry : queryDefinition.getProbabilityList()) {
                    // Check if the outcome matches
                    if (entry.getOutcome().equals(requestedQueryAssignment.get(queryVariableName))) {
                        // Check if the parent status in the entry matches the given evidence
                        if (entry.getStatusParent().equals(evidenceAssignments)) {
                            // Found the exact entry!
                            double directProbability = entry.getProbability();
                            System.out.println("Direct CPT lookup successful. Probability = " + directProbability);
                            // Return the result immediately, 0 additions and multiplications for this path
                            return String.format(Locale.US, "%.5f,0,0", directProbability);
                        }
                    }
                }
            }
        }

        System.out.println("No direct match found in CPT. Proceeding with Algorithm 1");
        // to iterate over ALL its outcomes later for normalization.
        Variable queryVariable = null;
        for(Variable var : network.getVariables()){
            if(var.getName().equals(queryVarName)){
                queryVariable = var;
                break;
            }
        }
        if(queryVariable == null){
            return "Error: Query variable '" + queryVarName + "' not found in network.";
        }

        // We need to calculate the unnormalized probability for all outcomes of the query variable
        // Map: {Query Outcome , Unnormalized Probability Sum}
        Map<String, Double> unnormalizedProbabilities = new HashMap<>();
        for(String outcome : queryVariable.getOutcomes()){
            unnormalizedProbabilities.put(outcome, 0.0);
        }

        List<String> hiddenVarNames = new ArrayList<>(hiddenMap.keySet()); // list of hidden variable names

        // We need to iterate through ALL outcomes of the query variable
        // and for each, sum over hidden variables.
        // The initial assignment starts with the fixed evidence variables.
        Map<String, String> currentAssignment = new HashMap<>();

        // Add evidence assignments to the initial assignment
        // We can get the observed value from the *outcome* of any entry in the filtered list in evidenceMap.
        for (Map.Entry<String, List<ProbabilityEntry>> entry : evidenceMap.entrySet()) {
            String evVarName = entry.getKey();
            List<ProbabilityEntry> filteredList = entry.getValue();
            if (!filteredList.isEmpty()) {
                // The observed value is the outcome of any entry in the filtered list (they should all be the same)
                String observedValue = filteredList.get(0).getOutcome();
                currentAssignment.put(evVarName, observedValue); // we add the evidence variable name and its observed value to the assignment
            } else {
                System.err.println("Warning: Evidence variable " + evVarName + " has an empty filtered list. This may indicate a data issue or impossible evidence.");
            }
        }

        // Recursive summation over hidden variables for each outcome of the query variable
        for(String currentQueryOutcome : queryVariable.getOutcomes()){
            // Start the recursive summation with the query variable fixed to this *current* outcome
            currentAssignment.put(queryVarName, currentQueryOutcome);

            // Start the recursion for the first hidden variable (index 0)
            double sumForOutcome = sumOverHidden(0, hiddenVarNames, currentAssignment, queryMap, evidenceMap, hiddenMap, network);
            unnormalizedProbabilities.put(currentQueryOutcome, sumForOutcome);

            // After summing for one query outcome, remove the query variable assignment
            // for the next iteration over query outcomes (for backtracking)
            currentAssignment.remove(queryVarName);
        }

        // Normalize the probabilities
        double normalizationFactor = 0.0;
        boolean firstProbForNormalization = true; // Flag to count additions correctly during normalization sum
        for(double prob : unnormalizedProbabilities.values()){
            normalizationFactor += prob;
            if (!firstProbForNormalization) {
                _additions++; // Count the addition during normalization sum
            } else {
                firstProbForNormalization = false;
            }
        }

        // Handle case where normalization factor is zero (evidence is impossible)
        if (normalizationFactor == 0.0) {
            // If normalization factor is 0, it means the evidence is impossible (P(E)=0).
            // P(Q|E) is undefined, but typically returned as 0.0 in this context.
            // We still need to return the result for the *requested* query outcome.
            if (unnormalizedProbabilities.containsKey(requestedQueryOutcome)) {
                String result = String.format("%.5f", 0.0) + "," + _additions + "," + _multiplications;
                return result;
            } else {
                // An internal error.
                return "Error: Requested query outcome '" + requestedQueryOutcome + "' not found in unnormalized probabilities map after summation.";
            }
        }

        // Get the unnormalized probability for the specific outcome requested in the original query
        double unnormalizedRequestedProb = unnormalizedProbabilities.get(requestedQueryOutcome);
        double normalizedProbability = unnormalizedRequestedProb / normalizationFactor;
        String result = String.format("%.5f", normalizedProbability) + "," + _additions + "," + _multiplications;

        return result;
    }

    /**
     * Recursive helper function to sum over hidden variable combinations.
     *
     * @param hiddenIndex      The index of the current hidden variable being processed (in hiddenVarNames list).
     * @param hiddenVarNames   List of all hidden variable names.
     * @param currentAssignment Assignment being built recursively (includes Evidence, fixed Query, and partially Hidden).
     * This map is modified during the recursion and backtracked.
     * @param queryMap         Map of Query Variable Name to its full List<ProbabilityEntry>.
     * @param evidenceMap      Map of Evidence Variable Name to its filtered List<ProbabilityEntry>.
     * @param hiddenMap        Map of Hidden Variable Name to its full List<ProbabilityEntry>.
     * @param network          The full Bayesian Network object (needed to get Variable objects for outcomes).
     * @return The sum of joint probabilities for all combinations of the remaining hidden variables.
     */
    private static double sumOverHidden(int hiddenIndex, List<String> hiddenVarNames, Map<String, String> currentAssignment, Map<String, List<ProbabilityEntry>> queryMap, Map<String, List<ProbabilityEntry>> evidenceMap, Map<String, List<ProbabilityEntry>> hiddenMap, BayesianNetwork network) {

        // Base case: All hidden variables have been assigned values
        if (hiddenIndex == hiddenVarNames.size()) {
            // We have a full assignment (Query value + Evidence values + specific Hidden values)
            // Calculate the joint probability of this full assignment
            double jointProb = calculateJointProbability(currentAssignment, queryMap, evidenceMap, hiddenMap, network);
            return jointProb;
        }

        // Recursive step: Iterate through outcomes of the current hidden variable
        String currentHiddenVarName = hiddenVarNames.get(hiddenIndex);
        // Need the Variable object to get outcomes for the current hidden variable
        Variable currentHiddenVariable = null;
        for(Variable var : network.getVariables()){
            if(var.getName().equals(currentHiddenVarName)){
                currentHiddenVariable = var;
                break;
            }
        }
        if(currentHiddenVariable == null){
            System.err.println("Error: Hidden variable " + currentHiddenVarName + " not found in network variables during recursion. Cannot get outcomes.");
            return 0.0; // Indicate error
        }

        double sum = 0.0;
        boolean firstOutcomeForSum = true; // Flag to count additions correctly

        for (String outcome : currentHiddenVariable.getOutcomes()) {
            // Assign the current outcome to the current hidden variable in the assignment
            currentAssignment.put(currentHiddenVarName, outcome);

            // Recursively sum over the remaining hidden variables
            double resultOfRecursiveCall = sumOverHidden(hiddenIndex + 1, hiddenVarNames, currentAssignment, queryMap, evidenceMap, hiddenMap, network);

            // Add the result of the recursive call (which is the sum of joint probs for all combinations
            // below this branch) to the current sum at this level.
            sum += resultOfRecursiveCall;

            // Count the addition ONLY if this is not the very first term being added to 0.0 at this level of summation.
            // The first term is just assigned (sum = result), subsequent terms involve an addition (sum = sum + result).
            if (!firstOutcomeForSum) {
                _additions++; // Count the addition here
            } else {
                firstOutcomeForSum = false; // The first outcome's result has been added
            }

            // Backtrack: Remove the assignment for the current hidden variable
            // This is crucial so that the next iteration of the loop assigns the *next* outcome correctly.
            currentAssignment.remove(currentHiddenVarName);
        }

        return sum; // Return the sum of joint probabilities for all combinations starting from this branch
    }

    /**
     * Calculates the joint probability of a given full assignment using the chain rule.
     * P(Assignment) = Prod( P(Variable=value | Parents(Variable)=parent_values) )
     * Counts (Number of Variables - 1) multiplications per call if Number of Variables > 1.
     *
     * @param assignment The full assignment of values for all variables (Query, Evidence, Hidden).
     * Map: VarName -> assigned Value.
     * @param queryMap   Map of Query Variable Name to its full List<ProbabilityEntry>.
     * @param evidenceMap Map of Evidence Variable Name to its filtered List<ProbabilityEntry>.
     * @param hiddenMap  Map of Hidden Variable Name to its full List<ProbabilityEntry>.
     * @param network    The full Bayesian Network object (needed to get parent names from Definitions).
     * @return The joint probability of the full assignment.
     */
    private static double calculateJointProbability(Map<String, String> assignment, Map<String, List<ProbabilityEntry>> queryMap, Map<String, List<ProbabilityEntry>> evidenceMap, Map<String, List<ProbabilityEntry>> hiddenMap, BayesianNetwork network) {

        double jointProb = 1.0;
        List<Definition> definitions = network.getDefinitions();
        int numberOfVariablesInNetwork = definitions.size(); // N = number of variables in the network

        // Iterate through all variables (Definitions) in the network to calculate P(Xi | Parents(Xi)) terms
        for (Definition def : definitions) {
            String varName = def.getName();
            String assignedValue = assignment.get(varName); // Get the value of this variable in the current assignment

            if (assignedValue == null) {
                // This variable should always be in the full assignment. If not, it's an error.
                System.err.println("Error: Variable " + varName + " not found in assignment during joint probability calculation.");
                return 0.0; // Error - joint probability is 0
            }

            // Get the list of probability entries for this variable from the correct map
            List<ProbabilityEntry> probEntries = null;
            if(queryMap.containsKey(varName)){
                probEntries = queryMap.get(varName); // Full list for query variable
            } else if (evidenceMap.containsKey(varName)){
                // For evidence variables, probEntries list is ALREADY filtered by the observed value.
                // We still need to find the entry within this filtered list that matches the parent states.
                probEntries = evidenceMap.get(varName);
            } else if (hiddenMap.containsKey(varName)){
                probEntries = hiddenMap.get(varName); // Full list for hidden variable
            } else {
                System.err.println("Error: Variable " + varName + " from definition not found in query, evidence, or hidden maps.");
                return 0.0; // Error - joint probability is 0
            }

            if (probEntries == null || probEntries.isEmpty()) {
                // This indicates a data issue or impossible evidence if an evidence variable was added with an empty list.
                // If no entries, the probability term is 0.
                System.err.println("Warning: Probability entries list is null or empty for variable " + varName + " in calculateJointProbability.");
                return 0.0; // Probability term is 0 if no entries are available, making the joint probability 0.
            }

            // Find the matching ProbabilityEntry for this variable and its parent states in this assignment
            ProbabilityEntry matchingEntry = findMatchingProbabilityEntry(varName, assignedValue, assignment, def.getParents(), probEntries);


            if (matchingEntry == null) {
                // If no matching entry is found for the specific assignment (value + parent states),
                // the probability term P(...) is 0.0, making the whole joint probability 0.0.
                return 0.0;
            }

            double probabilityTerm = matchingEntry.getProbability();

            // Multiply this probability term into the joint probability
            jointProb *= probabilityTerm;

            // Optimization: If jointProb becomes 0.0 at any point, the total is 0, can stop early.
            if (jointProb == 0.0) {
                return 0.0;
            }
        }

        // *** Count multiplications ONCE per call, after the loop ***
        // If there are N terms being multiplied, there are N-1 multiplication operations.
        // The number of terms is the number of variables in the network.
        if (numberOfVariablesInNetwork > 1) {
            _multiplications += (numberOfVariablesInNetwork - 1);
        }
        // If numberOfVariablesInNetwork is 1, (1-1)=0 multiplications are added, which is correct.

        return jointProb;
    }

    /**
     * Finds the ProbabilityEntry in a list that matches a specific outcome and parent assignments.
     * This method is crucial for looking up the correct probability from the CPT list.
     *
     * @param varName          The name of the variable (for context/debugging).
     * @param assignedValue    The outcome value being sought for this variable in the entry (e.g., "T" in B=T).
     * @param fullAssignment   The full assignment for all variables (to get the values of the parents from).
     * @param parentNames      The list of parent names for this variable's definition (order from XML/Definition matters for some CPT formats, but here we match by name).
     * @param probEntries      The list of ProbabilityEntry for this variable (full list for Query/Hidden, filtered for Evidence).
     * @return The matching ProbabilityEntry, or null if none found.
     */
    private static ProbabilityEntry findMatchingProbabilityEntry(String varName, String assignedValue, Map<String, String> fullAssignment, List<String> parentNames, List<ProbabilityEntry> probEntries) {

        if (probEntries == null || probEntries.isEmpty()) {
            // Cannot find a match in a null or empty list. Indicates an issue with the input probEntries list.
            System.err.println("Warning: Attempted to find matching entry in a null or empty list for variable " + varName + " (assigned: " + assignedValue + ")");
            return null;
        }

        for (ProbabilityEntry entry : probEntries) {
            //Check if the entry's outcome matches the assigned value for the current variable
            if (entry.getOutcome().trim().equals(assignedValue.trim())) {

                //Check if the parent states in the entry match the parent states in the full assignment
                Map<String, String> entryParentStates = entry.getStatusParent(); // The map of parent states required by *this specific CPT entry*

                boolean parentsMatch = true;

                // We need to check if the parent requirements of the entry match the values in the full assignment.
                // Iterate through the parent states defined *in this CPT entry*.
                // This handles cases where the entryParentStates might be empty if the variable has no parents.
                if (parentNames != null && !parentNames.isEmpty()) {
                    // Iterate through the parents expected by the definition (using parentNames)
                    // and check if their values in the full assignment match what is required by this entry.

                    for (String parentName : parentNames) {
                        // Get the required state for this parent from the CPT entry's parent state map
                        String requiredParentValue = entryParentStates.get(parentName);

                        // Get the actual state of this parent from the full assignment
                        String actualParentValueInAssignment = fullAssignment.get(parentName);

                        // If the required parent value is null, it might indicate a malformed entryParentStates map
                        // for a variable that *does* have parents according to parentNames.
                        // entryParentStates contain all parents if parentNames is not empty.
                        if (requiredParentValue == null) {
                            System.err.println("Warning: Entry for " + varName + "=" + assignedValue + " is missing required parent state for '" + parentName + "'. Skipping entry.");
                            parentsMatch = false; // Mismatch
                            break; // No match for this entry
                        }

                        // Compare the required parent value from the CPT entry with the actual value in the current assignment
                        if (actualParentValueInAssignment == null || !actualParentValueInAssignment.trim().equals(requiredParentValue.trim())) {
                            parentsMatch = false; // The parent's value in the assignment doesn't match the required value for this entry
                            break; // No match for parent states for this entry, move to the next ProbabilityEntry
                        }
                    }
                } else {
                    // If the variable has no parents, the entry's parent state map (entryParentStates) must be empty for a valid entry.
                    if (!entryParentStates.isEmpty()){
                        // This entry is malformed - it has parent states but the variable has no parents.
                        System.err.println("Warning: Entry for " + varName + "=" + assignedValue + " has parent states but variable has no parents (" + entryParentStates + "). Skipping entry.");
                        parentsMatch = false;
                    }
                }

                if (parentsMatch) {
                    return entry;
                }
            }
        }
        return null;// No matching entry found in the provided list for the given outcome and parent states
    }

}