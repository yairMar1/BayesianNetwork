import java.util.*;
import java.util.stream.Collectors;

/**
 * Factor class represents a factor in a Bayesian network.
 * It contains a domain of variables and their corresponding probability values.
 */

public class Factor {

    private final List<Variable> _domain;
    private final Map<Map<String, String>, Double> _values;

    // This constructor is used to create a Factor from a Definition object and a BayesianNetwork object.
    // It initializes the domain of the factor and populates the values based on the definition.
    // this constructor is used when creating factors at the beginning of algorithm 2.
    public Factor(Definition definition, BayesianNetwork network) {

        // Adding the Variable of the factor, itself to the domain
        List<Variable> domain = new ArrayList<>();
        Variable selfVariable = null;
        for(Variable var : network.getVariables()){
            if(var.getName().equals(definition.getName())){
                selfVariable = var;
                break;
            }
        }

        domain.add(selfVariable);

        // Map {varName , Variable}
        Map<String, Variable> variableMap = network.getVariables().stream()
                .collect(Collectors.toMap(Variable::getName, v -> v));

        // find the parents of the variable
        for (String parentName : definition.getParents()) {
            Variable parentVar = variableMap.get(parentName);
            domain.add(parentVar);
        }
        _domain = Collections.unmodifiableList(domain);


        Map<Map<String, String>, Double> values = new HashMap<>();
        for (ProbabilityEntry entry : definition.getProbabilityList()) {
            // Adding the parent status and the variable and his outcome to the map
            Map<String, String> assignmentForDomain = new HashMap<>(entry.getStatusParent());
            assignmentForDomain.put(definition.getName(), entry.getOutcome());

            values.put(Collections.unmodifiableMap(assignmentForDomain), entry.getProbability());
        }
        _values = Collections.unmodifiableMap(values);
    }

    // This constructor is used to create a Factor with a specific domain and values.
    Factor(List<Variable> domain, Map<Map<String, String>, Double> values) {
        _domain = List.copyOf(Objects.requireNonNull(domain));
        // Ensure keys in values map are immutable if they aren't already
        Map<Map<String, String>, Double> immutableValues = new HashMap<>();
        for (Map.Entry<Map<String, String>, Double> entry : values.entrySet()) {
            immutableValues.put(Collections.unmodifiableMap(new HashMap<>(entry.getKey())), entry.getValue());
        }
        _values = Collections.unmodifiableMap(immutableValues);
    }

    public List<Variable> getDomain() {return _domain;}
    public Map<Map<String, String>, Double> getValues() {return _values;}

    // This method retrieves the probability value for a given assignment of variable values.
    // We get a map with values, and we go over the values of the domain of our factor. And we return the desired value.
    // In many cases, not all the values in the new factor match the values in the old factor,
    // so we go over the values in the old factor and try to extract the desired probability from them
    public double getValue(Map<String, String> assignment) {
        // The map will contain the vars (and their value) we want to know the probability of
        Map<String, String> assignmentForDomain = new HashMap<>();
        for(Variable var : _domain) {
            String assignedValue = assignment.get(var.getName());
            if (assignedValue == null) {
                throw new IllegalArgumentException("Assignment is incomplete for factor domain in getValue. Missing: " + var.getName() + " in assignment " + assignment);
            }
            if (!var.getOutcomes().contains(assignedValue)) {
                throw new IllegalArgumentException("Warning: Invalid value '" + assignedValue + "' requested for variable '" + var.getName() + "'. Expected one of: " + var.getOutcomes());
            }
            assignmentForDomain.put(var.getName(), assignedValue);
        }
        Double value = _values.get(assignmentForDomain);

        if (value == null) {
            System.out.println("DEBUG: getValue returning 0.0 for missing key " + assignmentForDomain + " in factor over " + _domain.stream().map(Variable::getName).collect(Collectors.joining(",")));
            return 0.0;
        }
        return value;
    }

    public Factor restrict(String evidenceVariable, String evidenceValue) {
        Objects.requireNonNull(evidenceVariable, "Evidence variable name cannot be null");
        Objects.requireNonNull(evidenceValue, "Evidence value cannot be null");

        // Check if the evidence variable is actually in this factor's domain
        boolean variableInDomain = false;
        for (Variable var : _domain) {
            if (var.getName().equals(evidenceVariable)) {
                variableInDomain = true;
                if (!var.getOutcomes().contains(evidenceValue)) {
                    System.err.println("Warning: Evidence value '" + evidenceValue + "' is not a valid outcome for variable '" + evidenceVariable + "'. Restriction might result in an empty factor.");
                }
                break;
            }
        }

        // If the variable is not in the domain, the evidence doesn't affect this factor
        if (!variableInDomain) {
            return this; // The original factor unchanged
        }

        // Filter the values map to keep only rows matching the evidence
        Map<Map<String, String>, Double> restrictedValues = new HashMap<>();
        for (Map.Entry<Map<String, String>, Double> entry : _values.entrySet()) {
            Map<String, String> assignment = entry.getKey();
            // Check if the assignment for the evidence variable matches the observed value
            if (evidenceValue.equals(assignment.get(evidenceVariable))) {
                // Keep this row (assignment and its probability)
                restrictedValues.put(assignment, entry.getValue());
            }
        }
        // Create and return a new Factor with the same domain but restricted values
        // Using the private constructor that accepts the final domain and values
        return new Factor(_domain, restrictedValues);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Factor [Domain: ");
        if (_domain == null || _domain.isEmpty()) {
            sb.append("None");
        } else {
            List<Variable> sortedDomain = new ArrayList<>(_domain);
            sortedDomain.sort(Comparator.comparing(Variable::getName));
            sb.append(sortedDomain.stream().map(Variable::getName).collect(Collectors.joining(", ")));
        }
        sb.append("]\n");

        if (_values == null || _values.isEmpty()) {
            sb.append("  (Empty Factor)\n");
            return sb.toString();
        }

        if(_domain == null || _domain.isEmpty()) {throw new IllegalArgumentException("Empty Factor");}

        List<Variable> domainVariablesSorted = new ArrayList<>(_domain);
        domainVariablesSorted.sort(Comparator.comparing(Variable::getName));

        List<String> columnHeaders = domainVariablesSorted.stream()
                .map(Variable::getName)
                .collect(Collectors.toList());
        columnHeaders.add("Probability");

        Map<String, Integer> columnWidths = new HashMap<>();

        for (String header : columnHeaders) {
            columnWidths.put(header, header.length());
        }

        for (Map<String, String> assignmentKey : _values.keySet()) {
            for (Variable var : domainVariablesSorted) {
                String varName = var.getName();
                String assignedValue = assignmentKey.get(varName);
                if (assignedValue != null) {
                    columnWidths.put(varName, Math.max(columnWidths.get(varName), assignedValue.length()));
                }
            }
            double probabilityValue = _values.get(assignmentKey);
            String formattedProb = String.format("%.5f", probabilityValue);
            columnWidths.put("Probability", Math.max(columnWidths.get("Probability"), formattedProb.length()));
        }

        for (String header : columnHeaders) {
            int width = columnWidths.get(header);
            sb.append(String.format("%-" + (width + 2) + "s", header));
        }
        sb.append("\n");
        for (String header : columnHeaders) {
            int width = columnWidths.get(header);
            sb.append("-".repeat(Math.max(0, width + 2)));
        }
        sb.append("\n");

        List<Map.Entry<Map<String, String>, Double>> sortedValues = new ArrayList<>(_values.entrySet());
        sortedValues.sort((entry1, entry2) -> {
            Map<String, String> assign1 = entry1.getKey();
            Map<String, String> assign2 = entry2.getKey();

            for(Variable var : domainVariablesSorted) {
                String varName = var.getName();
                String val1 = assign1.get(varName);
                String val2 = assign2.get(varName);

                int cmp = 0;
                if ("T".equals(val1) && "F".equals(val2)) {
                    cmp = -1; // T קודם ל F
                } else if ("F".equals(val1) && "T".equals(val2)) {
                    cmp = 1;  // F אחרי T
                } else {
                    if (val1 == null && val2 == null) continue;
                    if (val1 == null) return -1;
                    if (val2 == null) return 1;
                    cmp = val1.compareTo(val2);
                }

                if (cmp != 0) return cmp;
            }
            return 0;
        });

        for (Map.Entry<Map<String, String>, Double> entry : sortedValues) {
            Map<String, String> assignment = entry.getKey();
            double probabilityValue = entry.getValue();
            String formattedProb = String.format("%.5f", probabilityValue);

            for (Variable var : domainVariablesSorted) {
                String varName = var.getName();
                String assignedValue = assignment.get(varName);
                int width = columnWidths.get(varName);
                sb.append(String.format("%-" + (width + 2) + "s", assignedValue));
            }

            int probWidth = columnWidths.get("Probability");
            sb.append(String.format("%-" + (probWidth + 2) + "s", formattedProb));

            sb.append("\n");
        }

        return sb.toString();
    }

}
