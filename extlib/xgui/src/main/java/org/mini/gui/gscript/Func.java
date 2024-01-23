package org.mini.gui.gscript;

import java.util.ArrayList;

public interface Func<T extends DataType> {
    T run(ArrayList<DataType> para);
}
