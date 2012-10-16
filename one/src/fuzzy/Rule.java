package fuzzy;
/**
 * Class to describe fuzzy rules. Conditions are connected using AND operator.
 */
public class Rule {

    private Term[] condition;
    private Term conclusion;
    private int agregationMethod;

    public Rule(Term[] condition_, Term conclusion_, int agregationMethod_) {
        this.conclusion = conclusion_;
        this.condition = condition_;
        this.agregationMethod = agregationMethod_;
    }

    /**
     * @param condition the condition to set
     */
    public void setCondition(Term[] condition) {
        this.condition = condition;
    }

    /**
     * @return the condition
     */
    public Term[] getCondition() {
        return condition;
    }

    /**
     * @param conclusion the conclusion to set
     */
    public void setConclusion(Term conclusion) {
        this.conclusion = conclusion;
    }

    /**
     * @return the conclusion
     */
    public Term getConclusion() {
        return conclusion;
    }

    public void setAgregationMethod(int agregationMethod) {
        this.agregationMethod = agregationMethod;
    }

    public int getAgregationMethod() {
        return agregationMethod;
    }
}
