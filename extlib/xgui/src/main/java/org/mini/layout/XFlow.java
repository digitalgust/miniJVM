/* SPDX-License-Identifier: MIT */
package org.mini.layout;

import org.mini.gui.GToolkit;
import org.mini.gui.callback.GCallBack;
import org.mini.glwrap.GLUtil;
import java.util.ArrayList;
import java.util.List;
import static org.mini.nanovg.Nanovg.*;

/** Internal line boxes shared by layout, label painting and hit testing. */
final class XFlow {
    static final class Part {
        XObject owner;
        byte[] bytes;
        int x, y, w, h, drawX, drawY;
        float top;
    }
    static final class Row {
        final List<Part> parts = new ArrayList<Part>();
        int y, w, h;
    }
    final XContainer parent;
    final List<Row> rows = new ArrayList<Row>();
    Row row = new Row();
    int height, minY;

    XFlow(XContainer parent) { this.parent = parent; }

    void next() {
        next(0f);
    }

    void next(float lineSpacing) {
        rows.add(row);
        Row next = new Row();
        int gap = lineSpacing < 0f ? (int) (lineSpacing - 0.5f) : (int) (lineSpacing + 0.5f);
        next.y = row.y + row.h + gap;
        row = next;
    }

    void object(XObject object) {
        if (object instanceof XBr) {
            row.h = Math.max(row.h, object.height);
            next();
            return;
        }
        if (row.w > 0 && row.w + object.width > parent.viewW) next();
        Part p = new Part();
        p.owner = object;
        p.x = row.w; p.y = row.y; p.w = object.width; p.h = object.height;
        row.parts.add(p);
        row.w += p.w;
        row.h = Math.max(row.h, p.h);
    }

    void text(XLabel label) {
        label.parts = new ArrayList<Part>();
        String text = label.text == null ? "" : label.text;
        long vg = GCallBack.getInstance().getNvContext();
        nvgSave(vg);
        try {
            nvgFontFace(vg, GToolkit.getFontWord());
            nvgFontSize(vg, label.getFontSize());
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
            nvgTextLineHeight(vg, 1f);
            float[] metrics = new float[1], bounds = new float[4];
            nvgTextMetrics(vg, null, null, metrics);
            int pos = 0;
            boolean trailingNewline = false;
            while (pos < text.length()) {
                int end = text.indexOf('\n', pos);
                if (end < 0) end = text.length();
                if (pos == end) {
                    row.h = Math.max(row.h, (int) Math.ceil(metrics[0]) + 2);
                    next(label.lineSpacing); pos++; trailingNewline = true; continue;
                }
                int available = Math.max(1, parent.viewW - row.w);
                int fit = fit(vg, text, pos, end, available);
                if (fit == pos && row.w > 0) { next(); continue; }
                if (fit == pos) fit = nextChar(text, pos);
                if (fit < end && isWord(text.charAt(fit - 1)) && isWord(text.charAt(fit))) {
                    int word = fit;
                    while (word > pos && isWord(text.charAt(word - 1))) word--;
                    if (word > pos) fit = word;
                    else if (row.w > 0) {
                        int wordEnd = fit;
                        while (wordEnd < end && isWord(text.charAt(wordEnd))) wordEnd++;
                        if (advance(vg, text.substring(pos, wordEnd), bounds) <= parent.viewW) {
                            next(); continue;
                        }
                    }
                }
                String fragment = text.substring(pos, fit);
                Part p = new Part();
                p.owner = label; p.bytes = GLUtil.toCstyleBytes(fragment);
                float advance = nvgTextBoundsJni(vg, 0, 0, p.bytes, 0, p.bytes.length - 1, bounds);
                p.x = row.w; p.y = row.y;
                p.w = Math.max(0, (int) Math.ceil(Math.max(advance, bounds[2]) - Math.min(0, bounds[0])));
                p.top = Math.min(0, bounds[1]);
                p.h = (int) Math.ceil(Math.max(metrics[0], bounds[3] - p.top)) + 2;
                label.parts.add(p); row.parts.add(p);
                row.w += p.w; row.h = Math.max(row.h, p.h);
                pos = fit; trailingNewline = false;
                if (pos < end) next(label.lineSpacing);
                else if (pos < text.length()) { next(label.lineSpacing); pos++; trailingNewline = true; }
            }
            if (trailingNewline || text.length() == 0) row.h = Math.max(row.h, (int) Math.ceil(metrics[0]) + 2);
        } finally { nvgRestore(vg); }
    }

    private static boolean isWord(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
    }
    private static int nextChar(String s, int p) {
        return p + (s.charAt(p) >= 0xd800 && s.charAt(p) <= 0xdbff && p + 1 < s.length()
                && s.charAt(p + 1) >= 0xdc00 && s.charAt(p + 1) <= 0xdfff ? 2 : 1);
    }
    private static float advance(long vg, String s, float[] b) {
        byte[] bytes = GLUtil.toCstyleBytes(s);
        return nvgTextBoundsJni(vg, 0, 0, bytes, 0, bytes.length - 1, b);
    }
    private static int fit(long vg, String s, int start, int end, int width) {
        // Bound the search near this line, rather than repeatedly measuring
        // half of an arbitrarily long paragraph. Keep UTF-16 pairs together.
        int lo = start, hi = end, span = 16;
        float[] b = new float[4];
        while (true) {
            int candidate = Math.min(end, start + span);
            if (candidate < end && candidate > start && s.charAt(candidate) >= 0xdc00 && s.charAt(candidate) <= 0xdfff) candidate--;
            float a = advance(vg, s.substring(start, candidate), b);
            if (Math.max(a, b[2]) - Math.min(0, b[0]) > width) { hi = candidate; break; }
            lo = candidate;
            if (candidate == end) return end;
            span = Math.min(end - start, span * 2);
        }
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (mid < end && s.charAt(mid) >= 0xdc00 && s.charAt(mid) <= 0xdfff) mid--;
            if (mid <= lo) mid = nextChar(s, lo);
            float a = advance(vg, s.substring(start, mid), b);
            if (Math.max(a, b[2]) - Math.min(0, b[0]) <= width) lo = mid;
            else {
                hi = mid - 1;
                if (hi > start && s.charAt(hi) >= 0xdc00 && s.charAt(hi) <= 0xdfff) hi--;
            }
        }
        return lo;
    }

    void finish() {
        if (!row.parts.isEmpty() || row.h > 0) rows.add(row);
        int maxY = 0;
        minY = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row current = rows.get(i);
            minY = Math.min(minY, current.y);
            maxY = Math.max(maxY, current.y + current.h);
        }
        height = maxY - minY;
        place(-minY, false);
    }

    void align() {
        int extra = Math.max(0, parent.viewH - height);
        int offset = (parent.align & NVG_ALIGN_BOTTOM) != 0 ? extra
                : (parent.align & NVG_ALIGN_MIDDLE) != 0 ? extra / 2 : 0;
        place(-minY + offset, true);
    }

    private void place(int offset, boolean aligned) {
        for (XObject child : parent.children) {
            if (child instanceof XLabel && ((XLabel) child).parts != null) {
                child.x = child.y = Integer.MAX_VALUE; child.width = child.height = 0;
            }
        }
        for (Row r : rows) {
            int extra = Math.max(0, parent.viewW - r.w);
            int shift = !aligned ? 0 : (parent.align & NVG_ALIGN_RIGHT) != 0 ? extra
                    : (parent.align & NVG_ALIGN_CENTER) != 0 ? extra / 2 : 0;
            for (Part p : r.parts) {
                XObject o = p.owner;
                if (p.bytes != null) {
                    // Positions are regenerated from each row, so alignment is not cumulative.
                    p.drawX = p.x + shift; p.drawY = r.y + offset;
                    o.x = Math.min(o.x, p.drawX); o.y = Math.min(o.y, p.drawY);
                    o.width = Math.max(o.width, p.drawX + p.w);
                    o.height = Math.max(o.height, p.drawY + p.h);
                } else {
                    o.x = p.x + shift; o.y = r.y + offset;
                    if (aligned && (parent.align & NVG_ALIGN_MIDDLE) != 0) o.y += (r.h - o.height) / 2;
                    else if (aligned && (parent.align & NVG_ALIGN_BOTTOM) != 0) o.y += r.h - o.height;
                }
            }
        }
        for (XObject child : parent.children) {
            if (child instanceof XLabel && ((XLabel) child).parts != null) {
                if (child.x == Integer.MAX_VALUE) { child.x = 0; child.y = 0; }
                child.width -= child.x; child.height -= child.y;
                child.viewW = child.width; child.viewH = child.height;
            }
            if (aligned && child.getGui() != null) {
                child.getGui().setLocation(child.x, child.y);
                child.getGui().setSize(child.width, child.height);
            }
            if (aligned && child instanceof XContainer) ((XContainer) child).align();
        }
    }
}
