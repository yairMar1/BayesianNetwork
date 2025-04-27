import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Algorithm2 {

    // Static counters for operations
    private static int _numberOfMultiplications = 0;
    private static int _numberOfAdditions = 0;

    // Getters for counters
    public static int get_numberOfAdditions() {
        return _numberOfAdditions;
    }

    public static int get_numberOfMultiplications() {
        return _numberOfMultiplications;
    }

    // Reset counters before each query calculation
    private static void resetCounters() {
        _numberOfAdditions = 0;
        _numberOfMultiplications = 0;
    }

    public static String calculateProbability(Map<String, String> requestedQueryAssignment,
                                              Map<String, List<ProbabilityEntry>> queryMap, // Contains the query variable(s)
                                              Map<String, List<ProbabilityEntry>> evidenceMap, // Contains evidence variables and their observed values
                                              Map<String, List<ProbabilityEntry>> hiddenMap, // Contains hidden variables
                                              BayesianNetwork network) throws IOException {

        resetCounters(); // Reset counters for this specific query

        // --- שלבים 1-2 --- (זהים)
        // ... קבלת שם משתנה השאילתה, יצירת variableMap ...
        String queryVariableName = queryMap.keySet().iterator().next();
        Map<String, Variable> variableMap = network.getVariables().stream()
                .collect(Collectors.toMap(Variable::getName, v -> v));

        // יצירת מפת העדויות - חשוב שתהיה זמינה לאורך כל הדרך
        final Map<String, String> evidenceAssignments = evidenceMap.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getFirst().getOutcome()));

        // זיהוי משתנים רלוונטיים (זהה)
        Set<String> relevantVarNames = new HashSet<>();
        relevantVarNames.addAll(queryMap.keySet());
        relevantVarNames.addAll(evidenceMap.keySet()); // Use evidenceMap keys which is same as evidenceAssignments keys
        Set<String> ancestorsToCheck = new HashSet<>(relevantVarNames);
        for(String varName : ancestorsToCheck) {
            List<String> ancestors = network.getAncestors(varName);
            relevantVarNames.addAll(ancestors);
        }
        relevantVarNames.addAll(queryMap.keySet());
        relevantVarNames.addAll(evidenceMap.keySet());

        // יצירת פקטורים ראשוניים (זהה)
        List<Factor> initialFactors = new ArrayList<>();
        for (Definition definition : network.getDefinitions()) {
            if (relevantVarNames.contains(definition.getName())) {
                initialFactors.add(new Factor(definition, network));
            }
        }

        List<Factor> factors;
        // --- 3. החלת עדות (Restriction) ---
        List<Factor> restrictedFactors = new ArrayList<>(); // רשימה חדשה לפקטורים שעברו restriction
        for (Factor factor : initialFactors) { // initialFactors היא הרשימה המקורית של פקטורים רלוונטיים
            Factor currentFactor = factor;
            for (Map.Entry<String, String> evidenceEntry : evidenceAssignments.entrySet()) {
                currentFactor = currentFactor.restrict(evidenceEntry.getKey(), evidenceEntry.getValue());
                if (currentFactor == null || currentFactor.getValues().isEmpty()) {
                    currentFactor = null;
                    break;
                }
            }
            if (currentFactor != null) {
                restrictedFactors.add(currentFactor); // הוסף לרשימה החדשה
            } else {
                System.out.println("Factor for " + factor.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + " became empty after restriction.");
            }
        }
        // כעת restrictedFactors מכילה את כל הפקטורים הרלוונטיים אחרי החלת העדות


        // --- 3.5 הסרת פקטורים המכילים רק משתני עדות ---
        List<Factor> factorsForVE = new ArrayList<>(); // רשימה חדשה עבור לולאת VE
        Set<String> evidenceVarNames = evidenceAssignments.keySet();
        System.out.println("Filtering factors post-restriction. Evidence vars: " + evidenceVarNames);
        for(Factor f : restrictedFactors) { // עבוד על הרשימה שעברה restriction
            Set<String> factorDomainNames = f.getDomain().stream()
                    .map(Variable::getName)
                    .collect(Collectors.toSet());

            // בדוק אם כל המשתנים בדומיין של הפקטור הם חלק מהעדות
            if (evidenceVarNames.containsAll(factorDomainNames)) {
                // אם כן, הפקטור הזה מייצג P(subset_of_evidence) או P(evidence_val)
                // והוא מיותר להמשך החישוב של P(Query|Evidence)
                System.out.println("Removing factor post-restriction as its domain only contains evidence: [" + String.join(",", factorDomainNames) + "]");
                // אל תוסיף את הפקטור הזה לרשימה עבור VE
            } else {
                factorsForVE.add(f); // שמור את הפקטור הזה עבור לולאת VE
            }
        }
        // השתמש ברשימה המסוננת factorsForVE להמשך
        factors = factorsForVE; // עדכן את המשתנה factors הראשי


        // --- 4. לולאת Variable Elimination ---
        List<String> hiddenVariableNames = hiddenMap.keySet().stream()
                .filter(relevantVarNames::contains)
                .filter(hVar -> !evidenceAssignments.containsKey(hVar)) // Don't eliminate evidence vars
                .collect(Collectors.toList());
        Collections.sort(hiddenVariableNames);
        System.out.println("Elimination Order: " + hiddenVariableNames);

        for (String hiddenVarName : hiddenVariableNames) {
            Variable hiddenVar = variableMap.get(hiddenVarName);
            if (hiddenVar == null) continue;

            System.out.println("\n--- Eliminating: " + hiddenVarName + " ---");

            List<Factor> factorsToJoin = new ArrayList<>();
            List<Factor> factorsToKeep = new ArrayList<>();
            for (Factor f : factors) {
                boolean containsHidden = f.getDomain().stream().anyMatch(v -> v.getName().equals(hiddenVarName));
                if (containsHidden) factorsToJoin.add(f);
                else factorsToKeep.add(f);
            }

            if (factorsToJoin.isEmpty()) continue;

            Factor finalJoinedFactor; // הפקטור לאחר ה-join (אם בוצע)

            if (factorsToJoin.size() == 1) {
                finalJoinedFactor = factorsToJoin.getFirst();
                System.out.println("Only one factor contains " + hiddenVarName + ". No join needed.");
            } else {
                System.out.println("Factors to join for " + hiddenVarName + ": " +
                        factorsToJoin.stream()
                                .map(f -> "[" + f.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + "] (Size: " + f.getValues().size() + ")")
                                .collect(Collectors.joining(", ")));

                // ב. ביצוע Join בזוגות לפי הכללים, תוך העברת העדות
                List<Factor> currentFactorsToJoin = new ArrayList<>(factorsToJoin);
                while (currentFactorsToJoin.size() > 1) {
                    currentFactorsToJoin.sort(Comparator
                            .<Factor, Integer>comparing(f -> f.getValues().size())
                            .thenComparing(f -> f.getDomain().stream().mapToInt(v -> v.getName().chars().sum()).sum())
                    );

                    Factor factor1 = currentFactorsToJoin.get(0);
                    Factor factor2 = currentFactorsToJoin.get(1);
                    System.out.println("Joining pair: [" + factor1.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + "] and [" + factor2.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + "]");

                    // *** שינוי: העברת evidenceAssignments ***
                    Factor joinedPairFactor = joinTwoFactors(factor1, factor2, variableMap, evidenceAssignments);

                    currentFactorsToJoin.remove(factor1);
                    currentFactorsToJoin.remove(factor2);
                    currentFactorsToJoin.add(joinedPairFactor);
                    System.out.println("Intermediate join result size: " + joinedPairFactor.getValues().size()); // This size should now be smaller!
                }
                finalJoinedFactor = currentFactorsToJoin.getFirst();
                System.out.println("Final Joined Factor for " + hiddenVarName + " (after pairwise joins):\n" + finalJoinedFactor);
            }


            // ג. ביצוע Sum out
            Factor summedOutFactor = sumOut(finalJoinedFactor, hiddenVar, variableMap); // SumOut לא צריך את העדות ישירות
            System.out.println("Factor after summing out " + hiddenVarName + ":\n" + summedOutFactor);

            // ד. עדכון רשימת הפקטורים
            factors = factorsToKeep;
            if (!summedOutFactor.getValues().isEmpty()) {
                factors.add(summedOutFactor);
            }
        }

        // --- 5. עיבוד סופי ---
        System.out.println("\n--- After Elimination ---");
        System.out.println("Remaining Factors (" + factors.size() + "):");
        factors.forEach(f -> System.out.println(f));

        Factor finalFactor;
        if (factors.isEmpty()) {
            // ... טיפול במקרה ריק ...
            Variable queryVar = variableMap.get(queryVariableName);
            Map<Map<String, String>, Double> zeroValue = Map.of(Map.of(queryVariableName, requestedQueryAssignment.get(queryVariableName)), 0.0);
            finalFactor = new Factor(List.of(queryVar), zeroValue);

        } else {
            List<Factor> remainingFactors = new ArrayList<>(factors);
            while (remainingFactors.size() > 1) {
                remainingFactors.sort(Comparator
                        .<Factor, Integer>comparing(f -> f.getValues().size())
                        .thenComparing(f -> f.getDomain().stream().mapToInt(v -> v.getName().chars().sum()).sum())
                );
                Factor f1 = remainingFactors.get(0);
                Factor f2 = remainingFactors.get(1);
                System.out.println("Final join: [" + f1.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + "] and [" + f2.getDomain().stream().map(Variable::getName).collect(Collectors.joining(",")) + "]");

                // *** שינוי: העברת evidenceAssignments ***
                Factor joined = joinTwoFactors(f1, f2, variableMap, evidenceAssignments);

                remainingFactors.remove(f1);
                remainingFactors.remove(f2);
                remainingFactors.add(joined);
            }
            finalFactor = remainingFactors.getFirst();
        }

        System.out.println("Final Factor (Pre-Normalization):\n" + finalFactor);

        // --- 6. נורמליזציה --- (ספירת החיבורים כאן נשארת זהה)
        Factor normalizedFactor = normalizeFactor(finalFactor);
        System.out.println("Normalized Final Factor:\n" + normalizedFactor);

        // --- 7. חילוץ התוצאה --- (זהה לקוד המתוקן הקודם)
        Map<String, String> finalAssignment = new HashMap<>();
        finalAssignment.putAll(requestedQueryAssignment);
        // evidenceAssignments כבר זמין
        for (Variable v : normalizedFactor.getDomain()) {
            String varName = v.getName();
            if (evidenceAssignments.containsKey(varName)) {
                finalAssignment.put(varName, evidenceAssignments.get(varName));
            } else if (!requestedQueryAssignment.containsKey(varName)) {
                throw new IllegalStateException("Final normalized factor contains unexpected variable: " + varName);
            }
        }
        System.out.println("DEBUG: Final assignment for lookup: " + finalAssignment);
        double resultProbability = 0.0;
        try {
            Set<String> finalFactorDomainNames = normalizedFactor.getDomain().stream()
                    .map(Variable::getName)
                    .collect(Collectors.toSet());
            if (!finalAssignment.keySet().equals(finalFactorDomainNames)) {
                throw new IllegalStateException("Constructed final assignment keys do not match normalized factor domain keys. AssignKeys: " + finalAssignment.keySet() + ", FactorKeys: " + finalFactorDomainNames);
            }
            resultProbability = normalizedFactor.getValue(finalAssignment); // getValue should still work
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("Error extracting final probability: " + e.getMessage());
            resultProbability = 0.0;
        }


        // --- 8. עיצוב הפלט ---
        String resultString = String.format(Locale.US, "%.5f,%d,%d",
                resultProbability,
                get_numberOfAdditions(),
                get_numberOfMultiplications());
        return resultString;
    }

    /**Helper methods*/

    // --- Helper Methods ---
    private static Factor joinTwoFactors(Factor f1, Factor f2, Map<String, Variable> variableMap, Map<String, String> evidenceAssignments) {
        Set<Variable> combinedDomainSet = new HashSet<>(f1.getDomain());
        combinedDomainSet.addAll(f2.getDomain());
        List<Variable> newDomain = new ArrayList<>(combinedDomainSet);
        newDomain.sort(Comparator.comparing(Variable::getName));

        Map<Map<String, String>, Double> newValues = new HashMap<>();

        // *** שינוי: קריאה ל-generateAssignments עם העדות ***
        List<Map<String, String>> assignmentsToConsider = generateAssignments(newDomain, evidenceAssignments);

        // חישוב ההסתברות רק להשמות הרלוונטיות (אלו שתואמות לעדות)
        for (Map<String, String> relevantAssignment : assignmentsToConsider) {
            try {
                double prob1 = f1.getValue(relevantAssignment);
                double prob2 = f2.getValue(relevantAssignment);

                // אם אחת ההסתברויות היא 0, גם התוצאה 0, אך עדיין נספור את הכפל
                // אלא אם נרצה אופטימיזציה לא לספור כפל ב-0. נשאיר את הספירה כרגע.
                double combinedProbability = prob1 * prob2;
                _numberOfMultiplications++; // ספירת כפל עבור כל שורה *רלוונטית* בפקטור החדש

                newValues.put(Collections.unmodifiableMap(new HashMap<>(relevantAssignment)), combinedProbability);

            } catch (IllegalArgumentException e) {
                System.err.println("Error during joinTwoFactors: " + e.getMessage());
                throw e;
            }
        }
        return new Factor(newDomain, newValues);
    }


    /**
     * Eliminates (sums out) a variable from a factor.
     *
     * @param factor         The input factor.
     * @param varToEliminate The Variable object to eliminate.
     * @param variableMap    Map of variable names to Variable objects.
     * @return A new Factor with the variable summed out.
     */
    private static Factor sumOut(Factor factor, Variable varToEliminate, Map<String, Variable> variableMap) {
        String varNameToEliminate = varToEliminate.getName();

        // 1. Determine the new domain (old domain minus the variable to eliminate)
        List<Variable> newDomain = factor.getDomain().stream()
                .filter(v -> !v.getName().equals(varNameToEliminate))
                .collect(Collectors.toList());
        // Ensure consistent order
        newDomain.sort(Comparator.comparing(Variable::getName));


        // 2. Group assignments and sum probabilities
        Map<Map<String, String>, Double> newValues = new HashMap<>();

        // Group entries by assignment *excluding* the variable to be eliminated
        Map<Map<String, String>, List<Double>> groupedProbabilities = new HashMap<>();

        for (Map.Entry<Map<String, String>, Double> entry : factor.getValues().entrySet()) {
            Map<String, String> originalAssignment = entry.getKey();
            Double probability = entry.getValue();

            // Create the assignment key for the new factor (without the eliminated variable)
            Map<String, String> newAssignmentKey = new HashMap<>();
            for (Variable v : newDomain) {
                newAssignmentKey.put(v.getName(), originalAssignment.get(v.getName()));
            }
            // Make the key immutable for map usage
            Map<String, String> immutableKey = Collections.unmodifiableMap(newAssignmentKey);

            // Add the probability to the list for this group
            groupedProbabilities.computeIfAbsent(immutableKey, k -> new ArrayList<>()).add(probability);
        }

        // 3. Calculate the summed probability for each group
        for (Map.Entry<Map<String, String>, List<Double>> groupEntry : groupedProbabilities.entrySet()) {
            Map<String, String> assignment = groupEntry.getKey();
            List<Double> probsToSum = groupEntry.getValue();

            double sum = 0.0;
            int additionsForThisGroup = 0;
            for (double p : probsToSum) {
                sum += p;
                if (additionsForThisGroup > 0) { // Count additions after the first value
                    _numberOfAdditions++;
                }
                additionsForThisGroup++;
            }
            newValues.put(assignment, sum); // Store the summed probability
        }


        // Use the private Factor constructor
        return new Factor(newDomain, newValues);
    }


    /**
     * Normalizes the probabilities in a factor so they sum to 1.
     *
     * @param factor The factor to normalize.
     * @return A new Factor with normalized probabilities.
     */
    private static Factor normalizeFactor(Factor factor) {
        double totalProbability = 0.0;
        int additionsForSum = 0;

        // 1. Calculate the sum of all probabilities in the factor
        for (double prob : factor.getValues().values()) {
            totalProbability += prob;
            if (additionsForSum > 0) { // Count additions after the first value
                _numberOfAdditions++;
            }
            additionsForSum++;
        }

        Map<Map<String, String>, Double> normalizedValues = new HashMap<>();

        // 2. Handle sum of zero (avoid division by zero, indicates contradiction or empty factor)
        if (Math.abs(totalProbability) < 1e-9) { // Use tolerance for floating point
            System.err.println("Warning: Total probability is zero during normalization. Returning factor with original (zero) values.");
            // Return a factor with zeros, or the original factor if preferred
            return new Factor(factor.getDomain(), factor.getValues()); // Return original (likely all zeros)
        }

        // 3. Divide each probability by the total sum
        for (Map.Entry<Map<String, String>, Double> entry : factor.getValues().entrySet()) {
            normalizedValues.put(entry.getKey(), entry.getValue() / totalProbability);
            // Note: Normalization division usually isn't counted in VE complexity additions/multiplications.
        }

        // Use the private Factor constructor
        return new Factor(factor.getDomain(), normalizedValues);
    }

    /**
     * Helper to generate all possible assignments for a given list of variables (domain).
     * Example: Variables A={T,F}, B={X,Y} -> [{A=T,B=X}, {A=T,B=Y}, {A=F,B=X}, {A=F,B=Y}]
     *
     * @param domain List of Variable objects.
     * @return A list of maps, where each map represents a complete assignment.
     */
    private static List<Map<String, String>> generateAssignments(List<Variable> domain, Map<String, String> evidenceAssignments) {
        List<Map<String, String>> assignments = new ArrayList<>();
        if (domain == null) { // בדיקה נוספת
            return assignments;
        }
        // *** שינוי: קריאה לפונקציה הרקורסיבית עם העדות ***
        generateAssignmentsRecursive(domain, 0, new HashMap<>(), assignments, evidenceAssignments);
        return assignments;
    }

    // *** שינוי: חתימה כוללת evidenceAssignments ***
    private static void generateAssignmentsRecursive(List<Variable> domain, int varIndex,
                                                     Map<String, String> currentAssignment,
                                                     List<Map<String, String>> allAssignments,
                                                     Map<String, String> evidenceAssignments) {
        if (varIndex == domain.size()) {
            allAssignments.add(Collections.unmodifiableMap(new HashMap<>(currentAssignment)));
            return;
        }

        Variable currentVar = domain.get(varIndex);
        String currentVarName = currentVar.getName();

        // *** שינוי: בדיקה אם המשתנה הנוכחי הוא משתנה עדות ***
        if (evidenceAssignments.containsKey(currentVarName)) {
            String fixedValue = evidenceAssignments.get(currentVarName);
            // בדוק אם הערך הקבוע חוקי עבור המשתנה (בדרך כלל כן, אבל טוב לבדוק)
            if (currentVar.getOutcomes().contains(fixedValue)) {
                currentAssignment.put(currentVarName, fixedValue);
                generateAssignmentsRecursive(domain, varIndex + 1, currentAssignment, allAssignments, evidenceAssignments);
                currentAssignment.remove(currentVarName); // Backtrack
            } else {
                // אם הערך בעדות לא חוקי למשתנה זה, זה מצב שגיאה או שמשהו השתבש
                System.err.println("Error: Evidence value '" + fixedValue + "' for variable '" + currentVarName + "' is not among its possible outcomes: " + currentVar.getOutcomes());
                // במקרה כזה, לא נוצרות השמות שתלויות בערך לא חוקי זה.
            }
        } else {
            // אם זה לא משתנה עדות, עבור על כל התוצאות האפשריות שלו כרגיל
            if (currentVar.getOutcomes() == null || currentVar.getOutcomes().isEmpty()) {
                System.err.println("Warning: Variable " + currentVar.getName() + " has no outcomes during assignment generation.");
                generateAssignmentsRecursive(domain, varIndex + 1, currentAssignment, allAssignments, evidenceAssignments); // Continue without assigning this var? Or throw error?
                return;
            }
            for (String outcome : currentVar.getOutcomes()) {
                currentAssignment.put(currentVarName, outcome);
                generateAssignmentsRecursive(domain, varIndex + 1, currentAssignment, allAssignments, evidenceAssignments);
                currentAssignment.remove(currentVarName); // Backtrack
            }
        }
    }

}