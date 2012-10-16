package fuzzy;
/**
 * Input variable that takes values in the range of linguistic terms.
 */
public class InputLinguisticVariable {

    private String name;
    private float min;
    private float max;
    private float value;

    public InputLinguisticVariable(String name_, float min_, float max_, float defaultValue_) /**
     * kontstruktor
     */
    {
        this.value = 0; // default value
        this.min = min_;
        this.max = max_;
        this.setName(name_);
    }

    public void setMin(float min) {
        this.min = min;
    }

    /**
     * @return the min
     */
    public float getMin() {
        return min;
    }

    /**
     * @param max
     *            the max to set
     */
    public void setMax(float max) {
        this.max = max;
    }

    /**
     * @return the max
     */
    public float getMax() {
        return max;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(float value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public float getValue() {
        return value;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
}
