package org.mini.layout;

import org.mini.gui.*;
import org.mini.json.JsonParser;
import org.mini.layout.loader.XmlExtAssist;
import org.xmlpull.v1.KXmlParser;

import java.util.List;

public class XTextInput
        extends XObject {

    static public final String XML_NAME = "input";

    protected boolean multiLine = false;
    protected boolean edit = true;
    protected boolean reset = true;
    protected boolean password = false;
    protected boolean scrollbar = false;
    protected int style = GTextField.BOX_STYLE_EDIT;
    protected String hint = "";
    protected String union = null;

    protected List<GTextBox.StyleRun> stylerun = null;

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
            multiLine = !"0".equals(attValue);
        } else if (attName.equals("edit")) {
            edit = !"0".equals(attValue);
        } else if (attName.equals("reset")) {
            reset = !"0".equals(attValue);
        } else if (attName.equals("style")) {
            if (attValue.equalsIgnoreCase("search")) {
                style = GTextField.BOX_STYLE_SEARCH;
            }
        } else if (attName.equals("hint")) {
            hint = attValue;
        } else if (attName.equals("union")) {
            union = attValue;
        } else if (attName.equals("password")) {
            password = !"0".equals(attValue);
        } else if (attName.equals("scroll")) {
            scrollbar = !"0".equals(attValue);
        } else if (attName.equals("stylerun")) {
            try {
                JsonParser<List<GTextBox.StyleRun>> jsonParser = new JsonParser<>();
                stylerun = jsonParser.deserial(attValue, List.class, null, "java.util.List<org.mini.gui.GTextBox$StyleRun>");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        super.parse(parser, assist);
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps.trim());
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

    protected <T extends GObject> T createGuiImpl() {
        if (multiLine) {
            return (T) new GTextBox(getAssist().getForm(), getText(), hint, x, y, width, height);
        } else {
            return (T) new GTextField(getAssist().getForm(), getText(), hint, x, y, width, height);
        }
    }

    protected void createAndSetGui() {
        if (textInput == null) {
            textInput = createGuiImpl();
            if (!multiLine) {
                textInput = createGuiImpl();
                ((GTextField) textInput).setBoxStyle(style);
                ((GTextField) textInput).setPasswordMode(password);
                ((GTextField) textInput).setResetEnable(reset);
            }
            initGuiMore();
            textInput.setEnable(enable);
            textInput.setEditable(edit);
            textInput.setScrollBar(scrollbar);
            if (stylerun != null && textInput instanceof GTextBox) {
                ((GTextBox) textInput).setStyles(stylerun);
            }
        } else {
            textInput.setLocation(x, y);
            textInput.setSize(width, height);
        }
    }

}
