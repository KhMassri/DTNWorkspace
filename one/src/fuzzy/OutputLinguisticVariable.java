package fuzzy;
/**
 * Output variable that takes values in the range of linguistic terms.
 */
public class OutputLinguisticVariable {

    private String name;
    private Term terms[];
    private float min;
    private float max;
    private float defaultValue;
    private int defuzzificationMethod;
    private float value;


    public OutputLinguisticVariable(String name_, float min_, float max_, float defaultValue_, int dm_) {
        this.value = 0;
        this.min = min_;
        this.max = max_;
        this.defaultValue = defaultValue_;
        this.defuzzificationMethod = dm_;
        this.setName(name_);
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(float defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int getDefuzzificationMethod() {
        return defuzzificationMethod;
    }

    public void setDefuzzificationMethod(int defuzzificationMethod) {
        this.defuzzificationMethod = defuzzificationMethod;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public float getMin() {
        return min;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public Term[] getTerms() {
        return terms;
    }

    public void setTerms(Term[] terms) {
        this.terms = terms;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
