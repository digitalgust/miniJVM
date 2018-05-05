//
// Created by Gust on 2018/1/30.
//

#ifndef JNI_GUI_NANOVG_JNI_ASSIST_H
#define JNI_GUI_NANOVG_JNI_ASSIST_H

// dont delete comment , this header file for generate org.mini.nanovg.Nanovg.java , and Nanovg.java generate jni_nanovg.c
/*

 //nanovg.h

enum NVGwinding {
	NVG_CCW = 1,			// Winding for solid shapes
	NVG_CW = 2,				// Winding for holes
};

enum NVGsolidity {
	NVG_SOLID = 1,			// CCW
	NVG_HOLE = 2,			// CW
};

enum NVGlineCap {
	NVG_BUTT,
	NVG_ROUND,
	NVG_SQUARE,
	NVG_BEVEL,
	NVG_MITER,
};

enum NVGalign {
	// Horizontal align
	NVG_ALIGN_LEFT 		= 1<<0,	// Default, align text horizontally to left.
	NVG_ALIGN_CENTER 	= 1<<1,	// Align text horizontally to center.
	NVG_ALIGN_RIGHT 	= 1<<2,	// Align text horizontally to right.
	// Vertical align
	NVG_ALIGN_TOP 		= 1<<3,	// Align text vertically to top.
	NVG_ALIGN_MIDDLE	= 1<<4,	// Align text vertically to middle.
	NVG_ALIGN_BOTTOM	= 1<<5,	// Align text vertically to bottom.
	NVG_ALIGN_BASELINE	= 1<<6, // Default, align text vertically to baseline.
};

enum NVGblendFactor {
	NVG_ZERO = 1<<0,
	NVG_ONE = 1<<1,
	NVG_SRC_COLOR = 1<<2,
	NVG_ONE_MINUS_SRC_COLOR = 1<<3,
	NVG_DST_COLOR = 1<<4,
	NVG_ONE_MINUS_DST_COLOR = 1<<5,
	NVG_SRC_ALPHA = 1<<6,
	NVG_ONE_MINUS_SRC_ALPHA = 1<<7,
	NVG_DST_ALPHA = 1<<8,
	NVG_ONE_MINUS_DST_ALPHA = 1<<9,
	NVG_SRC_ALPHA_SATURATE = 1<<10,
};

enum NVGcompositeOperation {
	NVG_SOURCE_OVER,
	NVG_SOURCE_IN,
	NVG_SOURCE_OUT,
	NVG_ATOP,
	NVG_DESTINATION_OVER,
	NVG_DESTINATION_IN,
	NVG_DESTINATION_OUT,
	NVG_DESTINATION_ATOP,
	NVG_LIGHTER,
	NVG_COPY,
	NVG_XOR,
};

NUTIL_API void nvgBeginFrame(NVGcontext* ctx, int windowWidth, int windowHeight, float devicePixelRatio);
NUTIL_API void nvgCancelFrame(NVGcontext* ctx);
NUTIL_API void nvgEndFrame(NVGcontext* ctx);
NUTIL_API void nvgGlobalCompositeOperation(NVGcontext* ctx, int op);
NUTIL_API void nvgGlobalCompositeBlendFunc(NVGcontext* ctx, int sfactor, int dfactor);
NUTIL_API void nvgGlobalCompositeBlendFuncSeparate(NVGcontext* ctx, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);
NUTIL_API NVGcolor nvgRGB(unsigned char r, unsigned char g, unsigned char b);
NUTIL_API NVGcolor nvgRGBf(float r, float g, float b);
NUTIL_API NVGcolor nvgRGBA(unsigned char r, unsigned char g, unsigned char b, unsigned char a);
NUTIL_API NVGcolor nvgRGBAf(float r, float g, float b, float a);
NUTIL_API NVGcolor nvgLerpRGBA(NVGcolor c0, NVGcolor c1, float u);
NUTIL_API NVGcolor nvgTransRGBA(NVGcolor c0, unsigned char a);
NUTIL_API NVGcolor nvgTransRGBAf(NVGcolor c0, float a);
NUTIL_API NVGcolor nvgHSL(float h, float s, float l);
NUTIL_API NVGcolor nvgHSLA(float h, float s, float l, unsigned char a);
NUTIL_API void nvgSave(NVGcontext* ctx);
NUTIL_API void nvgRestore(NVGcontext* ctx);
NUTIL_API void nvgReset(NVGcontext* ctx);
NUTIL_API void nvgShapeAntiAlias(NVGcontext* ctx, int enabled);
NUTIL_API void nvgStrokeColor(NVGcontext* ctx, NVGcolor color);
NUTIL_API void nvgStrokePaint(NVGcontext* ctx, NVGpaint paint);
NUTIL_API void nvgFillColor(NVGcontext* ctx, NVGcolor color);
NUTIL_API void nvgFillPaint(NVGcontext* ctx, NVGpaint paint);
NUTIL_API void nvgMiterLimit(NVGcontext* ctx, float limit);
NUTIL_API void nvgStrokeWidth(NVGcontext* ctx, float size);
NUTIL_API void nvgLineCap(NVGcontext* ctx, int cap);
NUTIL_API void nvgLineJoin(NVGcontext* ctx, int join);
NUTIL_API void nvgGlobalAlpha(NVGcontext* ctx, float alpha);
NUTIL_API void nvgResetTransform(NVGcontext* ctx);
NUTIL_API void nvgTransform(NVGcontext* ctx, float a, float b, float c, float d, float e, float f);
NUTIL_API void nvgTranslate(NVGcontext* ctx, float x, float y);
NUTIL_API void nvgRotate(NVGcontext* ctx, float angle);
NUTIL_API void nvgSkewX(NVGcontext* ctx, float angle);
NUTIL_API void nvgSkewY(NVGcontext* ctx, float angle);
NUTIL_API void nvgScale(NVGcontext* ctx, float x, float y);
NUTIL_API void nvgCurrentTransform(NVGcontext* ctx, float* xform);
NUTIL_API void nvgTransformIdentity(float* dst);
NUTIL_API void nvgTransformTranslate(float* dst, float tx, float ty);
NUTIL_API void nvgTransformScale(float* dst, float sx, float sy);
NUTIL_API void nvgTransformRotate(float* dst, float a);
NUTIL_API void nvgTransformSkewX(float* dst, float a);
NUTIL_API void nvgTransformSkewY(float* dst, float a);
NUTIL_API void nvgTransformMultiply(float* dst, const float* src);
NUTIL_API void nvgTransformPremultiply(float* dst, const float* src);
NUTIL_API int nvgTransformInverse(float* dst, const float* src);
NUTIL_API void nvgTransformPoint(float* dstx, float* dsty, const float* xform, float srcx, float srcy);
NUTIL_API float nvgDegToRad(float deg);
NUTIL_API float nvgRadToDeg(float rad);
NUTIL_API int nvgCreateImage(NVGcontext* ctx, const char* filename, int imageFlags);
NUTIL_API int nvgCreateImageMem(NVGcontext* ctx, int imageFlags, unsigned char* data, int ndata);
NUTIL_API int nvgCreateImageRGBA(NVGcontext* ctx, int w, int h, int imageFlags, const unsigned char* data);
NUTIL_API void nvgUpdateImage(NVGcontext* ctx, int image, const unsigned char* data);
NUTIL_API void nvgImageSize(NVGcontext* ctx, int image, int* w, int* h);
NUTIL_API void nvgDeleteImage(NVGcontext* ctx, int image);
NUTIL_API NVGpaint nvgLinearGradient(NVGcontext* ctx, float sx, float sy, float ex, float ey, NVGcolor icol, NVGcolor ocol);
NUTIL_API NVGpaint nvgBoxGradient(NVGcontext* ctx, float x, float y, float w, float h, float r, float f, NVGcolor icol, NVGcolor ocol);
NUTIL_API NVGpaint nvgRadialGradient(NVGcontext* ctx, float cx, float cy, float inr, float outr, NVGcolor icol, NVGcolor ocol);
NUTIL_API NVGpaint nvgImagePattern(NVGcontext* ctx, float ox, float oy, float ex, float ey, float angle, int image, float alpha);
NUTIL_API void nvgScissor(NVGcontext* ctx, float x, float y, float w, float h);
NUTIL_API void nvgIntersectScissor(NVGcontext* ctx, float x, float y, float w, float h);
NUTIL_API void nvgResetScissor(NVGcontext* ctx);
NUTIL_API void nvgBeginPath(NVGcontext* ctx);
NUTIL_API void nvgMoveTo(NVGcontext* ctx, float x, float y);
NUTIL_API void nvgLineTo(NVGcontext* ctx, float x, float y);
NUTIL_API void nvgBezierTo(NVGcontext* ctx, float c1x, float c1y, float c2x, float c2y, float x, float y);
NUTIL_API void nvgQuadTo(NVGcontext* ctx, float cx, float cy, float x, float y);
NUTIL_API void nvgArcTo(NVGcontext* ctx, float x1, float y1, float x2, float y2, float radius);
NUTIL_API void nvgClosePath(NVGcontext* ctx);
NUTIL_API void nvgPathWinding(NVGcontext* ctx, int dir);
NUTIL_API void nvgArc(NVGcontext* ctx, float cx, float cy, float r, float a0, float a1, int dir);
NUTIL_API void nvgRect(NVGcontext* ctx, float x, float y, float w, float h);
NUTIL_API void nvgRoundedRect(NVGcontext* ctx, float x, float y, float w, float h, float r);
NUTIL_API void nvgRoundedRectVarying(NVGcontext* ctx, float x, float y, float w, float h, float radTopLeft, float radTopRight, float radBottomRight, float radBottomLeft);
NUTIL_API void nvgEllipse(NVGcontext* ctx, float cx, float cy, float rx, float ry);
NUTIL_API void nvgCircle(NVGcontext* ctx, float cx, float cy, float r);
NUTIL_API void nvgFill(NVGcontext* ctx);
NUTIL_API void nvgStroke(NVGcontext* ctx);
NUTIL_API int nvgCreateFont(NVGcontext* ctx, const char* name, const char* filename);
NUTIL_API int nvgCreateFontMem(NVGcontext* ctx, const char* name, unsigned char* data, int ndata, int freeData);
NUTIL_API int nvgFindFont(NVGcontext* ctx, const char* name);
NUTIL_API int nvgAddFallbackFontId(NVGcontext* ctx, int baseFont, int fallbackFont);
NUTIL_API int nvgAddFallbackFont(NVGcontext* ctx, const char* baseFont, const char* fallbackFont);
NUTIL_API void nvgFontSize(NVGcontext* ctx, float size);
NUTIL_API void nvgFontBlur(NVGcontext* ctx, float blur);
NUTIL_API void nvgTextLetterSpacing(NVGcontext* ctx, float spacing);
NUTIL_API void nvgTextLineHeight(NVGcontext* ctx, float lineHeight);
NUTIL_API void nvgTextAlign(NVGcontext* ctx, int align);
NUTIL_API void nvgFontFaceId(NVGcontext* ctx, int font);
NUTIL_API void nvgFontFace(NVGcontext* ctx, const char* font);
//NUTIL_API float nvgText(NVGcontext* ctx, float x, float y, const char* string, const char* end);
//NUTIL_API void nvgTextBox(NVGcontext* ctx, float x, float y, float breakRowWidth, const char* string, const char* end);
//NUTIL_API float nvgTextBounds(NVGcontext* ctx, float x, float y, const char* string, const char* end, float* bounds);
//NUTIL_API void nvgTextBoxBounds(NVGcontext* ctx, float x, float y, float breakRowWidth, const char* string, const char* end, float* bounds);
//NUTIL_API int nvgTextGlyphPositions(NVGcontext* ctx, float x, float y, const char* string, const char* end, NVGglyphPosition* positions, int maxPositions);
NUTIL_API void nvgTextMetrics(NVGcontext* ctx, float* ascender, float* descender, float* lineh);
//NUTIL_API int nvgTextBreakLines(NVGcontext* ctx, const char* string, const char* end, float breakRowWidth, NVGtextRow* rows, int maxRows);

 //nanovg_gl.h
enum NVGcreateFlags {
	// Flag indicating if geometry based anti-aliasing is used (may not be needed when using MSAA).
	NVG_ANTIALIAS 		= 1<<0,
	// Flag indicating if strokes should be drawn using stencil buffer. The rendering will be a little
	// slower, but path overlaps (i.e. self-intersecting or sharp turns) will be drawn just once.
	NVG_STENCIL_STROKES	= 1<<1,
	// Flag indicating that additional debug checks are done.
	NVG_DEBUG 			= 1<<2,
};

NUTIL_API NVGcontext* nvgCreateGLES2(int flags);
NUTIL_API void nvgDeleteGLES2(NVGcontext* ctx);
NUTIL_API int nvglCreateImageFromHandleGLES2(NVGcontext* ctx, GLuint textureId, int w, int h, int flags);
NUTIL_API GLuint nvglImageHandleGLES2(NVGcontext* ctx, int image);

//NUTIL_API NVGcontext* nvgCreateGL2(int flags);
//NUTIL_API void nvgDeleteGL2(NVGcontext* ctx);
//NUTIL_API int nvglCreateImageFromHandleGL2(NVGcontext* ctx, GLuint textureId, int w, int h, int flags);
//NUTIL_API GLuint nvglImageHandleGL2(NVGcontext* ctx, int image);


//NUTIL_API NVGcontext* nvgCreateGL3(int flags);
//NUTIL_API void nvgDeleteGL3(NVGcontext* ctx);
//NUTIL_API int nvglCreateImageFromHandleGL3(NVGcontext* ctx, GLuint textureId, int w, int h, int flags);
//NUTIL_API GLuint nvglImageHandleGL3(NVGcontext* ctx, int image);


 //nanovg_gl3.h

enum GLNVGuniformLoc {
	GLNVG_LOC_VIEWSIZE,
	GLNVG_LOC_SCISSORMAT,
	GLNVG_LOC_SCISSOREXT,
	GLNVG_LOC_SCISSORSCALE,
	GLNVG_LOC_PAINTMAT,
	GLNVG_LOC_EXTENT,
	GLNVG_LOC_RADIUS,
	GLNVG_LOC_FEATHER,
	GLNVG_LOC_INNERCOL,
	GLNVG_LOC_OUTERCOL,
	GLNVG_LOC_STROKEMULT,
	GLNVG_LOC_TEX,
	GLNVG_LOC_TEXTYPE,
	GLNVG_LOC_TYPE,
	GLNVG_MAX_LOCS
};

enum GLNVGshaderType {
	NSVG_SHADER_FILLGRAD,
	NSVG_SHADER_FILLIMG,
	NSVG_SHADER_SIMPLE,
	NSVG_SHADER_IMG
};


NUTIL_API struct NVGtextRow *nvgCreateNVGtextRow(int count);
NUTIL_API void nvgDeleteNVGtextRow(struct NVGtextRow *val);
NUTIL_API float nvgNVGtextRow_width(struct NVGtextRow *ptr, int index);
NUTIL_API void *nvgNVGtextRow_start(struct NVGtextRow *ptr, int index);
NUTIL_API void *nvgNVGtextRow_end(struct NVGtextRow *ptr, int index);
NUTIL_API void *nvgNVGtextRow_next(struct NVGtextRow *ptr, int index);
NUTIL_API struct NVGglyphPosition *nvgCreateNVGglyphPosition(int count);
NUTIL_API void nvgDeleteNVGglyphPosition(struct NVGglyphPosition *val) ;
NUTIL_API float nvgNVGglyphPosition_x(struct NVGglyphPosition *ptr, int count);
NUTIL_API float nvgTextJni(NVGcontext* ctx, float x, float y, const char* string, int start, int end);
NUTIL_API void nvgTextBoxJni(NVGcontext* ctx, float x, float y, float breakRowWidth, const char* string, int start, int end);
NUTIL_API float nvgTextBoundsJni(NVGcontext* ctx, float x, float y, const char* string, int start, int end, float* bounds);
NUTIL_API void nvgTextBoxBoundsJni(NVGcontext* ctx, float x, float y, float breakRowWidth, const char* string, int start, int end, float* bounds);
NUTIL_API int nvgTextBreakLinesJni(NVGcontext* ctx, const char* string, int start, int end, float breakRowWidth, NVGtextRow* rows, int maxRows);
NUTIL_API int nvgTextGlyphPositionsJni(NVGcontext* ctx, float x, float y, const char* string, int start,int end, NVGglyphPosition* positions, int maxPositions);


*/
float nvgTextJni(NVGcontext *ctx, float x, float y, const char *string, int start, int end) {
    return nvgText(ctx, x, y, string + start, string + end);
}

void nvgTextBoxJni(NVGcontext *ctx, float x, float y, float breakRowWidth, const char *string, int start, int end) {
    nvgTextBox(ctx, x, y, breakRowWidth, string + start, string + end);
}

float nvgTextBoundsJni(NVGcontext *ctx, float x, float y, const char *string, int start, int end, float *bounds) {
    return nvgTextBounds(ctx, x, y, string + start, string + end, bounds);
}

void nvgTextBoxBoundsJni(NVGcontext *ctx, float x, float y, float breakRowWidth, const char *string, int start, int end,
                         float *bounds) {
    nvgTextBoxBounds(ctx, x, y, breakRowWidth, string + start, string + end, bounds);
}


int nvgTextBreakLinesJni(NVGcontext *ctx, const char *string, int start, int end, float breakRowWidth, NVGtextRow *rows,
                         int maxRows) {
    return nvgTextBreakLines(ctx, string + start, string + end, breakRowWidth, rows, maxRows);
}

int nvgTextGlyphPositionsJni(NVGcontext *ctx, float x, float y, const char *string, int start, int end,
                             NVGglyphPosition *positions, int maxPositions) {
    return nvgTextGlyphPositions(ctx, x, y, string + start, string + end, positions, maxPositions);
}

static struct NVGtextRow *nvgCreateNVGtextRow(int count) {
    struct NVGtextRow *val = calloc(sizeof(struct NVGtextRow), count);
    return val;
}

void nvgDeleteNVGtextRow(struct NVGtextRow *val) {
    free(val);
}


float nvgNVGtextRow_width(struct NVGtextRow *ptr, int index) {
    return ptr[index].width;
}


void *nvgNVGtextRow_start(struct NVGtextRow *ptr, int index) {
    return (void *) ptr[index].start;
}


void *nvgNVGtextRow_end(struct NVGtextRow *ptr, int index) {
    return (void *) ptr[index].end;
}


void *nvgNVGtextRow_next(struct NVGtextRow *ptr, int index) {
    return (void *) ptr[index].next;
}


struct NVGglyphPosition *nvgCreateNVGglyphPosition(int count) {
    struct NVGglyphPosition *val = calloc(sizeof(struct NVGglyphPosition), count);
    return val;
}

void nvgDeleteNVGglyphPosition(struct NVGglyphPosition *val) {
    free(val);
}

float nvgNVGglyphPosition_x(struct NVGglyphPosition *ptr, int index) {
    return ptr[index].x;
}

#endif //JNI_GUI_NANOVG_JNI_ASSIST_H
