package javax.swing;

public class JOptionPane {
    void showMessageDialog(java.awt.Component c, Object o, String s, int i) {
        if (Exception.class.isInstance(o))
            ((Exception)o).printStackTrace();
        System.out.println(o);
    }
}
