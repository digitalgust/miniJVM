


package java.security;

import java.util.Enumeration;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;


public class Policy {

    static Policy instance = new Policy();


    public static final PermissionCollection UNSUPPORTED_EMPTY_COLLECTION =
            new PermissionCollection() {
                @Override
                public void add(Permission p) {

                }
            };


    private static class PolicyInfo {

        final Policy policy;

        final boolean initialized;

        PolicyInfo(Policy policy, boolean initialized) {
            this.policy = policy;
            this.initialized = initialized;
        }
    }


    private static AtomicReference<PolicyInfo> policy =
            new AtomicReference<>(new PolicyInfo(null, false));


    public static Policy getPolicy() {
        return instance;
    }


    public static void setPolicy(Policy p) {
        if (p != null) {
            initPolicy(p);
        }
        synchronized (Policy.class) {
            policy.set(new PolicyInfo(p, p != null));
        }
    }


    private static void initPolicy(final Policy p) {


    }


    public static Policy getInstance(String type, Parameters params)
            throws NoSuchAlgorithmException {

        return instance;
    }


    public static Policy getInstance(String type,
                                     Parameters params,
                                     String provider)
            throws NoSuchProviderException, NoSuchAlgorithmException {

        return instance;
    }


    public static Policy getInstance(String type,
                                     Parameters params,
                                     Provider provider)
            throws NoSuchAlgorithmException {

        return instance;
    }


    public Provider getProvider() {
        return null;
    }


    public String getType() {
        return null;
    }


    public Parameters getParameters() {
        return null;
    }


    public PermissionCollection getPermissions(CodeSource codesource) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }


    public PermissionCollection getPermissions(ProtectionDomain domain) {
        return null;
    }


    private void addStaticPerms(PermissionCollection perms,
                                PermissionCollection statics) {
    }


    public boolean implies(ProtectionDomain domain, Permission permission) {
        return true;
    }


    public void refresh() {
    }


    public static interface Parameters {
    }


}
