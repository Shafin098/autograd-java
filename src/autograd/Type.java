package autograd;

public enum Type {
    Created, // when user instantiate a new Var object
    Born     // when new Var object is instantiated by another Var in some operation
}