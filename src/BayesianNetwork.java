import java.util.List;
import java.util.Objects;

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
        _definitions = List.copyOf(Objects.requireNonNull(definitions));
        _variables = List.copyOf(Objects.requireNonNull(variables));
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

    public String getCPT(){
        StringBuilder sb = new StringBuilder();
        for (Definition definition : _definitions) {
            sb.append(definition.toString());
        }
        return sb.toString();
    }

}
