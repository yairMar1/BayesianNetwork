import java.util.Arrays;

public class SimplestQuery {
    public static String calculateJointProbability(BayesianNetwork network, String query) {
        // Remove the symbols: P ( )
        query = query.replace("P(", "").replace(")", "");
        String answer = "";

        // Split the query into variable-value pairs
        String[] variableValuePairs = query.split(",");
        System.out.println("Variable-Value Pairs: " + Arrays.toString(variableValuePairs));
        final double[] jointProbability = {1.0};

        // Iterate through each variable-value pair
        for (int i = 0; i < variableValuePairs.length; i++) {
            String pair = variableValuePairs[i].trim();
            // Split the pair into variable and value
            String[] parts = pair.split("=");
            //System.out.println(Arrays.toString(parts));
            if (parts.length != 2) {
                System.err.println("Invalid variable-value pair: " + pair);
                return "Invalid input format";
            }
            String variable = parts[0].trim(); // Variable name
            String value = parts[1].trim(); // Value of the variable

            // Get the conditional probability table for the variable
            if(network.getVariablesString().contains("name='" + variable + '\'' )) {
                System.out.println("I got Variable: " + variable + ", Value: " + value);

                // Extracting the variable from the network
                // If it has no parents, we will take the probability of that variable with its value.
                // If it has parents, we will consider the value of its parents and their value

                network.getDefinitions().forEach(definition -> {
                    if (definition.getName().equals(variable)) {
                        // Check if the variable has parents
                        if (definition.getParents().isEmpty()) {
                            System.out.println("Variable has no parents");
                            // No parents, use the probability directly
                            for (ProbabilityEntry entry : definition.getProbabilityList()) {
                                if (entry.getOutcome().equals(value)) {
                                    jointProbability[0] *= entry.getProbability();
                                    System.out.println("Joint Probability: " + jointProbability[0]);
                                }
                            }
                        } else {
                            // Variable has parents, we need to find the corresponding entry in the CPT
                            System.out.println("Variable has parents");
                            // Extracting the value of the variable's parents from the query
                            // we multiply the joint probability by the probability of the variable given its parents
                            for (ProbabilityEntry entry : definition.getProbabilityList()) {
                                if(entry.getOutcome().equals(value)) { // Check if the outcome matches to the value of the variable
                                    // Check if the entry's parent status matches the query
                                    boolean match = true;
                                    for (String parent : definition.getParents()) {
                                        String parentValue = Arrays.stream(variableValuePairs)
                                                .filter(p -> p.startsWith(parent + "="))
                                                .map(p -> p.split("=")[1].trim())
                                                .findFirst()
                                                .orElse(null);
                                        if (parentValue == null || !entry.getStatusParent().get(parent).equals(parentValue)) {
                                            match = false;
                                            break;
                                        }
                                    }
                                    if (match) {
                                        jointProbability[0] *= entry.getProbability();
                                        System.out.println("Joint Probability with Parents: " + jointProbability[0]);
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
        // enter the joint probability to answer, number of add action, number of multiplying
        answer += String.format("%.5f", jointProbability[0]);
        answer += ",0," + (variableValuePairs.length-1);
        return answer;
    }
}
