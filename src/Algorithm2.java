import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Algorithm2 {

    public static int _numberOfMultiplications = 0;
    public static int _numberOfAdditions = 0;

    public static int get_numberOfAdditions() {return _numberOfAdditions;}
    public static int get_numberOfMultiplications() {return _numberOfMultiplications;}

    public void reset() {
        _numberOfAdditions = 0;
        _numberOfMultiplications = 0;
    }


    //TODO: 1. Understand which variables are not relevant to the query (as written on page 91)
    //TODO: 2. Prepare the factors from the Maps that are passed to the function
    //TODO: 3. Making a loop that goes through all the hidden variables.
    //TODO: 4. Gathering all the factors that have the hidden variable in them.
    //TODO: 5. Doing a join on them. And a new factor is created.
    //TODO: 6. The hidden variable is hidden in the last factor that remains.
    //TODO: 7. Finally, normalization is done for the requested query variable.

    public static String calculateProbability(Map<String, String> requestedQueryAssignment,
                                              Map<String, List<ProbabilityEntry>> queryMap,
                                              Map<String, List<ProbabilityEntry>> evidenceMap,
                                              Map<String, List<ProbabilityEntry>> hiddenMap,
                                              BayesianNetwork network) throws IOException {

        // Map to store the evidence assignments
        // the map look like {varName, outcome}
        Map<String, String> evidenceAssignments = new HashMap<>();
        for(Map.Entry<String, List<ProbabilityEntry>> evEntry : evidenceMap.entrySet()){
            if (!evEntry.getValue().isEmpty()){
                //System.out.println(evEntry+ ": " + evEntry.getValue().getFirst().getOutcome());// print evidence and his outcome
                evidenceAssignments.put(evEntry.getKey(), evEntry.getValue().getFirst().getOutcome());
            } else {
                System.err.println("Warning: Evidence variable " + evEntry.getKey() + " has empty entry list in evidenceMap. Cannot apply evidence.");
            }
        }

        // There some variables that are not relevant
        // Variable is relevant to the query:
        //                                        1. The variable query
        //                                        2. The variable evidence
        //                                        3. The variable is ancestor of the query variable or evidence variable
        // if variable is not answer of one of the above, then I won't build for him a factor

        Path OutPutFactors = Paths.get("Factors.txt");
        StringBuilder Factors = new StringBuilder();
        List<Factor> factors = new ArrayList<>();
        for (Definition definition : network.getDefinitions()) {
            if(requestedQueryAssignment.containsKey(definition.getName()) || evidenceMap.containsKey(definition.getName())) {
                //System.out.println("Found Variable: " + definition.getName() + " he relevant so I will build a factor for him");
                factors.add(new Factor(definition, network));
                Factors.append("Initial Factor for ").append(definition.getName()).append(":\n").append(factors.getLast()).append("\n");
            }else {
                boolean isAncestor = false;
                for(String targetVarName : requestedQueryAssignment.keySet()) {
                    List<String> ancestorName = network.getAncestors(targetVarName);
                    for(String ancestorVarName : ancestorName) {
                        if (ancestorVarName.equals(definition.getName())) {
                            isAncestor = true;
                            break;
                        }
                    }
                }
                for(String targetVarName : evidenceMap.keySet()) {
                    List<String> ancestorName = network.getAncestors(targetVarName);
                    for(String ancestorVarName : ancestorName) {
                        if (ancestorVarName.equals(definition.getName())) {
                            isAncestor = true;
                            break;
                        }
                    }
                }
                if (isAncestor) {
                    System.out.println("Found Variable: " + definition.getName() + " he relevant so I will build a factor for him");
                    factors.add(new Factor(definition, network));
                    Factors.append("Initial Factor for ").append(definition.getName()).append(":\n").append(factors.getLast()).append("\n");
                } else {
                    Factors.append("Found Variable: ").append(definition.getName()).append(" he not relevant so I won't build a factor for him\n");
                }
            }
        }
        Files.writeString(OutPutFactors, Factors, StandardCharsets.UTF_8);

        List<Factor> restrictedFactors = getFactors(factors, evidenceAssignments);
        /**Print to file the correct factor, after district*/
        Path OutPutFactorsRestricted = Paths.get("FactorsRestricted" + System.currentTimeMillis()+ ".txt");
        StringBuilder FactorsRestricted = new StringBuilder();
        factors = restrictedFactors;
        for(Factor factor : factors){
            FactorsRestricted.append(factor.toString()).append("\n");
        }
        Files.writeString(OutPutFactorsRestricted, FactorsRestricted, StandardCharsets.UTF_8);

        // Step 4: Join the factors

        //Factor joinedFactor = joinFactors(factors);

        // Step 6: Normalize the final factor
        //String result = normalize(finalFactor);

        return "Algorithm in progress, the correct result will be returned soon.";
    }

    /**
     * This method restricts the factors based on the evidence assignments.
     * It iterates through each factor and applies the evidence restrictions.
     *
     * @param factors             The list of factors to restrict.
     * @param evidenceAssignments The map of evidence assignments.
     * @return A list of factors after restrict appropriate line.
     */
    private static List<Factor> getFactors(List<Factor> factors, Map<String, String> evidenceAssignments) {
        List<Factor> restrictedFactors = new ArrayList<>();
        for (Factor factor : factors) {
            Factor currentFactor = factor;

            for (Map.Entry<String, String> evidenceEntry : evidenceAssignments.entrySet()) {
                String evidenceVarName = evidenceEntry.getKey();// name of the variable
                String evidenceValue = evidenceEntry.getValue();// outcome of the variable

                currentFactor = currentFactor.restrict(evidenceVarName, evidenceValue);// try to restrict lines from the factor
            }
            restrictedFactors.add(currentFactor);
//            System.out.println("Factor after applying evidence (Original domain: " +
//                    factor.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) +
//                    "):\n" + currentFactor);
        }
        return restrictedFactors;
    }

}
