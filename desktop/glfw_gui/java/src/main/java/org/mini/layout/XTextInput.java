package org.mini.layout;

import org.mini.gui.*;
import org.mini.gui.event.GStateChangeListener;
import org.mini.layout.xmlpull.KXmlParser;

public class XTextInput
        extends XObject implements GStateChangeListener {

    static public final String XML_NAME = "input";

    protected boolean multiLine = false;
    protected boolean edit = true;
    protected boolean password = false;
    protected int style = GTextField.BOX_STYLE_EDIT;
    protected String hint = "";
    protected String union = null;

    GTextObject textInput;


    public XTextInput(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("multiline")) {
            multiLine = "0".equals(attValue) ? false : true;
        } else if (attName.equals("edit")) {
            edit = "0".equals(attValue) ? false : true;
        } else if (attName.equals("style")) {
            if (attValue.equalsIgnoreCase("search")) {
                style = GTextField.BOX_STYLE_SEARCH;
            }
        } else if (attName.equals("hint")) {
            hint = attValue;
        } else if (attName.equals("union")) {
            union = attValue;
        } else if (attName.equals("password")) {
            password = "0".equals(attValue) ? false : true;
        }
    }


    public void parse(KXmlParser parser) throws Exception {
        super.parse(parser);
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps);
        toEndTag(parser, XML_NAME);
    }


    protected int getDefaultWidth(int parentViewW) {
        return parentViewW;
    }

    protected int getDefaultHeight(int parentViewH) {
        return XDef.DEFAULT_COMPONENT_HEIGHT;
    }

    public GObject getGui() {
        try {
            GContainer root = (GContainer) getRoot().getGui();
            if (root != null) {
                GObject unionObj = root.findByName(union);
                if (unionObj != null) {
                    textInput.setUnionObj(unionObj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textInput;
    }

    protected void createGui() {
        if (textInput == null) {
            if (multiLine) {
                textInput = new GTextBox(getText(), hint, x, y, width, height);

            } else {
                textInput = new GTextField(getText(), hint, x, y, width, height);
                ((GTextField) textInput).setBoxStyle(style);
                ((GTextField) textInput).setPasswordMode(password);
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
    public void onStateChange(GObject gobj) {
        getRoot().getEventHandler().onStateChange(gobj, null);
    }
}
