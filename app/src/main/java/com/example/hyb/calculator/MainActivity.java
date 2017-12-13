package com.example.hyb.calculator;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @BindView(R.id.calculateEditView)
    EditText calculatorEditText;
    @BindView(R.id.result)
    TextView result;

    @BindView(R.id.button0)
    Button button0;
    @BindView(R.id.buttonPosiNeg)
    Button buttonPosiNeg;
    @BindView(R.id.button1)
    Button button1;
    @BindView(R.id.button2)
    Button button2;
    @BindView(R.id.button3)
    Button button3;
    @BindView(R.id.button4)
    Button button4;
    @BindView(R.id.button5)
    Button button5;
    @BindView(R.id.button6)
    Button button6;
    @BindView(R.id.button7)
    Button button7;
    @BindView(R.id.button8)
    Button button8;
    @BindView(R.id.button9)
    Button button9;

    @BindView(R.id.buttonClear)
    Button buttonClear;

    @BindView(R.id.buttondot)
    Button buttonDot;
    @BindView(R.id.buttonPlus)
    Button buttonPlus;
    @BindView(R.id.buttonMinus)
    Button buttonMinus;
    @BindView(R.id.buttonMul)
    Button buttonMul;
    @BindView(R.id.buttonDiv)
    Button buttonDiv;
    @BindView(R.id.buttonMod)
    Button buttonMod;
    @BindView(R.id.buttonEquals)
    Button buttonEquals;

    // store the formula
    private ArrayList<Character> formula = new ArrayList<>();
    // max number of operand
    private final int NUMBEROFOPERAND = 10;
    // the index of last number or operand that user input
    private int lastIndex;

    private int operandsCount;

    // 0: no operand, 1: + or -, 2: * or /, 3: ()
    private int priority;

    private boolean isBracketOn = false;
    private int leftBracketNum;
    private int rightBracketNum;

    private long lastClickTime;
    private int clickNumber = 0;
    private static final int SECTETNUMBERs = 5;
    private static long TIMEDIFFERENCE = 500;
    private static final String SECTETMESSAGE = "Made by Yanbin Hu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        clearText(null);
    }

    public void showDigital(View view) {
        Button b = (Button) view;
        String input = b.getText().toString();

        // case 1: empty text => P4 or empty text => P0.
        // case 2: P5 => P51
        // case 3: N5N => N5N
        // case 4: (P5P + N2N) => (P5P + N2N)
        // case 5: P4P + => P4P + P3
        // case 6: ( => (P
        // case 7: N => N4

        // case 1, 5, 6
        if (lastIndex == -1 || isOperand(formula.get(lastIndex)) || '(' == formula.get(lastIndex)) {
            formula.add('P');
            lastIndex++;
            if (input.charAt(0) == '.') {
                formula.add('0');
                lastIndex++;
            }
            formula.add(input.charAt(0));
            lastIndex++;
        } else if (isNumberOrDot(formula.get(lastIndex))) {
            // case 2
            formula.add(input.charAt(0));
            lastIndex++;
        } else if (isPrefix(formula.get(lastIndex))) {
            // case 3,7
            if (!(lastIndex > 0 && isNumberOrDot(formula.get(lastIndex - 1)))) {
                formula.add(input.charAt(0));
                lastIndex++;
            }
        } else if (')' == formula.get(lastIndex)) {
            // case 4
        }
        showText();
        formatFinalResult(calculate(this.formula));
    }

    public void showOperand(View view) {
        if (lastIndex < 0) return;
        if (operandsCount >= NUMBEROFOPERAND) {
            Toast.makeText(this, "Max operand is " + NUMBEROFOPERAND, Toast.LENGTH_SHORT).show();
            return;
        }

        // cases, for example click "+" button
        // case 1: P5 => P5P+
        // case 2: P4P- => P4P+
        // case 3: (P5P + N2N) => (P5P + N2N) +
        // case 4: (P5P + N2N => (P5P + N2N +
        String input = ((Button) view).getText().toString();
        if("Mod".equals(input)){
            input = "%";
        }
        // case 2
        if (isOperand(formula.get(lastIndex))) {
            formula.set(lastIndex, input.charAt(0));
        } else if (isNumberOrDot(formula.get(lastIndex))) {
            // case 1
            addSuffix();
            formula.add(input.charAt(0));
            lastIndex++;
            operandsCount++;
        } else {
            // case 3 and case 4
            formula.add(input.charAt(0));
            lastIndex++;
            operandsCount++;
        }
        showText();
    }

    private ArrayList<Character> calculate(ArrayList<Character> f) {
        ArrayList<Character> finalResult = null;
        this.priority = getFormulaPriority(f);
        switch (this.priority) {
            case 0:
                finalResult = f;
            case 1:
                // reduce one equation, then rebuild formula , then calculate again
                int[] indexes = new int[2];
                indexes[0] = Integer.MIN_VALUE; // previoue operand index
                indexes[1] = Integer.MAX_VALUE; // next operand index
                BigDecimal newValue = reduceOneOperand(f, 1,indexes);
                if (newValue != null) {
                    ArrayList<Character> newFormula = rebuildFormula(f, newValue,indexes);
                    Log.d(TAG, "calculate: new formula: " + formulaToString(newFormula));
                    finalResult = calculate(newFormula);
                }
                break;
            case 2:
                int[] indexes2 = new int[2];
                indexes2[0] = Integer.MIN_VALUE;
                indexes2[1] = Integer.MAX_VALUE;
                BigDecimal newValue2 = reduceOneOperand(f, 2, indexes2);
                if (newValue2 != null) {
                    ArrayList<Character> newFormula2 = rebuildFormula(f, newValue2,indexes2);
                    Log.d(TAG, "calculate: new formula: " + formulaToString(newFormula2));
                    finalResult = calculate(newFormula2);
                }
                break;
            case 3:
                int[] indexes3 = new int[2];
                indexes3[0] = Integer.MIN_VALUE;
                indexes3[1] = Integer.MAX_VALUE;
                // from the bracket build a new formula and calculate it.
                ArrayList<Character> bracketFormular = extractFormularFromBracket(f,indexes3);
                Log.d(TAG, "calculate: bracket Formular: " + formulaToString(bracketFormular));
                ArrayList<Character> bracketResult = calculate(bracketFormular);
//                if(bracketResult != null){
//                    Log.d(TAG, "calculate: bracket formular result = " + bracketResult.toString());
//                }
                ArrayList<Character> newFormula3 = rebuildFormula(f, bracketResult,indexes3);
                finalResult = calculate(newFormula3);

                //finalResult = ?
                break;
            case 4:
                // mod operation
//                ArrayList<Character> leftResult = calculate(getLeftFormula(f));
//                ArrayList<Character> rightResult = calculate(getRightFormula(f));

                ArrayList<Character> leftResult = getLeftFormula(f);
                ArrayList<Character> rightResult = getRightFormula(f);
                if(!isIntValue(leftResult) || !isIntValue(rightResult)){
                    result.setText("Not a valid modulo operation");
                }else{
                    Log.d(TAG, "calculate: left:" + leftResult  + ", right:"+ rightResult);
                    BigDecimal modResult =  performOperand(formularToBigDecimalInteger(leftResult), formularToBigDecimalInteger(rightResult), '%');
                    Log.d(TAG, "calculate: modolu result " + modResult);
                    finalResult = bigDecimalToFormula(modResult);
                }
                break;
            default:
                break;
        }
        return finalResult;
    }

    // this formula contains only 0-9, ',' and prefix
    private boolean isIntValue(ArrayList<Character> f){
        if(f == null || f.isEmpty()) return false;
        StringBuilder sb = new StringBuilder();
        boolean isPositive = f.get(0) == 'P';
        for(int i = 0 ; i < f.size();i++){
            if(isOperand(f.get(i)) || isBracket(f.get(i))) return false;
            if(!isPrefix(f.get(i))){
                sb.append(f.get(i));
            }
        }
        BigDecimal bd =  new BigDecimal( isPositive ? sb.toString() : "-"+sb);
        return isIntegerValue(bd);
    }

    private boolean isIntegerValue(BigDecimal bd) {
        return bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0;
    }

    // convert a formula ( N40N) to BigDecimal
    private BigDecimal formularToBigDecimalInteger(ArrayList<Character> f){
        // f must be an integer value; N4N or N4.0N
        if(!isIntValue(f)) return null;
        StringBuilder sb = new StringBuilder();
        boolean isPositive = f.get(0) == 'P';
        for(int i = 0 ; i < f.size();i++){
            if('.' == f.get(i)) break;
            if(!isPrefix(f.get(i))){
                sb.append(f.get(i));
            }
        }
       return new BigDecimal( isPositive ? sb.toString() : "-"+sb);
    }

    private ArrayList<Character> bigDecimalToFormula(BigDecimal bd){
        boolean isPositive = bd.signum() >= 0;
        ArrayList<Character> arr = new ArrayList<>();
        arr.add(isPositive ? 'P' : 'N');
        String s = bd.stripTrailingZeros().toPlainString();
        for(int i = 0 ; i < s.length();i++){
            arr.add(s.charAt(i));
        }
        arr.add(isPositive ? 'P' : 'N');
        return  arr;
    }

    private ArrayList<Character> getLeftFormula(ArrayList<Character> f){
        int indexOfMod = f.indexOf('%');
        return new ArrayList<>(f.subList(0,indexOfMod));
    }

    private ArrayList<Character> getRightFormula(ArrayList<Character> f){
        int indexOfMod = f.indexOf('%');
        return new ArrayList<>(f.subList(indexOfMod + 1,f.size()));
    }

    private void formatFinalResult(ArrayList<Character> f) {
        if( f == null || f.size() == 0) return;
        StringBuilder formulaString = new StringBuilder();
        for (int i = 1; i < f.size(); i++) {
            if (isNumberOrDot(f.get(i))) {
                formulaString.append(f.get(i));
            }
        }
        // 0 belongs to positive
        boolean isPositive = f.get(0) == 'P' ||
                new BigDecimal(formulaString.toString()).compareTo(BigDecimal.ZERO) == 0;
        String text = isPositive ? formulaString.toString() : '-' + formulaString.toString();
        result.setText(text);
    }

    // according to priority, reduce one operand, means perform one operation +/- or *//
    // find the first operand according to priority, get the left number and right number, perform the calculation.
    private BigDecimal reduceOneOperand(ArrayList<Character> f, int priority, int[] indexes) {
        if (priority == 0) return null;
        BigDecimal newValue = null;
        for (int i = 0; i < f.size(); i++) {
            boolean condition = priority == 2 ? isMulOrDiv(f.get(i)) : isPlusOrMinus(f.get(i));
            if (condition) {
                // if operand is * or /
                BigDecimal operator1;
                StringBuilder operator1Str = new StringBuilder();
                BigDecimal operator2;
                StringBuilder operator2Str = new StringBuilder();
                // get the left number
                for (int j = i - 1; j >= 0; j--) {
                    if (isOperand(f.get(j))) {
                        // previous operand exists and being saved
                        indexes[0] = j;
                        break;
                    } else {
                        operator1Str.append(f.get(j));
                    }
                }
                String reverser = operator1Str.reverse().toString();
                if (reverser.charAt(0) == 'P') {
                    operator1 = new BigDecimal(reverser.substring(1, reverser.length() - 1));
                } else {
                    operator1 = new BigDecimal("-" + reverser.substring(1, reverser.length() - 1));
                }
                // get the right number
                boolean isPositive = 'P' == f.get(i + 1);
                for (int j = i + 1; j < f.size(); j++) {
                    if (isOperand(f.get(j))) {
                        // next operand exists and being saved
                        indexes[1] = j;
                        break;
                    } else if (isPrefix(f.get(j))) {
                        continue;
                    } else {
                        operator2Str.append(f.get(j));
                    }
                }
                operator2 = isPositive ? new BigDecimal(operator2Str.toString()) : new BigDecimal("-" + operator2Str);
                newValue = performOperand(operator1, operator2, f.get(i));
                break;
            }
        }
        return newValue;
    }

    private ArrayList<Character> rebuildFormula(ArrayList<Character> f, BigDecimal newValue, int[] indexes) {
        ArrayList<Character> newFormular = new ArrayList<>();
        if (indexes[0] > Integer.MIN_VALUE) {
            for (int i = 0; i <= indexes[0]; i++) {
                newFormular.add(f.get(i));
            }
        }

        boolean isPositive = newValue.compareTo(BigDecimal.ZERO) >= 0;
        newFormular.add(isPositive ? 'P' : 'N');
        String value = newValue.toPlainString();
        for (int i = isPositive ? 0 : 1; i < value.length(); i++) {
            newFormular.add(value.charAt(i));
        }
        newFormular.add(isPositive ? 'P' : 'N');

        if (indexes[1]  < Integer.MAX_VALUE) {
            for (int i = indexes[1]; i < f.size(); i++) {
                newFormular.add(f.get(i));
            }
        }
        return newFormular;
    }

    private ArrayList<Character> rebuildFormula(ArrayList<Character> f, ArrayList<Character> newValue, int[] indexes) {
        if(newValue == null) return null;
        ArrayList<Character> newFormular = new ArrayList<>();
        if (indexes[0] > Integer.MIN_VALUE) {
            for (int i = 0; i <= indexes[0]; i++) {
                newFormular.add(f.get(i));
            }
        }
        newFormular.addAll(newValue);
        if (indexes[1]  < Integer.MAX_VALUE) {
            for (int i = indexes[1]; i < f.size(); i++) {
                newFormular.add(f.get(i));
            }
        }
        return newFormular;
    }

    private ArrayList<Character> extractFormularFromBracket(ArrayList<Character> f, int[] indexes) {
        // case 1: 3+(4+3)+(4+2) => 4+3
        // case 2: ((8+2 => (8+2
        // case 3: ( (3+2) * (3+2) ) => (3+2) * (3+2)
        // case 4: (8-9) + (8  => 8-9  or  (5+6) + (3+3) + (5  => 5+6

        ArrayList<Character> newFormula = new ArrayList<>();
        // begin to add char to new formula
        boolean extracting = false;
        int balance = 0;
        for(int i = 0 ; i < f.size();i++){
            if ('(' == f.get(i) ) {
                extracting = true ;
                balance++;
                // first time to meet '('
                if(balance == 1) {
                    //previous Operand Index
                    indexes[0] = i == 0 ? Integer.MIN_VALUE : i - 1;
                    continue;
                }
            }
            if (')' == f.get(i)) {
                balance--;
            }
            if( extracting && balance == 0){
                // extracting = false;
                indexes[1] = i == f.size()-1 ? Integer.MAX_VALUE : i+1;
                break;
            }
            if (extracting) {
                newFormula.add(f.get(i));
            }
        }
        return newFormula;
    }

    private boolean isNumberOrDot(Character s) {
        return TextUtils.isDigitsOnly(s.toString()) || '.' == s;
    }

    private boolean isOperand(Character s) {
        return '+' == s || '-' == s || '*' == s || '/' == s || '%' == s;
    }

    private boolean isPrefix(Character c) {
        return 'P' == c || 'N' == c;
    }

    private boolean isMulOrDiv(Character s) {
        return '*' == s || '/' == s;
    }

    private boolean isPlusOrMinus(Character s) {
        return '+' == s || '-' == s;
    }

    private int getFormulaPriority(ArrayList<Character> f) {
        if (f == null) return -1;
        int pri = 0;
        for (int i = 0; i < f.size(); i++) {
            if (isPlusOrMinus(f.get(i)) && pri < 1) {
                pri = 1;
            } else if (isMulOrDiv(f.get(i)) && pri < 2) {
                pri = 2;
            } else if (isBracket(f.get(i)) && pri < 3) {
                pri = 3;
            } else if(isMod(f.get(i)) && pri < 4){
                pri = 4;
            }
        }
        return pri;
    }

    private String formulaToString(ArrayList<Character> f) {
        if(f == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : f) {
            result.append(c);
        }

        return result.toString();
    }

    private BigDecimal performOperand(BigDecimal a, BigDecimal b, Character operand) {
        BigDecimal newResult;
        switch (operand) {
            case '+':
                newResult = a.add(b);
                break;
            case '-':
                newResult = a.subtract(b);
                break;
            case '*':
                newResult = a.multiply(b);
                break;
            case '/':
                if (b.compareTo(BigDecimal.ZERO) != 0)
                    newResult = a.divide(b, 10, BigDecimal.ROUND_HALF_UP);
                else return null;
                break;
            default:
                newResult = new BigDecimal(mod(a.intValue(), b.intValue()));
        }
        // erase error
        BigDecimal bd = newResult.setScale(0, BigDecimal.ROUND_HALF_UP);
        BigDecimal round = new BigDecimal(0.0000000005).setScale(10, BigDecimal.ROUND_HALF_UP);
        if (newResult.subtract(bd).abs().compareTo(round) <= 0) {
            newResult = bd;
        }

        return newResult.stripTrailingZeros();
    }

    private int mod(int x, int y) {
        int result = x % y;
        return result < 0 ? result + y : result;
    }

    private void showText() {
        calculatorEditText.setText("");
        boolean bracketOff = false; // Must bracket be off? default is false;
        for (Character s : formula) {
            if ('P' == s) continue;
            if ('N' == s) {
                if (!bracketOff) {
                    calculatorEditText.append("(-");
                    bracketOff = true;
                    continue;
                } else {
                    calculatorEditText.append(")");
                    bracketOff = false;
                    continue;
                }
            }
            calculatorEditText.append(s.toString());
        }
        Log.d(TAG, "formula : " + formulaToString(this.formula));
    }

    private boolean isBracket(char c) {
        return '(' == c || ')' == c;
    }

    private boolean isMod(char c){
        return '%' == c;
    }
    public void clearText(View view) {
        calculatorEditText.setText("");
        lastIndex = -1;
        operandsCount = 0;
        priority = 0;
        isBracketOn = false;
        leftBracketNum = 0;
        rightBracketNum = 0;
        formula.clear();
        result.setText("");
    }

    public void addPosiNeg(View view) {
        // case 1 empty text or ( => N or (N
        // case 2: P5 => N5
        // case 3: P5P * => P5P * N
        // case 4: N43N => N43N  should not be converted
        // case 5: (N => (P     N => P

        // case 1 and case 3
        if (lastIndex == -1 || isOperand(formula.get(lastIndex)) || '(' == formula.get(lastIndex)) {
            formula.add('N');
            lastIndex++;
        } else if (isNumberOrDot(formula.get(lastIndex))) {
            // case 2
            for (int i = lastIndex - 1; i >= 0; i--) {
                if (formula.get(i) == 'P') {
                    formula.set(i, 'N');
                    break;
                } else if (formula.get(i) == 'N') {
                    formula.set(i, 'P');
                    break;
                }
            }
        } else if (isPrefix(formula.get(lastIndex))) {
            // case 5
            if (lastIndex == 0 || (lastIndex > 0 && !isNumberOrDot(formula.get(lastIndex - 1)))) {
                formula.set(lastIndex, formula.get(lastIndex) == 'P' ? 'N' : 'P');
            }
        }
        showText();
        if (isNumberOrDot(formula.get(lastIndex))) {
            calculate(formula);
        }
    }

    public void addBracket(View view) {
        // how to determin functionality when user click this button?
        // cases:
        // case 1:  empty text => (
        // case 2: (P3P + P3 => (P3P + P3P)
        // case 2.1: p4 => p4
        // case 2.2 N44 => N44N
        // case 2.3 N4N => N4N
        // case 3: (P3P + N4 => (P3P + N4N
        // case 4: (P3P + N4N => (P3P + N4N)
        // case 5 : (P3P + N4N) * => (P3P + N4N) * (
        // case 6: (( => (((
        // case 7: ((P5P) => ((P5P))
        // case 7.1: ((P5P)) => ((P5P))
        // case 8: (N => (N
        // case 9: ((P6P-P3P)* => ((P6P-P3P)*(

        // all the (sub)cases are divided into 2 groups, Is bracket on or off

        if (!isBracketOn) {
            // case 1 and case 5
            if (lastIndex == -1 || isOperand(formula.get(lastIndex))) {
                formula.add('(');
                lastIndex++;
                isBracketOn = true;
                leftBracketNum++;
            }
            // case 2.1 and case 2.2
            if (isNumberOrDot(formula.get(lastIndex))) {
                for (int i = lastIndex; i >= 0; i--) {
                    // find the first prefix
                    if (isPrefix(formula.get(i))) {
                        if (formula.get(i) == 'N') {
                            formula.add('N');
                            lastIndex++;
                        }
                        break;
                    }
                }
            }
        } else {
            // case 2 and case 3
            if (isNumberOrDot(formula.get(lastIndex))) {
                addSuffix();
                if ('P' == formula.get(lastIndex)) {
                    formula.add(')');
                    lastIndex++;
                    rightBracketNum++;
                    isBracketOn = leftBracketNum > rightBracketNum;
                }
            } else if ('N' == formula.get(lastIndex)) {
                if (lastIndex > 0 && isNumberOrDot(formula.get(lastIndex - 1))) {
                    // case 4
                    formula.add(')');
                    lastIndex++;
                    rightBracketNum++;
                    isBracketOn = leftBracketNum > rightBracketNum;
                }
                // else case 8
            } else if ('(' == formula.get(lastIndex)) {
                formula.add('(');
                lastIndex++;
                leftBracketNum++;
            } else if (')' == formula.get(lastIndex)) {
                if (isBracketOn) {
                    formula.add(')');
                    lastIndex++;
                    rightBracketNum++;
                    isBracketOn = leftBracketNum > rightBracketNum;
                }
            }else if(isOperand(formula.get(lastIndex))){
                // case 9
                formula.add('(');
                lastIndex++;
                leftBracketNum++;
            }
        }
        showText();
    }

    private void addSuffix() {
        if (lastIndex == -1 || isPrefix(formula.get(lastIndex))) return;
        // case 1:  N423:(-423, when click (), then add N to formula
        for (int i = lastIndex; i >= 0; i--) {
            if (isPrefix(formula.get(i))) {
                char prefix = formula.get(i);
                formula.add(prefix);
                lastIndex++;
                break;
            }
        }
    }

    public void showResult(View view) {
        if (TextUtils.isEmpty(result.getText().toString()) || SECTETMESSAGE.equals(result.getText().toString()) ) {
            long currentClickTime = SystemClock.uptimeMillis();
            long diffTime =  currentClickTime - lastClickTime;
            lastClickTime = currentClickTime;
            if(diffTime < TIMEDIFFERENCE){
                clickNumber++;
                if(clickNumber >= SECTETNUMBERs){
                    result.setText(SECTETMESSAGE);
                }
            }else{
                result.setText("");
                clickNumber = 0 ;
            }
            return;
        }
        String valueStr = result.getText().toString();
        clearText(null);
        boolean isPositive = Double.parseDouble(valueStr) >= 0;
        formula.add(isPositive ? 'P' : 'N');
        lastIndex++;
        for (int i = isPositive ? 0 : 1; i < valueStr.length(); i++) {
            formula.add(valueStr.charAt(i));
            lastIndex++;
        }
        showText();
    }
}
