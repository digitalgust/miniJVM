/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.layout;

import org.mini.gui.*;
import org.mini.layout.gscript.*;

import java.util.Vector;


/**
 * xml ui script libary
 *
 * @author Gust
 */
public class ScriptLib extends Lib {

    XContainer root = null;

    {
        methodNames.put("setBgColor".toLowerCase(), 0);//  set background color
        methodNames.put("setColor".toLowerCase(), 1);//  set background color
        methodNames.put("setText".toLowerCase(), 2);//  set text
        methodNames.put("getText".toLowerCase(), 3);//  get text
        methodNames.put("setLocation".toLowerCase(), 4);//
        methodNames.put("setSize".toLowerCase(), 5);//
        methodNames.put("getLocation".toLowerCase(), 6);//
        methodNames.put("getSize".toLowerCase(), 7);//
        methodNames.put("setCmd".toLowerCase(), 8);//
        methodNames.put("getCmd".toLowerCase(), 9);//
        methodNames.put("close".toLowerCase(), 10);//  close frame
        methodNames.put("getCurSlot".toLowerCase(), 11);//
        methodNames.put("showSlot".toLowerCase(), 12);//
        methodNames.put("getImg".toLowerCase(), 13);//
        methodNames.put("setImg".toLowerCase(), 14);//
    }

    ;

    /**
     * @param xf
     */
    public ScriptLib(XContainer xf) {
        root = xf;
    }

    /**
     * @param inp
     * @param para
     * @param methodID
     * @return
     */
    public DataType call(Interpreter inp, Vector para, int methodID) {
        switch (methodID) {
            case 0:
                return setBgColor(para);
            case 1:
                return setColor(para);
            case 2:
                return setText(para);
            case 3:
                return getText(para);
            case 4:
                return setLocation(para);
            case 5:
                return setSize(para);
            case 6:
                return getLocation(para);
            case 7:
                return getSize(para);
            case 8:
                return setCmd(para);
            case 9:
                return getCmd(para);
            case 10:
                return close(para);
            case 11:
                return getCurSlot(para);
            case 12:
                return showSlot(para);
            case 13:
                return getImg(para);
            case 14:
                return setImg(para);
            default:
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // inner method
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // implementation
    // -------------------------------------------------------------------------


    public DataType setBgColor(Vector para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                GObject go = xo.getGui();
                if (go != null) {
                    int r = ((Int) Interpreter.vPopBack(para)).getVal();
                    int g = ((Int) Interpreter.vPopBack(para)).getVal();
                    int b = ((Int) Interpreter.vPopBack(para)).getVal();
                    int a = ((Int) Interpreter.vPopBack(para)).getVal();
                    go.setBgColor(r, g, b, a);
                }
            }
        }
        return null;
    }

    public DataType setColor(Vector para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                GObject go = xo.getGui();
                if (go != null) {
                    int r = ((Int) Interpreter.vPopBack(para)).getVal();
                    int g = ((Int) Interpreter.vPopBack(para)).getVal();
                    int b = ((Int) Interpreter.vPopBack(para)).getVal();
                    int a = ((Int) Interpreter.vPopBack(para)).getVal();
                    go.setColor(r, g, b, a);
                }
            }
        }
        return null;
    }


    public DataType setText(Vector para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            String text = ((Str) Interpreter.vPopBack(para)).getVal();
            if (xo != null) {
                xo.setText(text);
                GObject go = xo.getGui();
                if (go != null) {
                    go.setText(text);
                }
            }
        }
        return null;
    }

    public DataType getText(Vector para) {
        String text = "";
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                GObject go = xo.getGui();
                if (go != null) {
                    text = go.getText();
                }
            }
        }
        return new Str(text);
    }

    public DataType setLocation(Vector para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                GObject go = xo.getGui();
                if (go != null) {
                    int x = ((Int) Interpreter.vPopBack(para)).getVal();
                    int y = ((Int) Interpreter.vPopBack(para)).getVal();
                    go.setLocation(x, y);
                }
            }
        }
        return null;
    }

    public DataType setSize(Vector para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                GObject go = xo.getGui();
                if (go != null) {
                    int w = ((Int) Interpreter.vPopBack(para)).getVal();
                    int h = ((Int) Interpreter.vPopBack(para)).getVal();
                    go.setSize(w, h);
                }
            }
        }
        return null;
    }


    public DataType getLocation(Vector para) {
        Array array = new Array(new int[]{2});
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                GObject go = xo.getGui();
                if (go != null) {
                    array.setValue(new int[]{0}, new Int((int) go.getLocationLeft()));
                    array.setValue(new int[]{1}, new Int((int) go.getLocationTop()));
                }
            }
        }
        return array;
    }

    public DataType getSize(Vector para) {
        Array array = new Array(new int[]{2});
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                GObject go = xo.getGui();
                if (go != null) {
                    array.setValue(new int[]{0}, new Int((int) go.getW()));
                    array.setValue(new int[]{1}, new Int((int) go.getH()));
                }
            }
        }
        return array;
    }

    public DataType setCmd(Vector para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                String cmd = ((Str) Interpreter.vPopBack(para)).getVal();
                xo.setCmd(cmd);
            }
        }
        return null;
    }

    public DataType getCmd(Vector para) {
        String text = "";
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                text = xo.getCmd();
            }
        }
        return new Str(text);
    }

    public DataType close(Vector para) {
        GObject go = null;
        Str p1 = (Str) Interpreter.vPopBack(para);
        String compont = p1 != null ? p1.getVal() : null;
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                go = xo.getGui();

            }
        } else {
            go = root.getGui();
        }
        if (go instanceof GFrame) {
            ((GFrame) go).close();
        }
        return null;
    }

    public DataType getCurSlot(Vector para) {
        Int val = new Int(0);
        Str p1 = (Str) Interpreter.vPopBack(para);
        String compont = p1 != null ? p1.getVal() : null;
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                GObject go = xo.getGui();
                if (go instanceof GViewSlot) {
                    val.setVal(((GViewSlot) go).getCurrentSlot());
                }

            }
        }
        return val;
    }

    public DataType showSlot(Vector para) {
        Str p1 = (Str) Interpreter.vPopBack(para);
        String compont = p1 != null ? p1.getVal() : null;
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                GObject go = xo.getGui();
                int slot = ((Int) Interpreter.vPopBack(para)).getVal();
                Int time = ((Int) Interpreter.vPopBack(para));
                if (go instanceof GViewSlot) {
                    ((GViewSlot) go).moveTo(slot, time == null ? 200 : time.getVal());
                }
            }
        }
        return null;
    }


    public DataType setImg(Vector para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo != null) {
                String img = ((Str) Interpreter.vPopBack(para)).getVal();
                if (xo instanceof XImageItem) {
                    XImageItem xi = ((XImageItem) xo);
                    xi.setPic(img);

                }
            }
        }
        return null;
    }

    public DataType getImg(Vector para) {
        String text = "";
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        if (compont != null) {
            XObject xo = root.find(compont);
            if (xo instanceof XImageItem) {
                XImageItem xi = ((XImageItem) xo);
                text = xi.getPic();
            }
        }
        return new Str(text);
    }
}
