package org.mini.layout.gscript;

/**
 * 自定义数组 Array that define myself <p>Title: </p> <p>Description: </p>
 * <p>Copyright: Copyright (c) 2007</p> <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class Array extends DataType {

    byte deepth = 0;
    DataType[] elements;

    /**
     * 构造方法 Construction
     *
     * @param dimSize int[]
     */
    public Array(int[] dimSize) {
        type = DTYPE_ARRAY;
        init(dimSize);
    }

    /**
     * 初始化数组 initial the array
     *
     * @param dimSize int[]
     */
    private void init(int[] dimSize) {

        this.deepth = (byte) dimSize.length;
        int len = dimSize[0]; //取出这一维数组的长度

        if (deepth > 1) {
            int[] newDimSize = new int[deepth - 1];
            System.arraycopy(dimSize, 1, newDimSize, 0, deepth - 1);
            elements = new Array[len];
            for (int i = 0; i < ((Array[]) elements).length; i++) { //初始化下层
                elements[i] = new Array(newDimSize);
            }
        } else {
            elements = new DataType[len];
            for (int i = 0; i < len; i++) {
                elements[i] = new Int(0);
            }
        }

    }

    /**
     * 取值 get value by pos that int[]{3,2} like return arr[3][2]
     *
     * @param pos int[]
     * @return Object
     */
    public DataType getValue(int[] pos) //throws Exception
    {

        Array arr = this;
        for (int i = 0; i < pos.length; i++) {
            if (arr.deepth == 1) {
                //if(i>arr.elements.length)throw new Exception(Interpreter.STRS_ERR[Interpreter.ERR_ARR_OUT]);
                return arr.elements[pos[i]];
            } else {
                arr = (Array) arr.elements[pos[i]];
            }
        }

        return arr;
    }

    /**
     * 设值 set value by postion that int[]{3,2} like return arr[3][2]
     *
     * @param pos int[]
     * @param value Object
     */
    public void setValue(int[] pos, DataType value) //throws Exception
    {
        Array arr = this;
        for (int i = 0; i < pos.length; i++) {
            if (arr.deepth == 1) {
                //if(i>arr.elements.length)throw new Exception(Interpreter.STRS_ERR[Interpreter.ERR_ARR_OUT]);

                arr.elements[pos[i]] = (DataType) value;
            } else {
                arr = (Array) arr.elements[pos[i]];
            }
        }
    }

    public String getString() {
        return "Array[" + elements == null ? "" : elements.length + "]";
    }

    public String toString() {
        return getString();
    }
}
