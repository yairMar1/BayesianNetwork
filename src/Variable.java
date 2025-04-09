import java.util.List;
/**
 * Represents a variable in a Bayesian network.
 * Each variable has a name and a list of possible outcomes.
 */
public class Variable {
    private final String _name;
    private final List<String> _outcomes;

    public Variable(String name, List<String> outcomes) {
        _name = name;
        _outcomes = outcomes;
    }

    public String getName() {return _name;}

    //public void setName(String name) {this.name = name;}

    public List<String> getOutcomes() {return _outcomes;}

    @Override
    public String toString() {
        return "Variable{" +
                "name='" + _name + '\'' +
                ", outcomes=" + _outcomes +
                '}';
    }
}
