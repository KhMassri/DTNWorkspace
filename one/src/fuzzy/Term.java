package fuzzy;
/**
 * Class to describe terms.
 */
public class Term {

    private String name;
    private float left0;
    private float left1;
    private float right1;
    private float right0;
    private float degree;
    private int indexLinguisticVariable;

    public Term(String name_, float left0_, float left1_, float right1_, float right0_, int index) {
        this.setName(name_);
        this.left0 = left0_;
        this.left1 = left1_;
        this.right1 = right1_;
        this.right0 = right0_;
        this.setIndexLinguisticVariable(index);
    }

    public void is(float value) {
        float degree = 0;
        if (value <= this.getLeft0()) {
            degree = 0;
        }
        if ((value > this.getLeft0()) && (value < this.getLeft1())) {
            degree = (value - this.getLeft0())
                    / (this.getLeft1() - this.getLeft0());
        }
        if ((value >= this.getLeft1()) && (value <= this.getRight1())) {
            degree = 1;
        }
        if ((value > this.getRight1()) && (value < this.getRight0())) {
            degree = (value - this.getRight0())
                    / (this.getRight1() - this.getRight0());
        }
        if (value >= this.getRight0()) {
            degree = 0;
        }
        this.degree = degree;

    }

    public float getDegree() {
        return degree;
    }

    public void setDegree(float degree) {
        this.degree = degree;
    }

    public float getLeft0() {
        return left0;
    }

    public void setLeft0(float left0) {
        this.left0 = left0;
    }

    public float getLeft1() {
        return left1;
    }

    public void setLeft1(float left1) {
        this.left1 = left1;
    }

    public float getRight0() {
        return right0;
    }

    public void setRight0(float right0) {
        this.right0 = right0;
    }

    public float getRight1() {
        return right1;
    }

    public void setRight1(float right1) {
        this.right1 = right1;
    }

    public void setIndexLinguisticVariable(int indexLinguisticVariable) {
        this.indexLinguisticVariable = indexLinguisticVariable;
    }

    public int getIndexLinguisticVariable() {
        return indexLinguisticVariable;
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
