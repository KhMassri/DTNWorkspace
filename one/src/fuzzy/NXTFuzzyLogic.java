package fuzzy;

import java.util.ArrayList;

/**
 * Class responsible for definition and execution of
 * fuzzy logic controller.
 * 
 * 
 * @author Piotr Jassem, Marek Galla, Pawe³ Jankowski, Jan Drogi
 */
public class NXTFuzzyLogic {

    private ArrayList<InputLinguisticVariable> ilvList;
    private ArrayList<OutputLinguisticVariable> olvList;
    private ArrayList<Term> termListIn;
    private ArrayList<Term> termListOut;
    private ArrayList<Rule> ruleList;
    /**
     * <p>
     * linguistic rule: IF-THEN rule with condition and conclusion, one or both
     * at least linguistic.
     * </p>
     */
    private Rule[] ruleblock;
    /**
     * <p>
     * Variable that takes values in the range of linguistic terms
     * </p>
     *
     */
    private InputLinguisticVariable[] inputLinguisticVariableBlock;
    /**
     * <p>
     * Variable that takes values in the range of linguistic terms
     * </p>
     *
     */
    private OutputLinguisticVariable[] outputLinguisticVariableBlock;
    /**
     * Deffuzification method - Center of Gravity
     */
    public static final int COG = 1;
    /**
     * Deffuzification method - Center of Area
     */
    public static final int COA = 2;
    /**
     * Deffuzification method - Left Most Maximum
     */
    public static final int LM = 3;
    /**
     * Deffuzification method - Right Most Maximum
     */
    public static final int RM = 4;
    /**
     * AND operator - Minimum
     */
    public static final int MIN = 1;
    /**
     * AND operator - Product
     */
    public static final int PROD = 2;
    /**
     * AND operator - Bounded difference
     */
    public static final int BDIF = 3;

    /**
     * Default constructor.
     */
    public NXTFuzzyLogic() {
        this.ilvList = new ArrayList<InputLinguisticVariable>();
        this.olvList = new ArrayList<OutputLinguisticVariable>();
        this.termListIn = new ArrayList<Term>();
        this.termListOut = new ArrayList<Term>();
        this.ruleList = new ArrayList<Rule>();
    }

    /**
     * Sets input value of {@link nxtfuzzylogic.InputLinguisticVariable}. Call before evaluate.
     * @param name Input linguistic variable name
     * @param value Value to set
     */
    public void setInputValue(String name, float value) {
        boolean flag = false;
        for (int i = 0; i < ilvList.size(); i++) {
            if (ilvList.get(i).getName().equals(name)) {
                ilvList.get(i).setValue(value);
                flag = true;
            }
        }
        if (!flag) {
            throw new NXTFuzzyLogicException("No such linguistic variable: " + name);
        }
    }

    /**
     * Gets output value of {@link nxtfuzzylogic.OutputLinguisticVariable}. Call after evaluate.
     * @param name Output linguistic variable name
     * @return Value of output linguistic variable
     */
    public float getOutputValue(String name) {
        for (int i = 0; i < olvList.size(); i++) {
            if (olvList.get(i).getName().equals(name)) {
                return olvList.get(i).getValue();
            }
        }
        throw new NXTFuzzyLogicException("No such linguistic variable: " + name);
    }

    /**
     * <p>
     * Defines output linguistic variable.
     * </p>
     *
     * @param name
     *            linguistic variable name
     * @param min
     *            the lowest value for this linguistic variable
     * @param max
     *            the highest value
     * @param defaultValue default value
     * @param defuzzyficationMethod defuzzyfication method:
     * {@link nxtfuzzylogic.NXTFuzzyLogic#COG}
     * {@link nxtfuzzylogic.NXTFuzzyLogic#COA}
     * {@link nxtfuzzylogic.NXTFuzzyLogic#LM}
     * {@link nxtfuzzylogic.NXTFuzzyLogic#RM}
     */
    public void defineOutputLinguisticVariable(String name, float min,
            float max, float defaultValue, int defuzzyficationMethod) {
        for (int i = 0; i < olvList.size(); i++) {
            if (olvList.get(i).getName().equals(name)) {
                throw new NXTFuzzyLogicException("Linguistic variable " + name + " already exists");
            }
        }
        if (min >= max) {
            throw new NXTFuzzyLogicException("Cannot define linguistic variable: " + min + " >= " + max);
        }
        olvList.add(new OutputLinguisticVariable(name, min, max, defaultValue,
                defuzzyficationMethod));

    }

    /**
     * Defines input linguistic variable.
     *
     * @param name
     *            linguistic variable name
     * @param min
     *            the lowest value
     * @param max
     *            the highest value
     * @param defaultValue default value
     */
    public void defineInputLinguisticVariable(String name, float min,
            float max, float defaultValue) {
        for (int i = 0; i < ilvList.size(); i++) {
            if (ilvList.get(i).getName().equals(name)) {
                throw new NXTFuzzyLogicException("Linguistic variable " + name + " already exists");
            }
        }
        if (min >= max) {
            throw new NXTFuzzyLogicException("Cannot define linguistic variable: " + min + " >= " + max);
        }
        ilvList.add(new InputLinguisticVariable(name, min, max, defaultValue));

    }

    /**
     * <p>
     * Defines triangular term
     * </p>
     *
     * @param name
     *            name of the term
     * @param linguisticVariableName
     *            name of the linguistic variable described by this term
     * @param left0
     *            left bottom vertical of the triangle
     * @param middle
     *            upper vertical of the triangle
     * @param right0
     *            right bottom vertical of the triangle
     */
    public void defineTermTriangular(String name,
            String linguisticVariableName, float left0, float middle,
            float right0) {
        if (left0 >= middle || middle >= right0) {
            throw new NXTFuzzyLogicException("Cannot define triangular term: range error.");
        }
        this.defineTerm(name, linguisticVariableName, left0, middle, middle,
                right0);
    }

    /**
     * <p>
     * Defines trapezoidal term.
     * </p>
     *
     * @param name
     *            name of the term
     * @param linguisticVariableName
     *            name of the linguistic variable described by this term
     * @param left0
     *            left bottom vertical of the trapezoid
     * @param left1
     *            left upper vertical of the trapezoid
     * @param right1
     *            right upper vertical of the trapezoid
     * @param right0
     *            right bottom vertical of the trapezoid
     */
    public void defineTermTrapezoidal(String name,
            String linguisticVariableName, float left0, float left1,
            float right1, float right0) {
        if (left0 >= left1 || left1 >= right1 || right1 >= right0) {
            throw new NXTFuzzyLogicException("Cannot define trapezoidal term: range error.");
        }
        this.defineTerm(name, linguisticVariableName, left0, left1, right1,
                right0);
    }

    /**
     * Defines singleton term.
     *
     * @param name
     *            name of the term
     * @param linguisticVariableName
     *            name of the linguistic variable described by this term
     *
     * @param value
     *            value in which member function is equal to one.
     */
    public void defineTermSingleton(String name, String linguisticVariableName,
            float value) {

        this.defineTerm(name, linguisticVariableName, value, value, value,
                value);
    }

    /**
     * Defines S shape term.
     *
     * @param name
     *            name of the term
     * @param linguisticVariableName
     *            name of the linguistic variable described by this term
     * @param left0
     *            Maximum value in which membership function takes zero.
     * @param left1
     *            Minimum value in which membership function takes one.
     */
    public void defineTermSType(String name, String linguisticVariableName,
            float left0, float left1) {
        float max = -1;
        // int indexIn = -1;
        for (int i = 0; i < ilvList.size(); i++) {
            if (ilvList.get(i).getName().equals(linguisticVariableName)) {
                max = ilvList.get(i).getMax();
            }
        }

        for (int i = 0; i < olvList.size(); i++) {
            if (olvList.get(i).getName().equals(linguisticVariableName)) {
                max = olvList.get(i).getMax();
            }
        }
        if (left0 >= left1) {
            throw new NXTFuzzyLogicException("Cannot define S type term: " + left0 + " >= " + left1);
        }
        this.defineTerm(name, linguisticVariableName, left0, left1, max, max);

    }

    /**
     * Defines Z shape term.
     *
     * @param name
     *            name of term
     * @param linguisticVariableName
     *            name of the linguistic variable described by this term
     * @param right1
     *            Minimum value in which membership function takes one.
     * @param right0
     *            Maximum value in which membership function takes zero.
     */
    public void defineTermZType(String name, String linguisticVariableName,
            float right1, float right0) {
        float min = -1;
        for (int i = 0; i < ilvList.size(); i++) {
            if (ilvList.get(i).getName().equals(linguisticVariableName)) {
                min = ilvList.get(i).getMin();
            }
        }

        for (int i = 0; i < olvList.size(); i++) {
            if (olvList.get(i).getName().equals(linguisticVariableName)) {
                min = olvList.get(i).getMin();
            }
        }
        if (right1 >= right0) {
            throw new NXTFuzzyLogicException("Cannot define Z type term: " + right1 + " >= " + right0);
        }
        this.defineTerm(name, linguisticVariableName, min, min, right1, right0);

    }

    private void defineTerm(String name, String linguisticVariableName,
            float left0, float left1, float right1, float right0) {
        int indexOut = -1;
        int indexIn = -1;
        boolean flag = false;
        for (int i = 0; i < ilvList.size(); i++) {
            if (ilvList.get(i).getName().equals(linguisticVariableName)) {
                indexIn = i;
                flag = true;
            }
        }

        for (int i = 0; i < olvList.size(); i++) {
            if (olvList.get(i).getName().equals(linguisticVariableName)) {
                indexOut = i;
                flag = true;
            }
        }

        if (!flag) {
            throw new NXTFuzzyLogicException("No such linguistic variable: " + linguisticVariableName);
        }

        if (indexIn != -1) {
            termListIn.add(new Term(name, left0, left1, right1, right0, indexIn));
        }
        if (indexOut != -1) {
            termListOut.add(new Term(name, left0, left1, right1, right0,
                    indexOut));
        }

    }

    /**
     *
     * Function to define fuzzy rules. At least one rule have to be defined.
     * Conditions are connected using AND operator.
     *
     * Example:
    <PRE> defineRule(new String[] {"negSmall", "backSmall"}, "backFast", NXTFuzzyLogic.MIN) </PRE>
     *
     * @param conditions condtions terms names
     * @param conclusion conclusion term name
     * @param agregationMethod agregation method:
     * {@link nxtfuzzylogic.NXTFuzzyLogic#MIN}
     * {@link nxtfuzzylogic.NXTFuzzyLogic#PROD}
     * {@link nxtfuzzylogic.NXTFuzzyLogic#BDIF}
     */
    public void defineRule(String[] conditions, String conclusion,
            int agregationMethod) {
        int conditionFlag = 0;
        boolean conclusionFlag = false;
        Term conditions_[] = new Term[conditions.length];
        Term conclusion_ = new Term("", 0, 0, 0, 0, 0);
        for (int j = 0; j < conditions.length; j++) {
            for (int i = 0; i < termListIn.size(); i++) {
                if (conditions[j].equals(termListIn.get(i).getName())) {
                    conditions_[j] = termListIn.get(i);
                    conditionFlag++;
                }
            }
        }
        for (int i = 0; i < termListOut.size(); i++) {
            if (conclusion.equals(termListOut.get(i).getName())) {
                conclusion_ = termListOut.get(i);
                conclusionFlag = true;
            }
        }
        if(conditionFlag != conditions.length)
            throw new NXTFuzzyLogicException("Not all conditions found.");
        if(!conclusionFlag)
            throw new NXTFuzzyLogicException("Conclusion not found");
        if (conditions_.length < 1) {
            throw new NXTFuzzyLogicException("Empty condtions table.");
        }
        if (conclusion_.getName().equals("")) {
            throw new NXTFuzzyLogicException("No conclusion.");
        }
        if (agregationMethod > 3 || agregationMethod < 1) {
            throw new NXTFuzzyLogicException("No such agregation method.");
        }
        ruleList.add(new Rule(conditions_, conclusion_, agregationMethod));

    }

    /**
     * Initializes the data defined by user.
     * Execute this function at the end of controller definition.
     *
     */
    public void init() {
        OutputLinguisticVariable olvArray[] = new OutputLinguisticVariable[olvList.size()];
        InputLinguisticVariable ilvArray[] = new InputLinguisticVariable[ilvList.size()];
        Rule rulesArray[] = new Rule[ruleList.size()];
        for (int i = 0; i < olvList.size(); i++) {
            int counter = 0;
            for (int j = 0; j < termListOut.size(); j++) {
                if (termListOut.get(j).getIndexLinguisticVariable() == i) {
                    counter++;
                }
            }

            Term pomTerms[] = new Term[counter];
            counter = 0;
            for (int j = 0; j < termListOut.size(); j++) {
                if (termListOut.get(j).getIndexLinguisticVariable() == i) {
                    pomTerms[counter] = termListOut.get(j);
                    counter++;
                }
            }
            olvList.get(i).setTerms(pomTerms);
            olvArray[i] = olvList.get(i);
        }

        if (ilvList.size() == 0) {
            throw new NXTFuzzyLogicException("No input linguistic variable defined.");
        }
        if (olvList.size() == 0) {
            throw new NXTFuzzyLogicException("No output linguistic variable defined.");
        }
        if (termListIn.size() == 0) {
            throw new NXTFuzzyLogicException("No input terms defined");
        }
        if (termListOut.size() == 0) {
            throw new NXTFuzzyLogicException("No output terms defined");
        }
        if (ruleList.size() == 0) {
            throw new NXTFuzzyLogicException("No rules defined.");
        }

        for (int i = 0; i < ilvList.size(); i++) {
            ilvArray[i] = ilvList.get(i);
        }

        for (int i = 0; i < ruleList.size(); i++) {
            rulesArray[i] = ruleList.get(i);
        }

        inputLinguisticVariableBlock = ilvArray;
        outputLinguisticVariableBlock = olvArray;
        ruleblock = rulesArray;
    }

    /**
     * <p>
     * Operator used in Fuzzy Logic theory. Minimum by default.
     * </p>
     *
     * @param terms
     * @param f
     * @return
     */
    private float and(Term[] terms, int f) {

        for (int i = 0; i < terms.length; i++) {
            terms[i].is(inputLinguisticVariableBlock[terms[i].getIndexLinguisticVariable()].getValue());
        }
        float result;

        Term tempTerm;
        if (f == NXTFuzzyLogic.PROD) {
            result = 1;
            for (int i = 0; i < terms.length; i++) {
                tempTerm = terms[i];
                result = result * tempTerm.getDegree();
            }
        } else {
            if (f == NXTFuzzyLogic.BDIF) {
                tempTerm = terms[0];
                result = tempTerm.getDegree();
                for (int i = 1; i < terms.length; i++) {
                    tempTerm = terms[i];
                    if (result + tempTerm.getDegree() - 1 > 0) {
                        result = result + tempTerm.getDegree() - 1;
                    } else {
                        result = 0;
                    }
                }
            } else {
                result = 1;
                for (int i = 0; i < terms.length; i++) {
                    tempTerm = terms[i];
                    if (tempTerm.getDegree() < result) {
                        result = tempTerm.getDegree();
                    }
                }
            }
        }
        return result;
    }

    private float fuzzify() {
        /**
         * @author Marek Galla <h4>Fuzzification. <h4/>
         *         <p>
         *         The values of the input variables have to be converted into
         *         degrees of membership for the membership functions defined on
         *         the variable.
         *         <p/>
         */
        for (int i = 0; i < outputLinguisticVariableBlock.length; i++) {
            for (int j = 0; j < outputLinguisticVariableBlock[i].getTerms().length; j++) {
                outputLinguisticVariableBlock[i].getTerms()[j].setDegree(0);
            }
        }

        for (int i = 0; i < getRuleblock().length; i++) {

            if (and(getRuleblock()[i].getCondition(), getRuleblock()[i].getAgregationMethod()) > getRuleblock()[i].getConclusion().getDegree()) {
                getRuleblock()[i].getConclusion().setDegree(
                        and(getRuleblock()[i].getCondition(), getRuleblock()[i].getAgregationMethod()));
            }

        }

        return 0;
    }

    private float trapezoidArea(Term terma) {

        float pt1 = terma.getLeft0();
        float pt2 = terma.getLeft1();
        float pt3 = terma.getRight1();
        float pt4 = terma.getRight0();
        float scale = terma.getDegree();

        float offset;

        if (pt1 != 0) {
            offset = pt1;
            pt1 -= offset;
            pt2 -= offset;
            pt3 -= offset;
            pt4 -= offset;
        }

        pt2 = pt2 * scale;
        if (pt4 - pt3 != 0) {
            pt3 = pt3 + ((1 - scale) * (pt4 - pt3));
        }

        float leftArea = ((pt2 - pt1) * scale) / 2;
        float midArea = (pt3 - pt2) * scale;
        float rightArea = ((pt4 - pt3) * scale) / 2;

        float area = leftArea + midArea + rightArea;
        return area;
    }

    private float COG(OutputLinguisticVariable out) {
        /**
         * @author Piotr <h4>Center of gravity defuzzification method</h4>
         *         <p>
         *         The center of gravity is a geometric property of any object.
         *         The center of gravity is the average location of the weight
         *         of an object. We can completely describe the motion of any
         *         object through space in terms of the translation of the
         *         center of gravity of the object from one place to another,
         *         and the rotation of the object about its center of gravity if
         *         it is free to rotate.
         *         </p>
         *         <p>
         *         For a general shaped object, there is a simple mechanical way
         *         to determine the center of gravity:
         *         </p>
         *         <p>
         *         If we just balance the object using a string or an edge, the
         *         point at which the object is balanced is the center of
         *         gravity. (Just like balancing a pencil on your finger!)
         *         </p>
         */
        float totalArea = 0;
        float defuzz = 0;
        for (int i = 0; i < out.getTerms().length; i++) {

            Term terma = out.getTerms()[i];
            float pt1 = terma.getLeft0();
            float pt2 = terma.getLeft1();
            float pt3 = terma.getRight1();
            float pt4 = terma.getRight0();
            float scale = terma.getDegree();
            float leftCenter, rightCenter, midCenter;
            float leftArea, rightArea, midArea;
            float cog, offset;

            offset = pt1;
            pt1 = (pt1 - offset);
            pt2 = (pt2 - offset);
            pt3 = (pt3 - offset);
            pt4 = (pt4 - offset);

            leftCenter = ((pt1 + pt2 * 2) / 3) + offset;
            midCenter = ((pt3 + pt2) / 2) + offset;
            rightCenter = ((pt3 * 2 + pt4) / 3) + offset;

            leftArea = ((pt2 - pt1) * scale) / 2;
            midArea = (pt3 - pt2) * scale;
            rightArea = ((pt4 - pt3) * scale) / 2;
            if (leftArea + midArea + rightArea != 0) {
                cog = (float) ((leftCenter * leftArea + rightCenter * rightArea + midCenter
                        * midArea) / (leftArea + midArea + rightArea));
            } else {
                cog = 0;
            }
            float area = trapezoidArea(terma);

            totalArea += area;
            defuzz += cog * area;
        }
        float retDefuzz = 0;
        if (totalArea != 0) {
            retDefuzz = defuzz / totalArea;
        }
        return retDefuzz;
    }

    /**
     * <p>
     * Center of area defuzzification method.
     * </p>
     * <p>
     * Wykorzystuje metodÄ Årodka figury
     * </p>
     */
    private float COA(OutputLinguisticVariable out) {

        float totalArea = 0;
        float defuzz = 0;
        for (int i = 0; i < out.getTerms().length; i++) {

            Term terma = out.getTerms()[i];
            float x1 = terma.getLeft0();
            float x2 = terma.getLeft1();
            float x3 = terma.getRight1();
            float x4 = terma.getRight0();
            float h = terma.getDegree();

            float offset = x1;
            x1 = (x1 - offset);
            x2 = (x2 - offset);
            x3 = (x3 - offset);
            x4 = (x4 - offset);

            float P = (x4 + x3 - x2 - x1) / 2 * h;
            float coa;
            if (h == 0) {
                coa = 0;
            } else {
                coa = ((P / 2) - (x2 - x1) / 2 * h) / h + x2;
                if ((x2 - x1) / 2 * h > P / 2) {
                    coa = x4 - P / h;
                }
                if ((x4 - x3) / 2 * h > P / 2) {
                    coa = P / h + x1;
                }
                coa += offset;
            }
            float area = trapezoidArea(terma);
            totalArea += area;
            defuzz += coa * area;
        }
        float retDefuzz = 0;
        if (totalArea != 0) {
            retDefuzz = defuzz / totalArea;
        }
        return retDefuzz;
    }

    // funkcja defuzyfikuj1ca przy pomocy metody LM (Left Most Maximum)
    private float LM(OutputLinguisticVariable out) {
        /**
         *
         * <h4>Left Most Maximum LM</h4>
         * <p>
         * The value of the output variable is determined for which the
         * membership function of the output reaches its leftmost maximum.
         * </p>
         */
        float left = Float.MAX_VALUE;
        float right = Float.MIN_VALUE;
        for (int i = 0; i < out.getTerms().length; i++) {
            Term terma = out.getTerms()[i];
            float x1 = terma.getLeft0();
            float x4 = terma.getRight0();
            if (x1 < left) {
                left = x1;
            }
            if (x4 > right) {
                right = x4;
            }
        }

        float i = left;
        float exit = 0;
        float maxPos = 0;
        float lmax = 0;
        while ((i < right) && (exit == 0)) {
            float max = 0;
            for (int i1 = 0; i1 < out.getTerms().length; i1++) {
                Term terma = out.getTerms()[i1];
                float x1 = terma.getLeft0();
                float x2 = terma.getLeft1();
                float x3 = terma.getRight1();
                float x4 = terma.getRight0();
                float h = terma.getDegree();

                if ((i >= x1) && (i <= x3)) {

                    x2 = x1 + h * (x2 - x1);
                    x3 = x4 - h * (x4 - x3);
                    if ((i <= x3) && (i >= x2)) {
                        if (h > max) {
                            max = h;
                        }

                    } else {
                        if (i < x2) {
                            h = h * (i - x1) / (x2 - x1);
                            if (h > max) {
                                max = h;
                            }

                        } else {
                            h = h * (x4 - i1) / (x4 - x3);
                            if (h > max) {
                                max = h;
                            }

                        }
                    }

                }
            }
            if (max > lmax) {
                lmax = max;
                maxPos = i;
            }
            if (max < lmax) {
                exit = 1;
            }
            i += 0.01;
        }
        return maxPos;
    }

    private float RM(OutputLinguisticVariable out) {
        /**
         * <h4>Right Most Maximum RM</h4>
         * <p>
         * The value of the output variable is determined for which the
         * membership function of the output reaches its rightmost maximum.
         * </p>
         */
        float left = Float.MAX_VALUE;
        float right = Float.MIN_VALUE;
        for (int i = 0; i < out.getTerms().length; i++) {
            Term terma = out.getTerms()[i];
            float x1 = terma.getLeft0();
            float x4 = terma.getRight0();
            if (x1 < left) {
                left = x1;
            }
            if (x4 > right) {
                right = x4;
            }
        }

        float i = right;
        float exit = 0;
        float maxPos = 0;
        float rmax = 0;
        while ((i > left) && (exit == 0)) {
            float max = 0;
            for (int i1 = 0; i1 < out.getTerms().length; i1++) {
                Term terma = out.getTerms()[i1];
                float x1 = terma.getLeft0();
                float x2 = terma.getLeft1();
                float x3 = terma.getRight1();
                float x4 = terma.getRight0();
                float h = terma.getDegree();

                if ((i >= x1) && (i <= x4)) {

                    x2 = x1 + h * (x2 - x1);
                    x3 = x4 - h * (x4 - x3);
                    // +i);
                    if ((i <= x3) && (i >= x2)) {
                        if (h > max) {
                            max = h;
                        }

                    } else {
                        if (i < x2) {
                            h = h * (i - x1) / (x2 - x1);
                            if (h > max) {
                                max = h;
                            }

                        } else {
                            h = h * (x4 - i) / (x4 - x3);
                            if (h > max) {
                                max = h;
                            }

                        }
                    }

                }
            }
            if (max > rmax) {
                rmax = max;
                maxPos = i;
            }
            if (max < rmax) {
                exit = 1;
            }
            i -= 0.01;
        }
        return maxPos;
    }

    /**
     * <p>
     * Blok wyostrzania
     * </p>
     * <p>
     * Sprawdzamy jakÄ metoda wyostrzania zostaÅa wybrana przez uÅ¼ytkownika.
     * </p>
     * <p>
     * Wyliczamy wartoÅÄ korzystajÄc z odpowiedniej metody
     * </p>
     * <p>
     * Na koniec sprawdzamy, czy otrzymana wartoÅÄ nie jest mniejsza od
     * zdefiowanego minimum
     * </p>
     *
     */
    private float defuzzify(OutputLinguisticVariable out) {

        float tmp = out.getDefuzzificationMethod();
        float returnValue = 0;
        if (tmp == NXTFuzzyLogic.COG) {
            returnValue = COG(out);
        }
        if (tmp == NXTFuzzyLogic.COA) {
            returnValue = COA(out);
        }
        if (tmp == NXTFuzzyLogic.LM) {
            returnValue = LM(out);
        }
        if (tmp == NXTFuzzyLogic.RM) {
            returnValue = RM(out);
        }

        if (out.getMin() > returnValue) {
            returnValue = out.getMin();
        }
        if (out.getMax() < returnValue) {
            returnValue = out.getMax();
        }

        return returnValue;
    }

    /**
     * Computes output of fuzzy controller. Input variable values
     * have to be set up before the call.
     */
    public void evaluate() {


        fuzzify();
        for (int i = 0; i < outputLinguisticVariableBlock.length; i++) {
            float value = defuzzify(outputLinguisticVariableBlock[i]);
            outputLinguisticVariableBlock[i].setValue(value);
        }

    }

    private Rule[] getRuleblock() {
        return this.ruleblock;
    }
}
