import autograd.Var;

public class AutogradTest {

    public static void main(String[] args) {
        Var x = new Var(2);
        Var y = new Var(4);
//        Var minusX = x.mul(new Var(-1));
//        Var e_pow_minusX = y.pow(x.mul(new Var(-1)));
//        Var z = x.div(new Var(1).sum(e_pow_minusX));
        Var z = y.pow(x.negative().mul(new Var(2)));
        System.out.println(z.getCalculatedValue());
        System.out.println(y.getGradient());
    }
}