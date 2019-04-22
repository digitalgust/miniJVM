package java.lang.invoke;

public class CallSite {

    MethodHandle target;

    CallSite(MethodHandle target) {
        this.target = target;
    }

    public MethodHandle getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(MethodHandle target) {
        this.target = target;
    }

    public MethodType type() {
        if (target != null) {
            return target.type();
        }
        return null;
    }
}
