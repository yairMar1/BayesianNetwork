import java.util.List;
import java.util.Objects;

/**
 * This class creates a "definition" object:
 * What is the name of the object, and the object's parents (if any),
 * and its probability table (taking into account its parents)
 */

public class Definition {
    private final String _name;
    private final List<String> _parents;
    private final List<ProbabilityEntry> _probabilityList;

    public Definition(String name, List<String> parents, List<ProbabilityEntry> ProbabilityList) {
        _name = Objects.requireNonNull(name, "Name cannot be null");
        _parents = List.copyOf(Objects.requireNonNull(parents, "Parents List cannot be null, it should be empty"));
        _probabilityList = List.copyOf(Objects.requireNonNull(ProbabilityList, "ProbabilityList cannot be null"));
    }

    public String getName() {return _name;}

    public List<String> getParents() {return _parents;}

    public List<ProbabilityEntry> getProbabilityList() {return _probabilityList;}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- CPT for Variable: ").append(getName()).append(" ---\n");

        if (!_parents.isEmpty()) {
            sb.append("Parents: [").append(String.join(", ", _parents)).append("]\n");
        } else {
            sb.append("Parents: None\n");
        }

        sb.append("Probability Entries:\n");
        if (_probabilityList.isEmpty()) {
            sb.append("  (No entries)\n");
        } else {
            for (ProbabilityEntry entry : _probabilityList) {
                sb.append("  ").append(entry.toString()).append("\n");
            }
        }
        sb.append("--- End CPT for ").append(getName()).append(" ---\n");
        return sb.toString();
    }

}