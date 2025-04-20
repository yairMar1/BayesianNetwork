import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents a probability entry in a probability table for (one) variable.
 * It contains a map of parents status, and the probability of the outcome given that status.
 */
public class ProbabilityEntry {

    private final double _probability;

    private final String _outcome;

    private final Map<String, String> _statusParent;

    public ProbabilityEntry(Map<String, String> statusParent, double probability, String outcome) {
        if (probability < 0 || probability > 1) {
            throw new IllegalArgumentException("Probability must be between 0 and 1");
        }
        _statusParent = new HashMap<>(statusParent); // Create a copy of the map to avoid external modification
        _probability = probability;
        _outcome = outcome;
    }

    public double getProbability() {
        return _probability;
    }

    public Map<String, String> getStatusParent() {
        return _statusParent;
    }

    public String getStatusParents() {
        if (_statusParent.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : _statusParent.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
        }
        if (!sb.isEmpty()) {
            sb.setLength(sb.length() - 2); // Remove the last comma and space
        }
        return sb.toString();
    }

    public String getOutcome() {
        return _outcome;
    }

    @Override
    public String toString() {
        String parentInfo = getStatusParents();
        if (parentInfo.isEmpty()) {
            return ("P(" + getOutcome() + ") = " + getProbability());
        } else {
            return ("P(" + getOutcome() + " | " + parentInfo + ") = " + getProbability());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProbabilityEntry that = (ProbabilityEntry) o;
        return Double.compare(that._probability, _probability) == 0 &&
                Objects.equals(_outcome, that._outcome) && // Use Objects.equals for null safety
                Objects.equals(_statusParent, that._statusParent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_probability, _outcome, _statusParent);
    }
}