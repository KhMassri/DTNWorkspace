package fuzzy;
/**
 *
 * @author Pawel Jankowski
 */

public class RulesBase {

    private NXTFuzzyLogic nfl = new NXTFuzzyLogic();

    public RulesBase() {

        nfl = new NXTFuzzyLogic();

       nfl.defineInputLinguisticVariable("FTC", 0, 7, 0);
       nfl.defineInputLinguisticVariable("MS", 0, 200, 0);
       
        
        nfl.defineTermZType("low", "FTC", 0, 3);
        nfl.defineTermTriangular("medium", "FTC", 0, 3, 6);
        nfl.defineTermSType("high", "FTC", 3, 6);
        
        nfl.defineTermTriangular("small", "MS", -100, 0, 100);
        nfl.defineTermTriangular("med", "MS", 0, 100, 200);
        nfl.defineTermTriangular("large", "MS", 100, 200, 300);
        
        nfl.defineOutputLinguisticVariable("BS", 0, 1, 0, NXTFuzzyLogic.COA);
        nfl.defineTermZType("BS0", "BS", 0.0f, 0.15f);
        nfl.defineTermTriangular("BS1", "BS", 0.15f, 0.2f, 0.25f);
        nfl.defineTermTriangular("BS2", "BS", 0.25f, 0.3f, 0.35f);
        nfl.defineTermTriangular("BS3", "BS", 0.35f, 0.4f, 0.45f);
        nfl.defineTermTriangular("BS4", "BS", 0.45f, 0.5f, 0.55f);
        nfl.defineTermTriangular("BS5", "BS", 0.55f, 0.6f, 0.65f);
        nfl.defineTermTriangular("BS6", "BS", 0.65f, 0.7f, 0.75f);
        nfl.defineTermTriangular("BS7", "BS", 0.75f, 0.8f, 0.85f);
        nfl.defineTermSType("BS8", "BS", 0.85f, 1.0f);
        
               

        String[] r0 = {"low", "small"};
        nfl.defineRule(r0, "BS0", NXTFuzzyLogic.MIN);
        String[] r1 = {"low", "med"};
        nfl.defineRule(r1, "BS1", NXTFuzzyLogic.MIN);
        String[] r2 = {"low", "large"};
        nfl.defineRule(r2, "BS2", NXTFuzzyLogic.MIN);
        String[] r3 = {"medium", "small"};
        nfl.defineRule(r3, "BS3", NXTFuzzyLogic.MIN);
        String[] r4 = {"medium", "med"};
        nfl.defineRule(r4, "BS4", NXTFuzzyLogic.MIN);
        String[] r5 = {"medium", "large"};
        nfl.defineRule(r5, "BS5", NXTFuzzyLogic.MIN);
        String[] r6 = {"high", "small"};
        nfl.defineRule(r6, "BS6", NXTFuzzyLogic.MIN);
        String[] r7 = {"high", "med"};
        nfl.defineRule(r7, "BS7", NXTFuzzyLogic.MIN);
        String[] r8 = {"high", "large"};
        nfl.defineRule(r8, "BS8", NXTFuzzyLogic.MIN);
        

       
        nfl.init();
    }

    public void setInput(int fTC, int mS){
        nfl.setInputValue("FTC", fTC);
        nfl.setInputValue("MS", mS);
    }

    public double getOutput(){
        nfl.evaluate();
        return  nfl.getOutputValue("BS");
    }
}
