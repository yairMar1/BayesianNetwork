import java.util.ArrayList;
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
                                              BayesianNetwork network) {

        List<Factor> factors = new ArrayList<>();

        for (Definition definition : network.getDefinitions()) {
            Factor factor = new Factor(definition, network);
            System.out.println(factor);
        }

        // Step 1: Identify the relevant line at factors

        // Step 4: Join the factors
        //Factor joinedFactor = joinFactors(factors);

        // Step 6: Normalize the final factor
        //String result = normalize(finalFactor);

        return "Algorithm in progress, the correct result will be returned soon.";
    }

















}
