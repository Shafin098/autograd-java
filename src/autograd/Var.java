package autograd;

import java.util.ArrayList;
import java.util.List;

public class Var {

    private List<Var> roots = new ArrayList<>();
    private Operation operation;
    private Var leftValue;
    private Var rightValue;
    private Var bottomValue; // only used in single operand operation
    private Type variableType;
    private double calculatedValue;
    private double gradient = 0;
    private boolean calculatedAllDerivatives = false;
    private List<Double> derivativeToRoot = new ArrayList<Double>();
    private boolean alreadyInEquationTree = false; // Same variable object should be in equation tree only once

    public Var(double x) {
        this.setVariableType(Type.Created);
        this.setOperation(Operation.None);
        this.setCalculatedValue(x);
    }

    private Var(Operation op, Var leftVar, Var rightVar) {
        this.setVariableType(Type.Born);
        this.alreadyInEquationTree = true;
        this.leftValue = leftVar;
        this.rightValue = rightVar;
        switch (op) {
            case Sum:
                this.setOperation(Operation.Sum);
                this.setCalculatedValue(leftVar.getCalculatedValue() + rightVar.getCalculatedValue());
                leftVar.setDerivativeToRoot(1);
                rightVar.setDerivativeToRoot(1);
                break;
            case Sub:
                this.setOperation(Operation.Sub);
                this.setCalculatedValue(leftVar.getCalculatedValue() - rightVar.getCalculatedValue());
                leftVar.setDerivativeToRoot(1);
                rightVar.setDerivativeToRoot(-1);
                break;
            case Mul:
                this.setOperation(Operation.Mul);
                this.setCalculatedValue(leftVar.getCalculatedValue() * rightVar.getCalculatedValue());
                leftVar.setDerivativeToRoot(rightVar.getCalculatedValue());
                rightVar.setDerivativeToRoot(leftVar.getCalculatedValue());
                break;
            case Div:
                this.setOperation(Operation.Div);
                this.setCalculatedValue(leftVar.getCalculatedValue() / rightVar.getCalculatedValue());
                leftVar.setDerivativeToRoot(1 / rightVar.getCalculatedValue());
                rightVar.setDerivativeToRoot(-(leftVar.getCalculatedValue() / Math.pow(rightVar.getCalculatedValue(), 2)));
                break;
            case Pow:
                this.setOperation(Operation.Pow);
                this.setCalculatedValue(Math.pow(leftVar.getCalculatedValue(), rightVar.getCalculatedValue()));
                leftVar.setDerivativeToRoot(rightVar.getCalculatedValue() * Math.pow(leftVar.getCalculatedValue(),
                        rightVar.getCalculatedValue() - 1));
                rightVar.setDerivativeToRoot(Math.log(leftVar.getCalculatedValue()) *
                        Math.pow(leftVar.getCalculatedValue(), rightVar.getCalculatedValue()));
                break;
            default:
                this.setOperation(Operation.None);
                this.setCalculatedValue(rightVar.getCalculatedValue());
                leftVar.setDerivativeToRoot(0);
                rightVar.setDerivativeToRoot(0);
        }
    }

    public Var sum(Var rightVar) {
        Var currentRootVar = new Var(Operation.Sum, this, rightVar);
        this.roots.add(currentRootVar);
        rightVar.roots.add(currentRootVar);
        return currentRootVar;

    }

    public Var sub(Var rightVar) {
        Var currentRootVar = new Var(Operation.Sub, this, rightVar);
        this.roots.add(currentRootVar);
        rightVar.roots.add(currentRootVar);
        return currentRootVar;

    }

    public Var mul(Var rightVar) {
        Var currentRootVar = new Var(Operation.Mul, this, rightVar);
        this.roots.add(currentRootVar);
        rightVar.roots.add(currentRootVar);
        return currentRootVar;

    }

    public Var div(Var rightVar) {
        Var currentRootVar = new Var(Operation.Div, this, rightVar);
        this.roots.add(currentRootVar);
        rightVar.roots.add(currentRootVar);
        return currentRootVar;
    }

    public Var pow(Var rightVar) {
        Var currentRootVar = new Var(Operation.Pow, this, rightVar);
        this.roots.add(currentRootVar);
        rightVar.roots.add(currentRootVar);
        return currentRootVar;
    }

    public Var negative() {
        Var minusV = new Var(this.getCalculatedValue() * -1);
        this.roots.add(minusV);
        this.derivativeToRoot.add(-1.0);
        minusV.bottomValue = this;
        return minusV;
    }


    public double getGradient() {
        if (this.variableType == Type.Created && this.getRoots().size() == 0) {
            return this.gradient;
        }
        if (!calculatedAllDerivatives) {
            Var absoluteRoot = getAbsoluteRoot();
            absoluteRoot.calculatedAllDerivatives = true;
            absoluteRoot.gradient = 1;
            calculatePartialDerivatives(absoluteRoot);
        }
        return this.gradient;
    }

    private void calculatePartialDerivatives(Var absoluteRoot) {
        List<Var> queue = new ArrayList<>();
        if (absoluteRoot.leftValue != null && absoluteRoot.rightValue != null) {
            queue.add(absoluteRoot.leftValue);
            queue.add(absoluteRoot.rightValue);

            Var leftValOfRoot = absoluteRoot.leftValue;
            Var rightValOfRoot = absoluteRoot.rightValue;
            for (int i=0; i < leftValOfRoot.getRoots().size(); i++) {
                if (leftValOfRoot.getRoots().get(i) == absoluteRoot) {
                    double partialDerivative = leftValOfRoot.getDerivativesToRoot().get(i) * absoluteRoot.gradient;
                    leftValOfRoot.gradient += partialDerivative;
                    break;
                }
            }
            for (int i=0; i < rightValOfRoot.getRoots().size(); i++) {
                if (rightValOfRoot.getRoots().get(i) == absoluteRoot) {
                    double partialDerivative = rightValOfRoot.getDerivativesToRoot().get(i) * absoluteRoot.gradient;
                    rightValOfRoot.gradient += partialDerivative;
                    break;
                }
            }
        } else if (absoluteRoot.bottomValue != null) {
            queue.add(absoluteRoot.bottomValue);

            Var bottomValOfRoot = absoluteRoot.bottomValue;
            for (int i=0; i < bottomValOfRoot.getRoots().size(); i++) {
                if (bottomValOfRoot.getRoots().get(i) == absoluteRoot) {
                    double partialDerivative = bottomValOfRoot.getDerivativesToRoot().get(i) * absoluteRoot.gradient;
                    bottomValOfRoot.gradient += partialDerivative;
                    break;
                }
            }
        }

        while (queue.size() != 0) {
            Var v = queue.remove(0);
            if (v.leftValue != null && v.rightValue != null) {
                v.leftValue.updateDownStream(v);
                v.rightValue.updateDownStream(v);
                queue.add(v.leftValue);
                queue.add(v.rightValue);
            } else if (v.bottomValue != null) {
                v.bottomValue.updateDownStream(v);
                queue.add(v.bottomValue);
            }
        }
    }

    private void updateDownStream(Var root) {
        for (int i=0; i < this.getRoots().size(); i++) {
            if (this.getRoots().get(i) == root) {
                this.gradient += this.getDerivativesToRoot().get(i) * root.gradient;
                break;
            }
        }
    }

    private Var getAbsoluteRoot() {
        if (this.roots.size() > 0) {
            Var root = this.roots.get(0);
            return root.getAbsoluteRoot();
        } else {
            return this;
        }
    }

    public List<Var> getRoots() {
        return this.roots;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Var getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(Var leftValue) {
        this.leftValue = leftValue;
    }

    public Var getRightValue() {
        return rightValue;
    }

    public void setRightValue(Var rightValue) {
        this.rightValue = rightValue;
    }

    public double getCalculatedValue() {
        return calculatedValue;
    }

    public List<Double> getDerivativesToRoot() {
        return derivativeToRoot;
    }

    public void setDerivativeToRoot(double derivativeToRoot) {
        this.derivativeToRoot.add(derivativeToRoot);
    }

    private void setCalculatedValue(double calculatedValue) {
        this.calculatedValue = calculatedValue;
    }

    public Type getVariableType() {
        return variableType;
    }

    private void setVariableType(Type variableType) {
        this.variableType = variableType;
    }

    @Override
    public String toString() {
        String printValue = "";
//        printValue += this.getOperation() + "=" + this.calculatedValue + "; ";
        Var l = this.leftValue;
        Var r = this.rightValue;

        if (l == null && r == null) {
            return this.getCalculatedValue() + "";
        } else if (r == null) {
            printValue += l.toString() + "; ";
        } else if (l == null) {
            printValue += r.toString();
        } else {
            printValue += l.toString() + " " + convertOpToSymbol(this.getOperation()) + " " + r.toString();
        }

        return "(" + printValue + ")";
    }

    private String convertOpToSymbol(Operation operation) {
        switch (operation) {
            case Sum:
                return "+";
            case Sub:
                return "-";
            case Mul:
                return "*";
            case Div:
                return "/";
            case Pow:
                return "^";
            default:
                return "no symbol";
        }
    }

    //........slow method of calculating derivative but works......../
//    public double getGradient() {
//        double gradient = 0;
//        for (int i = 0; i < this.root.size(); i++) {
//            gradient += getGradientToRootAt(i);
//        }
//        return gradient;
//    }
//
//    private double getGradientToRootAt(int index) {
//        double gradient = this.getDerivativesToRoot().get(index);
//        Variable root = this.root.get(index);
//        while (root != null) {
//            if (index <= root.getRoots().size()-1) {
//                if (root.getRoots().get(index) == null) break;
//            } else {
//                break;
//            }
//            gradient = gradient * root.getDerivativesToRoot().get(index);
//            root = root.getRoots().get(index);
//        }
//        return gradient;
//    }

}