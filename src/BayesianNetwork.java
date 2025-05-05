import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a Bayesian network:
 * Network name, variables and their CPT table
 */

public class BayesianNetwork {
    private final String _name;
    private final List<Definition> _definitions;
    private final List<Variable> _variables;

    public BayesianNetwork(String name, List<Definition> definitions, List<Variable> variables) {
        _name = Objects.requireNonNull(name,"Name cannot be null");
        Objects.requireNonNull(definitions, "Definitions list cannot be null");
        Objects.requireNonNull(variables, "Variables list cannot be null");
        List<Definition> definitionsCopy = new ArrayList<>(definitions);
        List<Variable> variablesCopy = new ArrayList<>(variables);

        _definitions = Collections.unmodifiableList(definitionsCopy);
        _variables = Collections.unmodifiableList(variablesCopy);
    }

    public String getName() {return _name;}

    public List<Definition> getDefinitions() {return _definitions;}
    public String getDefinitionsString() {
        StringBuilder sb = new StringBuilder();
        for (Definition definition : _definitions) {
            sb.append(definition.toString());
        }
        return sb.toString();
    }

    public List<Variable> getVariables() {return _variables;}
    public String getVariablesString() {
        StringBuilder sb = new StringBuilder();
        for (Variable var : _variables) {
            sb.append(var.toString()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bayesian Network: ").append(getName()).append("\n");
        sb.append("Definitions:\n").append(getDefinitionsString());
        sb.append("Variables:\n").append(getVariablesString());
        return sb.toString();
    }

    /**
     * This method returns the ancestors of a variable in the Bayesian network.
     * It uses BFS to find all ancestors of the given variable.
     *
     * @param name The name of the variable whose ancestors we want to find.
     * @return A list of ancestor variable names.
     */
    public List<String> getAncestors(String name) {
        // Map {variableName , Variable(the Variable object)}
        Map<String, Variable> variableMap = this.getVariables().stream()
                .collect(Collectors.toMap(Variable::getName, v -> v));

        Variable startVariable = variableMap.get(name);// The variable we want to find its ancestors

        if (startVariable == null) {
            return Collections.emptyList();
        }

        Set<Variable> ancestors = new HashSet<>(); // to store the ancestors
        Set<Variable> visited = new HashSet<>();   // To prevent infinite loop
        Queue<Variable> queue = new LinkedList<>();// for BFS

        queue.offer(startVariable); // start with the variable we want to find its ancestors
        visited.add(startVariable);  // mark it as visited

        while (!queue.isEmpty()) {
            Variable currentNode = queue.poll(); // pop the first element from the queue
            ancestors.add(currentNode); // add it to the ancestors set. In another implementation, you can also not add it.

            // Store the current node as definition to get its parents
            Definition currentDefinition = null;
            for (Definition def : this.getDefinitions()) {
                if (def.getName().equals(currentNode.getName())) {
                    currentDefinition = def;
                    break;
                }
            }

            if (currentDefinition != null) {
                // go through all the parents of the current node
                for (String parentName : currentDefinition.getParents()) {
                    Variable parentVariable = variableMap.get(parentName);
                    // check if the parent variable is not null and not visited
                    if (parentVariable != null && visited.add(parentVariable)) {
                        queue.offer(parentVariable);// add the parent to the queue
                    }
                }
            }
        }

        return ancestors.stream()
                .map(Variable::getName)
                .collect(Collectors.toList()); // Convert to a list of names
    }

}
