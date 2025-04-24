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
    // We use this constructor when we want to create a factor with a specific domain and values.
    private Factor(List<Variable> domain, Map<Map<String, String>, Double> values) {
        _domain = List.copyOf(domain);
        _values = Map.copyOf(values);
    }

    public List<Variable> getDomain() {return _domain;}
    public Map<Map<String, String>, Double> getValues() {return _values;}

    public double getValue(Map<String, String> assignment) {
        Map<String, String> assignmentForDomain = new HashMap<>();
        for(Variable var : _domain) {
            String assignedValue = assignment.get(var.getName());
            if (assignedValue == null) {// if the variable is not in the input assignment
                throw new IllegalArgumentException("Assignment is incomplete for factor domain in getValue. Missing: " + var.getName());
            }
            assignmentForDomain.put(var.getName(), assignedValue);
        }

        Double value = _values.get(Collections.unmodifiableMap(assignmentForDomain));
        if (value == null) {// if the combination is not found in the factor
            throw new IllegalStateException("Value not found for complete domain assignment in Factor.getValue. Assignment: " + assignmentForDomain);
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
