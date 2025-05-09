package org.mini.gui.gscript;

import org.mini.util.IntList;

/**
 * 自定义数组 Array that define myself <p>Title: </p> <p>Description: </p>
 * <p>Copyright: Copyright (c) 2007</p> <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class Array extends DataType {

    int deepth = 0;
    DataType[] elements;

    /**
     * 构造方法 Construction
     *
     * @param dim int[]
     */
    public Array(IntList dim) {
        type = DTYPE_ARRAY;
        init(dim, 0);
    }

    Array(IntList dim, int dimIndex) {
        type = DTYPE_ARRAY;
        init(dim, dimIndex);
    }

    /**
     * 初始化数组 initial the array
     *
     * @param dim int[]
     */
    private void init(IntList dim, int dimIndex) {

        this.deepth = dim.size() - dimIndex;
        int len = dim.get(dimIndex); //取出这一维数组的长度

        if (deepth > 1) {
            dimIndex++;
            elements = new Array[len];
            for (int i = 0; i < elements.length; i++) { //初始化下层
                elements[i] = new Array(dim, dimIndex);
            }
        } else {
            elements = new DataType[len];
            for (int i = 0; i < len; i++) {
                elements[i] = new Int(0, false);
            }
        }

    }

    /**
     * 取值 get value by pos that int[]{3,2} like return arr[3][2]
     *
     * @param pos int[]
     * @return Object
     */
    public DataType getValue(IntList pos) //throws Exception
    {

        Array arr = this;
        for (int i = 0; i < pos.size(); i++) {
            if (arr.deepth == 1) {
                //if(i>arr.elements.length)throw new Exception(Interpreter.STRS_ERR[Interpreter.ERR_ARR_OUT]);
                return arr.elements[pos.get(i)];
            } else {
                arr = (Array) arr.elements[pos.get(i)];
            }
        }

        return arr;
    }

    /**
     * 设值 set value by postion that int[]{3,2} like return arr[3][2]
     *
     * @param pos   int[]
     * @param value Object
     */
    public DataType setValue(IntList pos, DataType value) //throws Exception
    {
        Array arr = this;
        for (int i = 0; i < pos.size(); i++) {
            if (arr.deepth == 1) {
                //if(i>arr.elements.length)throw new Exception(Interpreter.STRS_ERR[Interpreter.ERR_ARR_OUT]);
                DataType old = arr.elements[pos.get(i)];
                old.setRecyclable(true);
                value.setRecyclable(false);
                arr.elements[pos.get(i)] = (DataType) value;
                return old;
            } else {
                arr = (Array) arr.elements[pos.get(i)];
            }
        }
        return null;
    }

    public String getString() {
        return "Array[" + (elements == null ? "" : elements.length) + "]";
    }

    public String toString() {
        return getString();
    }

    /**
     * 获取指定维度的大小
     *
     * @param dim 维度索引(从0开始)
     * @return 该维度的大小
     */
    public int getDimensionSize(int dim) {
        if (dim == 0) {
            return elements.length;
        } else if (dim < deepth) {
            return ((Array) elements[0]).getDimensionSize(dim - 1);
        }
        return 0;
    }
}
