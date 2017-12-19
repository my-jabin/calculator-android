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

import com.udojava.evalex.Expression;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final int NUMBEROFOPERAND = 15;
    // the index of last number or operand that user input
    private int lastIndex;

    private int operandsCount;
    private boolean isBracketOn = false;
    private int leftBracketNum;
    private int rightBracketNum;

    private long lastClickTime;
    private int clickNumber = 0;
    private static final int SECTETNUMBERs = 5;
    private static long TIMEDIFFERENCE = 500;
    private static final String SECTETMESSAGE = "Made by Yanbin";

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
        calculate();
    }

    private void calculate() {
        BigDecimal finalResult = null;
        Expression expression = new Expression(formateFormula().toString());
        finalResult = expression.setPrecision(11).setRoundingMode(RoundingMode.HALF_UP).eval();

        // erase error
        BigDecimal bd = finalResult.setScale(0, BigDecimal.ROUND_HALF_UP);
        BigDecimal round = new BigDecimal(0.0000000005).setScale(10, BigDecimal.ROUND_HALF_UP);
        if (finalResult.subtract(bd).abs().compareTo(round) <= 0) {
            finalResult = bd;
        }

        this.result.setText(finalResult.stripTrailingZeros().toPlainString());
    }


    public void showOperand(View view) {
        if (lastIndex < 0 || '(' == formula.get(lastIndex)) return;
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
        if ("Mod".equals(input)) {
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

    private StringBuilder formateFormula() {
        StringBuilder sb = new StringBuilder();
        boolean bracketOn = false;
        boolean readingNumber = false;
        for (int i = 0; i < formula.size(); i++) {
            if (isPrefix(formula.get(i))) {
                if ('N' == formula.get(i) && !readingNumber)
                    sb.append("-");
                readingNumber = !readingNumber;
            } else {
                sb.append(formula.get(i));
                if ('(' == formula.get(i)) {
                    bracketOn = true;
                }
                if (')' == formula.get(i)) {
                    bracketOn = false;
                }
            }
        }
        if (bracketOn)
            sb.append(')');
        return sb;
    }

    private boolean isIntegerValue(BigDecimal bd) {
        return bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0;
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

    private String formulaToString(ArrayList<Character> f) {
        if (f == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : f) {
            result.append(c);
        }
        return result.toString();
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
    }

    public void clearText(View view) {
        calculatorEditText.setText("");
        lastIndex = -1;
        operandsCount = 0;
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
            Log.d(TAG, "formula" + formulaToString(this.formula));
            calculate();
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
            } else if (isOperand(formula.get(lastIndex))) {
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
