import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a variable in a Bayesian network.
 * Each variable has a name and a list of possible outcomes.
 */
public class Variable {
    private final String _name;
    private final List<String> _outcomes;

    public Variable(String name, List<String> outcomes) {
        _name = Objects.requireNonNull(name, "Name cannot be null");

        Objects.requireNonNull(outcomes, "Outcomes list cannot be null");
        List<String> outcomesCopy = new ArrayList<>(outcomes);
        this._outcomes = Collections.unmodifiableList(outcomesCopy);
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
