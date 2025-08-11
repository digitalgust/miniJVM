

package java.security;


public final class AccessControlContext {


    public AccessControlContext(ProtectionDomain context[]) {
    }


    public AccessControlContext(AccessControlContext acc,
                                DomainCombiner combiner) {

    }


    AccessControlContext(ProtectionDomain caller, DomainCombiner combiner,
                         AccessControlContext parent, AccessControlContext context,
                         Permission[] perms) {

    }


    AccessControlContext(ProtectionDomain context[],
                         boolean isPrivileged) {
    }


    AccessControlContext(ProtectionDomain[] context,
                         AccessControlContext privilegedContext) {
    }


    ProtectionDomain[] getContext() {
        return new ProtectionDomain[0];
    }


    boolean isPrivileged() {
        return true;
    }


    public DomainCombiner getDomainCombiner() {
        return null;
    }


    public void checkPermission(Permission perm)
            throws AccessControlException {
    }


    public AccessControlContext optimize() {
        return this;
    }
}
