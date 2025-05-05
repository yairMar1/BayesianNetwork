import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Algorithm2 {

    //TODO: 1. Understand which variables are not relevant to the query (as written on page 91)
    //TODO: 2. Prepare the factors from the Maps that are passed to the function
    //TODO: 3. Making a loop that goes through all the hidden variables.
    //TODO: 4. Gathering all the factors that have the hidden variable in them.
    //TODO: 5. Doing a join on them. And a new factor is created.
    //TODO: 6. The hidden variable is hidden in the last factor that remains.
    //TODO: 7. Finally, normalization is done for the requested query variable.

    private static int _numberOfMultiplications = 0;
    private static int _numberOfAdditions = 0;

    public static int get_numberOfAdditions() {
        return _numberOfAdditions;
    }
    public static int get_numberOfMultiplications() {
        return _numberOfMultiplications;
    }

    // Reset the counters for another query
    private static void resetCounters() {
        _numberOfAdditions = 0;
        _numberOfMultiplications = 0;
    }

    public static String calculateProbability(Map<String, String> requestedQueryAssignment, Map<String, List<ProbabilityEntry>> queryMap, Map<String, List<ProbabilityEntry>> evidenceMap, Map<String, List<ProbabilityEntry>> hiddenMap, BayesianNetwork network) throws IOException {

        resetCounters();

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

        System.out.println("Processing query with variable elimination algorithm.");
        // Identify relevant variables
        Set<String> relevantVariable = new HashSet<>();

        // Add query and evidence variables to the relevant set
        relevantVariable.addAll(queryMap.keySet());
        relevantVariable.addAll(evidenceMap.keySet());

        // If variable is not query or evidence, he will add only if he is ancestor of the query\evidence variable
        Set<String> ancestors = new HashSet<>(relevantVariable);
        for(String varName : ancestors) {
            List<String> ancestor = network.getAncestors(varName);
            relevantVariable.addAll(ancestor);
        }

        // Create initial factors, only from 'relevantVariable'
        List<Factor> initialFactors = new ArrayList<>();
        for (Definition definition : network.getDefinitions()) {
            if (relevantVariable.contains(definition.getName())) {
                initialFactors.add(new Factor(definition, network)); //first construct at Factor class
            }
        }

        // Restrict factors based on evidence
        // Arise the factors lines that are not relevant base on the evidence we saw
        List<Factor> restrictedFactors = new ArrayList<>();
        for (Factor factor : initialFactors) {
            Factor currentFactor = factor;
            // We go through all the evidence variables and try to restrict lines that are not contain the evidence outcome we saw
            for (Map.Entry<String, String> evidenceEntry : evidenceAssignments.entrySet()) {
                currentFactor = currentFactor.restrict(evidenceEntry.getKey(), evidenceEntry.getValue());
                if (currentFactor == null || currentFactor.getValues().isEmpty()) {
                    currentFactor = null;
                    break;
                }
            }
            if (currentFactor != null) {
                restrictedFactors.add(currentFactor);
            } else {
                System.out.println("Factor for " + factor.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + " became empty after restriction.");
            }
        }

        // Eliminate factors with less than 2 rows
        for (int i = 0; i < restrictedFactors.size(); i++) {
            Factor factor = restrictedFactors.get(i);
            int counter = 0;
            for (Map.Entry<Map<String, String>, Double> entry : factor.getValues().entrySet()) {
                counter++;
            }
            if(counter < 2){
                restrictedFactors.remove(factor);
                // Because we removed an element, we need to adjust the index.
                // all the elements after the removed element will shift left
                i--;
            }
        }
        for (Factor factor : restrictedFactors) {
            System.out.println(factor);
        }
        //TODO: explain - till now we have list of factors ('restrictedFactors').
        //      These factors were created only from variables desired by the algorithm.
        //      And rows were removed if necessary (rows that did not match the observed evidence variables were removed).
        //      And we removed factors that their size is less than 2.

        // Rename the List that contains the desired factors
        // We will work on the list 'factors' from now on
        List<Factor> factors;
        factors = restrictedFactors;

        // Making list of hidden variables' ordered by their names we want to eliminate
        List<String> hiddenVariableNames = hiddenMap.keySet().stream()
                .filter(relevantVariable::contains)
                .filter(hVar -> !evidenceAssignments.containsKey(hVar)) // Ensure we don't include evidence var
                .collect(Collectors.toList());
        Collections.sort(hiddenVariableNames);
        System.out.println("Elimination Order: " + hiddenVariableNames);

        // loop through the factors that contain hidden variables
        // and made a join on them
        for (String hiddenVarName : hiddenVariableNames) {
            Variable hiddenVar = variableMap.get(hiddenVarName);
            if (hiddenVar == null) continue;

            System.out.println("\n--- Eliminating: " + hiddenVarName + " ---");

            // Filter factors to join and those to keep
            List<Factor> factorsToJoin = new ArrayList<>();
            List<Factor> factorsToKeep = new ArrayList<>();
            for (Factor f : factors) {
                boolean containsHidden = f.getDomain().stream().anyMatch(v -> v.getName().equals(hiddenVarName));
                if (containsHidden) factorsToJoin.add(f);
                else factorsToKeep.add(f);
            }

            if (factorsToJoin.isEmpty()) continue;

            Factor newFactor;

            if (factorsToJoin.size() == 1) {
                newFactor = factorsToJoin.get(0);
                System.out.println("Only one factor contains " + hiddenVarName + ". No join needed.");
            } else {
                System.out.println("Factors to join for " + hiddenVarName + ": " +
                        factorsToJoin.stream()
                                .map(f -> "[" + f.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + "] (Size: " + f.getValues().size() + ")")
                                .collect(Collectors.joining(", ")));

                List<Factor> currentFactorsToJoin = new ArrayList<>(factorsToJoin);
                // Join operation will continue until only one factor remains
                // Sort the factors to join by size, then by domain name sum (ASCII).
                // All the sorts are in ascending order. From the smallest to the largest.
                while (currentFactorsToJoin.size() > 1) {
                    currentFactorsToJoin.sort(Comparator
                            .<Factor, Integer>comparing(f -> f.getValues().size())
                            .thenComparing(f -> f.getDomain().stream().mapToInt(v -> v.getName().chars().sum()).sum())
                    );

                    Factor factor1 = currentFactorsToJoin.get(0);
                    Factor factor2 = currentFactorsToJoin.get(1);
                    System.out.println("Joining pair: [" + factor1.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + "] and [" + factor2.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + "]");

                    // Call to helper method to join two factors
                    Factor joinedFactor = joinTwoFactors(factor1, factor2, evidenceAssignments);

                    // Update the list of factors to join
                    currentFactorsToJoin.remove(factor1);
                    currentFactorsToJoin.remove(factor2);
                    currentFactorsToJoin.add(joinedFactor);
                    System.out.println("Intermediate join result size: " + joinedFactor.getValues().size()); // This size should now be smaller!
                }
                // After the loop, only one factor remains in 'currentFactorsToJoin' with the current hidden variable
                newFactor = currentFactorsToJoin.get(0);
                System.out.println("Final Joined Factor for " + hiddenVarName + " (after pairwise joins):\n" + newFactor);
            }

            // Eliminate the hidden variable from the new factor
            // We will sum out the correct rows
            Factor summedOutFactor = sumOut(newFactor, hiddenVar, variableMap);
            System.out.println("Factor after summing out " + hiddenVarName + ":\n" + summedOutFactor);

            factors = factorsToKeep;

            // Check if the summed out factor is empty or has one row
            // If it is, I can discard this factor
            int counter = 0;
            for (Double factorValue : summedOutFactor.getValues().values()){
                counter++;
            }
            if(counter > 1){
                factors.add(summedOutFactor);// Add the summed out factor to the list of factors to keep
            }

        }

        // Handle the factors that remain after all hidden variables have been eliminated
        System.out.println("\n--- After Elimination ---");
        System.out.println("Remaining Factors (" + factors.size() + "):");
        factors.forEach(System.out::println);

        Factor finalFactor;
        if (factors.isEmpty()) {
            Variable queryVar = variableMap.get(queryVariableName);
            if (queryVar == null) {
                throw new IllegalStateException("Query variable '" + queryVariableName + "' not found in variableMap.");
            }
            String requestedValue = requestedQueryAssignment.get(queryVariableName);
            if (requestedValue == null) {
                throw new IllegalStateException("Requested value for query variable '" + queryVariableName + "' not found in requestedQueryAssignment map.");
            }
            Map<String, String> innerMap = new HashMap<>();
            innerMap.put(queryVariableName, requestedValue);
            Map<Map<String, String>, Double> zeroValueMap = new HashMap<>();
            zeroValueMap.put(innerMap, 0.0);
            List<Variable> domainList = new ArrayList<>();
            domainList.add(queryVar);
            finalFactor = new Factor(domainList, zeroValueMap);

        } else { // The factors need to contain only the query variable
            List<Factor> remainingFactors = new ArrayList<>(factors);
            while (remainingFactors.size() > 1) {
                remainingFactors.sort(Comparator
                        .<Factor, Integer>comparing(f -> f.getValues().size())
                        .thenComparing(f -> f.getDomain().stream().mapToInt(v -> v.getName().chars().sum()).sum())
                );
                Factor f1 = remainingFactors.get(0);
                Factor f2 = remainingFactors.get(1);
                System.out.println("Final join: [" + f1.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + "] and [" + f2.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + "]");

                Factor joined = joinTwoFactors(f1, f2, evidenceAssignments);

                remainingFactors.remove(f1);
                remainingFactors.remove(f2);
                remainingFactors.add(joined);
            }
            finalFactor = remainingFactors.get(0);
        }

        System.out.println("Final Factor (Pre-Normalization):\n" + finalFactor);

        // Normalize the final factor
        Factor normalizedFactor = normalizeFactor(finalFactor);
        System.out.println("Normalized Final Factor:\n" + normalizedFactor);

        // Store the query variable we need to look for, and the evidence variables
        Map<String, String> finalAssignment = new HashMap<>(requestedQueryAssignment);

        for (Variable v : normalizedFactor.getDomain()) {
            String varName = v.getName();
            if (evidenceAssignments.containsKey(varName)) {
                finalAssignment.put(varName, evidenceAssignments.get(varName));
            } else if (!requestedQueryAssignment.containsKey(varName)) {
                throw new IllegalStateException("Final normalized factor contains unexpected variable: " + varName);
            }
        }
        System.out.println("DEBUG: Final assignment for lookup: " + finalAssignment);
        double resultProbability = 0.0;
        try {
            Set<String> finalFactorDomainNames = normalizedFactor.getDomain().stream()
                    .map(Variable::getName)
                    .collect(Collectors.toSet());
            if (!finalAssignment.keySet().equals(finalFactorDomainNames)) {
                throw new IllegalStateException("Constructed final assignment keys do not match normalized factor domain keys. AssignKeys: " + finalAssignment.keySet() + ", FactorKeys: " + finalFactorDomainNames);
            }
            resultProbability = normalizedFactor.getValue(finalAssignment);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("Error extracting final probability: " + e.getMessage());
            resultProbability = 0.0;
        }

        // Return the result as a formatted string
        return String.format(Locale.US, "%.5f,%d,%d",
                resultProbability,
                get_numberOfAdditions(),
                get_numberOfMultiplications());
    }

    /**Helper methods*/
    private static Factor joinTwoFactors(Factor f1, Factor f2, Map<String, String> evidenceAssignments) {

        // The new domain is the union of the two factors' domains
        Set<Variable> combinedDomainSet = new HashSet<>(f1.getDomain());
        combinedDomainSet.addAll(f2.getDomain());

        List<Variable> newDomain = new ArrayList<>(combinedDomainSet);
        newDomain.sort(Comparator.comparing(Variable::getName)); // I don't have to sort, I prefer to have order in the factor.
        Map<Map<String, String>, Double> newValues = new HashMap<>(); // Store the new values of the factor

        // Call to helper function to generate all possible combination for the new domain
        List<Map<String, String>> AllCombination = generateAssignments(newDomain, evidenceAssignments);

        for (Map<String, String> rowOfCombination : AllCombination) {
            try {
                double prob1 = f1.getValue(rowOfCombination);
                double prob2 = f2.getValue(rowOfCombination);

                double combinedProbability = prob1 * prob2;
                _numberOfMultiplications++;

                //newValues.put(Map.copyOf(rowOfCombination), combinedProbability);
                Map<String, String> keyCopy = new HashMap<>(rowOfCombination);
                Map<String, String> immutableKey = Collections.unmodifiableMap(keyCopy);
                newValues.put(immutableKey, combinedProbability);

            } catch (IllegalArgumentException e) {
                System.err.println("Error during joinTwoFactors: " + e.getMessage());
                throw e;
            }
        }
        return new Factor(newDomain, newValues);
    }

    /**
     * Eliminates (sums out) a variable from a factor.
     *
     * @param factor         The input factor.
     * @param varToEliminate The Variable object to eliminate.
     * @param variableMap    Map of variable names to Variable objects.
     * @return A new Factor with the variable summed out.
     */
    private static Factor sumOut(Factor factor, Variable varToEliminate, Map<String, Variable> variableMap) {
        String varNameToEliminate = varToEliminate.getName();

        // Create a new domain excluding the variable to be eliminated
        List<Variable> newDomain = factor.getDomain().stream()
                .filter(v -> !v.getName().equals(varNameToEliminate))
                .collect(Collectors.toList());
        // Ensure consistent order, like we kept in the join operation
        newDomain.sort(Comparator.comparing(Variable::getName));

        Map<Map<String, String>, Double> newValues = new HashMap<>();

        // Group entries by assignment *excluding* the variable to be eliminated
        Map<Map<String, String>, List<Double>> groupedProbabilities = new HashMap<>();

        for (Map.Entry<Map<String, String>, Double> entry : factor.getValues().entrySet()) {
            Map<String, String> originalAssignment = entry.getKey();
            Double probability = entry.getValue();

            // Create the assignment key for the new factor (without the eliminated variable)
            Map<String, String> newAssignmentKey = new HashMap<>();
            for (Variable v : newDomain) {
                newAssignmentKey.put(v.getName(), originalAssignment.get(v.getName()));
            }
            // Make the key immutable for map usage
            // This map contain all the variables that are not the variable we want to eliminate, and their values
            Map<String, String> immutableKey = Collections.unmodifiableMap(newAssignmentKey);

            // Add the probability to the list for this group
            groupedProbabilities.computeIfAbsent(immutableKey, k -> new ArrayList<>()).add(probability);
        }

        // Calculate the summed probability for each group
        for (Map.Entry<Map<String, String>, List<Double>> groupEntry : groupedProbabilities.entrySet()) {
            Map<String, String> assignment = groupEntry.getKey();
            List<Double> probabilitiesToSum = groupEntry.getValue();

            double sum = 0.0;
            int additionsForThisGroup = 0;
            for (double p : probabilitiesToSum) {
                sum += p;
                if (additionsForThisGroup > 0) { // Count additions after the first value
                    _numberOfAdditions++;
                }
                additionsForThisGroup++;
            }
            newValues.put(assignment, sum);
        }

        return new Factor(newDomain, newValues);
    }

    /**
     * Normalizes the probabilities in a factor so they sum to 1.
     *
     * @param factor The factor to normalize.
     * @return A new Factor with normalized probabilities.
     */
    private static Factor normalizeFactor(Factor factor) {
        double totalProbability = 0.0;
        int additionsForSum = 0;

        // Calculate the sum of all probabilities in the factor
        for (double prob : factor.getValues().values()) {
            totalProbability += prob;
            if (additionsForSum > 0) { // Count additions after the first value
                _numberOfAdditions++;
            }
            additionsForSum++;
        }
        //This map Will store the normalized values
        Map<Map<String, String>, Double> normalizedValues = new HashMap<>();

        // Avoid division by zero
        if (Math.abs(totalProbability) < 1e-9) {
            System.err.println("Warning: Total probability is zero during normalization. Returning factor with original values.");
            return new Factor(factor.getDomain(), factor.getValues()); // Return original (likely all zeros)
        }

        // Divide each probability by the total sum
        for (Map.Entry<Map<String, String>, Double> entry : factor.getValues().entrySet()) {
            normalizedValues.put(entry.getKey(), entry.getValue() / totalProbability);
        }

        return new Factor(factor.getDomain(), normalizedValues);
    }

    /**
     * Helper to generate all possible assignments for a given list of variables (domain).
     * Example: Variables A={T,F}, B={X,Y} -> [{A=T,B=X}, {A=T,B=Y}, {A=F,B=X}, {A=F,B=Y}]
     *
     * @param domain List of Variable objects.
     * @return A list of maps, where each map represents a combination for those domains.
     */
    private static List<Map<String, String>> generateAssignments(List<Variable> domain, Map<String, String> evidenceAssignments) {
        List<Map<String, String>> assignments = new ArrayList<>();
        if (domain == null) {
            return assignments;
        }

        generateAssignmentsRecursive(domain, 0, new HashMap<>(), assignments, evidenceAssignments);
        return assignments;
    }

    /**
     * Recursive helper method to generate all possible assignments for a given list of variables (domain).
     *
     * @param domain            List of Variable objects.
     * @param varIndex          Current index in the domain list.
     * @param currentAssignment Current assignment being built.
     * @param allAssignments    List to store all generated assignments.

     * Recursive construction of all possibilities for specific variables:
     * currentAssignment - represents a specific combination for the given variables.
     * allAssignments - represents the list of combinations for the given variables.
     */
    private static void generateAssignmentsRecursive(List<Variable> domain, int varIndex,
                                                     Map<String, String> currentAssignment,
                                                     List<Map<String, String>> allAssignments,
                                                     Map<String, String> evidenceAssignments) {
        // Base case: if all variables have been assigned, add the current combination to the list
        if (varIndex == domain.size()) {
            allAssignments.add(new HashMap<>(currentAssignment));
            return;
        }

        Variable currentVar = domain.get(varIndex);
        String currentVarName = currentVar.getName();

        // Check if the current variable is an evidence variable. It has a fixed value.
        if (evidenceAssignments.containsKey(currentVarName)) {
            String fixedValue = evidenceAssignments.get(currentVarName);
            if (currentVar.getOutcomes().contains(fixedValue)) { // Check if the fixed value is valid for the current variable
                currentAssignment.put(currentVarName, fixedValue);
                generateAssignmentsRecursive(domain, varIndex + 1, currentAssignment, allAssignments, evidenceAssignments);
                currentAssignment.remove(currentVarName); // Backtrack
            } else {
                throw new IllegalArgumentException("Error: Evidence value '" + fixedValue + "' for variable '" + currentVarName + "' is not among its possible outcomes: " + currentVar.getOutcomes());
            }
        } else {
            // If the variable is not an evidence variable, generate all possible outcomes
            if (currentVar.getOutcomes() == null || currentVar.getOutcomes().isEmpty()) {
                throw new IllegalStateException("Error: Variable '" + currentVar.getName() + "' has no outcomes defined. Cannot generate assignments.");
            }
            // Iterate through all possible outcomes for the current variable
            for (String outcome : currentVar.getOutcomes()) {
                currentAssignment.put(currentVarName, outcome);
                generateAssignmentsRecursive(domain, varIndex + 1, currentAssignment, allAssignments, evidenceAssignments);
                currentAssignment.remove(currentVarName); // Backtrack
            }
        }
    }

}