package org.mini.xmlui;

import org.mini.gui.GObject;
import org.mini.gui.GTextBox;
import org.mini.gui.GTextField;
import org.mini.gui.GTextObject;
import org.mini.gui.event.GKeyboardShowListener;
import org.mini.gui.event.GStateChangeListener;
import org.mini.xmlui.xmlpull.KXmlParser;

public class XTextInput
        extends XObject implements GKeyboardShowListener, GStateChangeListener {

    static public final String XML_NAME = "input";

    boolean multiLine = false;
    boolean edit = true;
    int style = GTextField.BOX_STYLE_EDIT;
    String hint = "";

    GTextObject textInput;


    public XTextInput(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("multiline")) {
            if (attValue != null) {
                int v = 0;
                try {
                    v = Integer.parseInt(attValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                multiLine = v == 0 ? false : true;
            }
        } else if (attName.equals("edit")) {
            if (attValue != null) {
                int v = 0;
                try {
                    v = Integer.parseInt(attValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                edit = v == 0 ? false : true;
            }
        } else if (attName.equals("style")) {
            if (attValue.equalsIgnoreCase("search")) {
                style = GTextField.BOX_STYLE_SEARCH;
            }
        } else if (attName.equals("hint")) {
            hint = attValue;
        }
    }


    public void parse(KXmlParser parser) throws Exception {
        super.parse(parser);
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps);
        toEndTag(parser, XML_NAME);
    }


    void preAlignVertical() {
        if (height == XDef.NODEF) {
            if (raw_heightPercent != XDef.NODEF && parent.viewH != XDef.NODEF) {
                viewH = height = raw_heightPercent * parent.viewH / 100;
            } else {
                viewH = height = XDef.DEFAULT_COMPONENT_HEIGHT;
            }
        }
    }

    void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                viewW = width = parent.viewW;
            } else {
                viewW = width = raw_widthPercent * parent.viewW / 100;
            }
        }
    }

    public GObject getGui() {
        return textInput;
    }

    void createGui() {
        if (textInput == null) {
            if (multiLine) {
                textInput = new GTextBox(getText(), hint, x, y, width, height);

            } else {
                textInput = new GTextField(getText(), hint, x, y, width, height);
                ((GTextField) textInput).setBoxStyle(style);
            }
            textInput.setName(name);
            textInput.setAttachment(this);
            //textInput.getForm().setKeyshowListener(this);
            textInput.setStateChangeListener(this);
            textInput.setEditable(edit);
        } else {
            textInput.setLocation(x, y);
            textInput.setSize(width, height);
        }
    }

    @Override
    public void keyboardShow(boolean show, float x, float y, float w, float h) {
        getRoot().getEventHandler().keyboardShow(show, x, y, w, h);
    }

    @Override
    public void onStateChange(GObject gobj) {
        getRoot().getEventHandler().onStateChange(gobj, null);
    }
}
