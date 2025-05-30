/*
 GLFM
 https://github.com/brackeen/glfm
 Copyright (c) 2014-2021 David Brackeen
 
 This software is provided 'as-is', without any express or implied warranty.
 In no event will the authors be held liable for any damages arising from the
 use of this software. Permission is granted to anyone to use this software
 for any purpose, including commercial applications, and to alter it and
 redistribute it freely, subject to the following restrictions:
 
 1. The origin of this software must not be misrepresented; you must not
    claim that you wrote the original software. If you use this software in a
    product, an acknowledgment in the product documentation would be appreciated
    but is not required.
 2. Altered source versions must be plainly marked as such, and must not be
    misrepresented as being the original software.
 3. This notice may not be removed or altered from any source distribution.
 */

#include "glfm.h"
#include "IAPManager.h"

#if !defined(GLFM_INCLUDE_METAL)
#define GLFM_INCLUDE_METAL 1
#endif

#if defined(GLFM_PLATFORM_IOS) || defined(GLFM_PLATFORM_TVOS)

#import <UIKit/UIKit.h>
//================================= gust add 1 =======================
#import <AVFoundation/AVFoundation.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import <AssetsLibrary/AssetsLibrary.h>
#import <AVFoundation/AVFoundation.h>


#ifdef NSFoundationVersionNumber_iOS_9_x_Max
#import <UserNotifications/UserNotifications.h>
#endif

#import <notify.h>
#define NotificationLock CFSTR("com.apple.springboard.lockcomplete")
#define NotificationChange CFSTR("com.apple.springboard.lockstate")
#define NotificationPwdUI CFSTR("com.apple.springboard.hasBlankedScreen")


//================================= gust add 1 =======================

#if TARGET_OS_IOS
#import <CoreHaptics/CoreHaptics.h>
#import <CoreMotion/CoreMotion.h>
#endif
#if GLFM_INCLUDE_METAL
#import <MetalKit/MetalKit.h>
#endif

#include <dlfcn.h>
#include "glfm_internal.h"

#define MAX_SIMULTANEOUS_TOUCHES 10

#ifndef NDEBUG
#define CHECK_GL_ERROR() ((void)0)
#else
#define CHECK_GL_ERROR() do { GLenum error = glGetError(); if (error != GL_NO_ERROR) \
NSLog(@"OpenGL error 0x%04x at glfm_platform_ios.m:%i", error, __LINE__); } while(0)
#endif

#if __has_feature(objc_arc)
#define GLFM_RETAIN(value) value
#define GLFM_AUTORELEASE(value) value
#define GLFM_RELEASE(value) ((void)0)
#define GLFM_WEAK __weak
#else
#define GLFM_RETAIN(value) [value retain]
#define GLFM_AUTORELEASE(value) [value autorelease]
#define GLFM_RELEASE(value) [value release]
#define GLFM_WEAK __unsafe_unretained
#endif

@interface GLFMAppDelegate : NSObject <UIApplicationDelegate>

@property(nonatomic, strong) UIWindow *window;
@property(nonatomic, assign) BOOL active;

@end

#pragma mark - GLFMView

static void glfm__preferredDrawableSize(CGRect bounds, CGFloat contentScaleFactor, int *width, int *height);

@protocol GLFMView

@property(nonatomic, readonly) GLFMRenderingAPI renderingAPI;
@property(nonatomic, readonly) int drawableWidth;
@property(nonatomic, readonly) int drawableHeight;
@property(nonatomic, assign) BOOL animating;
@property(nonatomic, copy, nullable) void (^preRenderCallback)(void);

- (void)draw;
- (void)swapBuffers;
- (void)requestRefresh;

@end

#if GLFM_INCLUDE_METAL

#pragma mark - GLFMMetalView

@interface GLFMMetalView : MTKView <GLFMView, MTKViewDelegate>

@property(nonatomic, assign) GLFMDisplay *glfmDisplay;
@property(nonatomic, assign) int drawableWidth;
@property(nonatomic, assign) int drawableHeight;
@property(nonatomic, assign) BOOL surfaceCreatedNotified;
@property(nonatomic, assign) BOOL refreshRequested;

@end

@implementation GLFMMetalView

@synthesize preRenderCallback = _preRenderCallback;
@dynamic renderingAPI, animating;

- (instancetype)initWithFrame:(CGRect)frame contentScaleFactor:(CGFloat)contentScaleFactor
                       device:(id<MTLDevice>)device glfmDisplay:(GLFMDisplay *)glfmDisplay {
    if ((self = [super initWithFrame:frame device:device])) {
        self.contentScaleFactor = contentScaleFactor;
        self.delegate = self;
        self.glfmDisplay = glfmDisplay;
        self.drawableWidth = (int)self.drawableSize.width;
        self.drawableHeight = (int)self.drawableSize.height;
        [self requestRefresh];

        switch (glfmDisplay->colorFormat) {
            case GLFMColorFormatRGB565:
                self.colorPixelFormat = MTLPixelFormatB5G6R5Unorm;
                break;
            case GLFMColorFormatRGBA8888:
            default:
                self.colorPixelFormat = MTLPixelFormatBGRA8Unorm;
                break;
        }
        
        if (glfmDisplay->depthFormat == GLFMDepthFormatNone &&
            glfmDisplay->stencilFormat == GLFMStencilFormatNone) {
            self.depthStencilPixelFormat = MTLPixelFormatInvalid;
        } else if (glfmDisplay->depthFormat == GLFMDepthFormatNone) {
            self.depthStencilPixelFormat = MTLPixelFormatStencil8;
        } else if (glfmDisplay->stencilFormat == GLFMStencilFormatNone) {
            if (@available(iOS 13, tvOS 13, *)) {
                if (glfmDisplay->depthFormat == GLFMDepthFormat16) {
                    self.depthStencilPixelFormat = MTLPixelFormatDepth16Unorm;
                } else {
                    self.depthStencilPixelFormat = MTLPixelFormatDepth32Float;
                }
            } else {
                self.depthStencilPixelFormat = MTLPixelFormatDepth32Float;
            }
            
        } else {
            self.depthStencilPixelFormat = MTLPixelFormatDepth32Float_Stencil8;
        }
        
        self.sampleCount = (glfmDisplay->multisample == GLFMMultisampleNone) ? 1 : 4;
    }
    return self;
}

- (GLFMRenderingAPI)renderingAPI {
    return GLFMRenderingAPIMetal;
}

- (BOOL)animating {
    return !self.paused;
}

- (void)setAnimating:(BOOL)animating {
    if (self.animating != animating) {
        self.paused = !animating;
        [self requestRefresh];
    }
}

- (void)mtkView:(MTKView *)view drawableSizeWillChange:(CGSize)size {
    
}

- (void)drawInMTKView:(MTKView *)view {
    int newDrawableWidth = (int)self.drawableSize.width;
    int newDrawableHeight = (int)self.drawableSize.height;
    if (!self.surfaceCreatedNotified) {
        self.surfaceCreatedNotified = YES;
        [self requestRefresh];

        self.drawableWidth = newDrawableWidth;
        self.drawableHeight = newDrawableHeight;
        if (_glfmDisplay->surfaceCreatedFunc) {
            _glfmDisplay->surfaceCreatedFunc(_glfmDisplay, self.drawableWidth, self.drawableHeight);
        }
    } else if (newDrawableWidth != self.drawableWidth || newDrawableHeight != self.drawableHeight) {
        [self requestRefresh];
        self.drawableWidth = newDrawableWidth;
        self.drawableHeight = newDrawableHeight;
        if (_glfmDisplay->surfaceResizedFunc) {
            _glfmDisplay->surfaceResizedFunc(_glfmDisplay, self.drawableWidth, self.drawableHeight);
        }
    }
    
    if (_preRenderCallback) {
        _preRenderCallback();
    }
    
    if (self.refreshRequested) {
        self.refreshRequested = NO;
        if (_glfmDisplay->surfaceRefreshFunc) {
            _glfmDisplay->surfaceRefreshFunc(_glfmDisplay);
        }
    }
    
    if (_glfmDisplay->renderFunc) {
        _glfmDisplay->renderFunc(_glfmDisplay);
    }
}

- (void)swapBuffers {
    // Do nothing
}

- (void)requestRefresh {
    self.refreshRequested = YES;
}

- (void)layoutSubviews {
    // First render as soon as safeAreaInsets are set
    if (!self.surfaceCreatedNotified) {
        [self requestRefresh];
        [self draw];
    }
}

#if !__has_feature(objc_arc)
- (void)dealloc {
    [_preRenderCallback release];
    [super dealloc];
}
#endif

@end

#endif

#pragma mark - GLFMOpenGLView

@interface GLFMOpenGLView : UIView <GLFMView>

@property(nonatomic, assign) GLFMDisplay *glfmDisplay;
@property(nonatomic, assign) GLFMRenderingAPI renderingAPI;
@property(nonatomic, strong) CADisplayLink *displayLink;
@property(nonatomic, strong) EAGLContext *context;
@property(nonatomic, strong) NSString *colorFormat;
@property(nonatomic, assign) BOOL preserveBackbuffer;
@property(nonatomic, assign) NSUInteger depthBits;
@property(nonatomic, assign) NSUInteger stencilBits;
@property(nonatomic, assign) BOOL multisampling;
@property(nonatomic, assign) BOOL surfaceCreatedNotified;
@property(nonatomic, assign) BOOL surfaceSizeChanged;
@property(nonatomic, assign) BOOL refreshRequested;

@end

@implementation GLFMOpenGLView {
    GLint _drawableWidth;
    GLint _drawableHeight;
    GLuint _defaultFramebuffer;
    GLuint _colorRenderbuffer;
    GLuint _attachmentRenderbuffer;
    GLuint _msaaFramebuffer;
    GLuint _msaaRenderbuffer;
}

@synthesize preRenderCallback = _preRenderCallback;
@dynamic drawableWidth, drawableHeight, animating;

+ (Class)layerClass {
    return [CAEAGLLayer class];
}

- (instancetype)initWithFrame:(CGRect)frame contentScaleFactor:(CGFloat)contentScaleFactor
                  glfmDisplay:(GLFMDisplay *)glfmDisplay {
    if ((self = [super initWithFrame:frame])) {
        
        self.contentScaleFactor = contentScaleFactor;
        self.glfmDisplay = glfmDisplay;
        [self requestRefresh];
        
        if (glfmDisplay->preferredAPI >= GLFMRenderingAPIOpenGLES3) {
            self.context = GLFM_AUTORELEASE([[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES3]);
            self.renderingAPI = GLFMRenderingAPIOpenGLES3;
        }
        if (!self.context) {
            self.context = GLFM_AUTORELEASE([[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2]);
            self.renderingAPI = GLFMRenderingAPIOpenGLES2;
        }
        
        if (!self.context) {
            glfm__reportSurfaceError(glfmDisplay, "Failed to create ES context");
            GLFM_RELEASE(self);
            return nil;
        }
        
        switch (glfmDisplay->colorFormat) {
            case GLFMColorFormatRGB565:
                self.colorFormat = kEAGLColorFormatRGB565;
                break;
            case GLFMColorFormatRGBA8888:
            default:
                self.colorFormat = kEAGLColorFormatRGBA8;
                break;
        }
        
        switch (glfmDisplay->depthFormat) {
            case GLFMDepthFormatNone:
            default:
                self.depthBits = 0;
                break;
            case GLFMDepthFormat16:
                self.depthBits = 16;
                break;
            case GLFMDepthFormat24:
                self.depthBits = 24;
                break;
        }
        
        switch (glfmDisplay->stencilFormat) {
            case GLFMStencilFormatNone:
            default:
                self.stencilBits = 0;
                break;
            case GLFMStencilFormat8:
                self.stencilBits = 8;
                break;
        }
        
        self.multisampling = glfmDisplay->multisample != GLFMMultisampleNone;
        
        [self createDrawable];
    }
    return self;
}

- (void)dealloc {
    self.animating = NO;
    [self deleteDrawable];
    if ([EAGLContext currentContext] == self.context) {
        [EAGLContext setCurrentContext:nil];
    }
#if !__has_feature(objc_arc)
    self.context = nil;
    self.colorFormat = nil;
    [_preRenderCallback release];
    [super dealloc];
#endif
}

- (int)drawableWidth {
    return (int)_drawableWidth;
}

- (int)drawableHeight {
    return (int)_drawableHeight;
}

- (BOOL)animating {
    return (self.displayLink != nil);
}

- (void)setAnimating:(BOOL)animating {
    if (self.animating != animating) {
        [self requestRefresh];
        if (!animating) {
            [self.displayLink invalidate];
            self.displayLink = nil;
        } else {
            self.displayLink = [CADisplayLink displayLinkWithTarget:self
                                                           selector:@selector(render:)];
            [self.displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
        }
    }
}

- (void)createDrawable {
    if (_defaultFramebuffer != 0 || !self.context) {
        return;
    }

    if (!self.colorFormat) {
        self.colorFormat = kEAGLColorFormatRGBA8;
    }

    [EAGLContext setCurrentContext:self.context];

    CAEAGLLayer *eaglLayer = (CAEAGLLayer *)self.layer;
    eaglLayer.opaque = YES;
    eaglLayer.drawableProperties =
        @{ kEAGLDrawablePropertyRetainedBacking : @(self.preserveBackbuffer),
           kEAGLDrawablePropertyColorFormat : self.colorFormat };

    glGenFramebuffers(1, &_defaultFramebuffer);
    glGenRenderbuffers(1, &_colorRenderbuffer);
    glBindFramebuffer(GL_FRAMEBUFFER, _defaultFramebuffer);
    glBindRenderbuffer(GL_RENDERBUFFER, _colorRenderbuffer);

    // iPhone 6 Display Zoom hack - use a modified bounds so that the renderbufferStorage method
    // creates the correct size renderbuffer.
    CGRect oldBounds = eaglLayer.bounds;
    if (eaglLayer.contentsScale == 2.343750) {
        if (eaglLayer.bounds.size.width == 320.0 && eaglLayer.bounds.size.height == 568.0) {
            eaglLayer.bounds = CGRectMake(eaglLayer.bounds.origin.x, eaglLayer.bounds.origin.y,
                                          eaglLayer.bounds.size.width,
                                          1334 / eaglLayer.contentsScale);
        } else if (eaglLayer.bounds.size.width == 568.0 && eaglLayer.bounds.size.height == 320.0) {
            eaglLayer.bounds = CGRectMake(eaglLayer.bounds.origin.x, eaglLayer.bounds.origin.y,
                                          1334 / eaglLayer.contentsScale,
                                          eaglLayer.bounds.size.height);
        }
    }

    if (![self.context renderbufferStorage:GL_RENDERBUFFER fromDrawable:eaglLayer]) {
        NSLog(@"Error: Call to renderbufferStorage failed");
    }

    eaglLayer.bounds = oldBounds;

    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER,
                              _colorRenderbuffer);

    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &_drawableWidth);
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &_drawableHeight);

    if (_multisampling) {
        glGenFramebuffers(1, &_msaaFramebuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, _msaaFramebuffer);

        glGenRenderbuffers(1, &_msaaRenderbuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, _msaaRenderbuffer);

        GLenum internalformat = GL_RGBA8_OES;
        if ([kEAGLColorFormatRGB565 isEqualToString:_colorFormat]) {
            internalformat = GL_RGB565;
        }

        glRenderbufferStorageMultisampleAPPLE(GL_RENDERBUFFER, 4, internalformat,
                                              _drawableWidth, _drawableHeight);

        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER,
                                  _msaaRenderbuffer);

        GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            NSLog(@"Error: Couldn't create multisample framebuffer: 0x%04x", status);
        }
    }

    if (_depthBits > 0 || _stencilBits > 0) {
        glGenRenderbuffers(1, &_attachmentRenderbuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, _attachmentRenderbuffer);

        GLenum internalformat;
        if (_depthBits > 0 && _stencilBits > 0) {
            internalformat = GL_DEPTH24_STENCIL8_OES;
        } else if (_depthBits >= 24) {
            internalformat = GL_DEPTH_COMPONENT24_OES;
        } else if (_depthBits > 0) {
            internalformat = GL_DEPTH_COMPONENT16;
        } else {
            internalformat = GL_STENCIL_INDEX8;
        }

        if (_multisampling) {
            glRenderbufferStorageMultisampleAPPLE(GL_RENDERBUFFER, 4, internalformat,
                                                  _drawableWidth, _drawableHeight);
        } else {
            glRenderbufferStorage(GL_RENDERBUFFER, internalformat, _drawableWidth, _drawableHeight);
        }

        if (_depthBits > 0) {
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER,
                                      _attachmentRenderbuffer);
        }
        if (_stencilBits > 0) {
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER,
                                      _attachmentRenderbuffer);
        }
    }

    GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status != GL_FRAMEBUFFER_COMPLETE) {
        NSLog(@"Error: Framebuffer incomplete: 0x%04x", status);
    }

    CHECK_GL_ERROR();
}

- (void)deleteDrawable {
    if (_defaultFramebuffer) {
        glDeleteFramebuffers(1, &_defaultFramebuffer);
        _defaultFramebuffer = 0;
    }
    if (_colorRenderbuffer) {
        glDeleteRenderbuffers(1, &_colorRenderbuffer);
        _colorRenderbuffer = 0;
    }
    if (_attachmentRenderbuffer) {
        glDeleteRenderbuffers(1, &_attachmentRenderbuffer);
        _attachmentRenderbuffer = 0;
    }
    if (_msaaRenderbuffer) {
        glDeleteRenderbuffers(1, &_msaaRenderbuffer);
        _msaaRenderbuffer = 0;
    }
    if (_msaaFramebuffer) {
        glDeleteFramebuffers(1, &_msaaFramebuffer);
        _msaaFramebuffer = 0;
    }
}

- (void)prepareRender {
    [EAGLContext setCurrentContext:self.context];
    if (_multisampling) {
        glBindFramebuffer(GL_FRAMEBUFFER, _msaaFramebuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, _msaaRenderbuffer);
    } else {
        glBindFramebuffer(GL_FRAMEBUFFER, _defaultFramebuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, _colorRenderbuffer);
    }
    CHECK_GL_ERROR();
}

- (void)finishRender {
    if (_multisampling) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER_APPLE, _msaaFramebuffer);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER_APPLE, _defaultFramebuffer);
        glResolveMultisampleFramebufferAPPLE();
    }

    static bool checked_GL_EXT_discard_framebuffer = false;
    static bool has_GL_EXT_discard_framebuffer = false;
    if (!checked_GL_EXT_discard_framebuffer) {
        checked_GL_EXT_discard_framebuffer = true;
        if (glfmExtensionSupported("GL_EXT_discard_framebuffer")) {
            has_GL_EXT_discard_framebuffer = true;
        }
    }
    if (has_GL_EXT_discard_framebuffer) {
        GLenum target = GL_FRAMEBUFFER;
        GLenum attachments[3];
        GLsizei numAttachments = 0;
        if (_multisampling) {
            target = GL_READ_FRAMEBUFFER_APPLE;
            attachments[numAttachments++] = GL_COLOR_ATTACHMENT0;
        }
        if (_depthBits > 0) {
            attachments[numAttachments++] = GL_DEPTH_ATTACHMENT;
        }
        if (_stencilBits > 0) {
            attachments[numAttachments++] = GL_STENCIL_ATTACHMENT;
        }
        if (numAttachments > 0) {
            if (_multisampling) {
                glBindFramebuffer(GL_FRAMEBUFFER, _msaaFramebuffer);
            } else {
                glBindFramebuffer(GL_FRAMEBUFFER, _defaultFramebuffer);
            }
            glDiscardFramebufferEXT(target, numAttachments, attachments);
        }
    }

    glBindRenderbuffer(GL_RENDERBUFFER, _colorRenderbuffer);
    [self.context presentRenderbuffer:GL_RENDERBUFFER];

    CHECK_GL_ERROR();
}

- (void)render:(CADisplayLink *)displayLink {
    if (!self.surfaceCreatedNotified) {
        self.surfaceCreatedNotified = YES;
        [self requestRefresh];

        if (_glfmDisplay->surfaceCreatedFunc) {
            _glfmDisplay->surfaceCreatedFunc(_glfmDisplay, self.drawableWidth, self.drawableHeight);
        }
    }
    
    if (self.surfaceSizeChanged) {
        self.surfaceSizeChanged  = NO;
        [self requestRefresh];
        if (_glfmDisplay->surfaceResizedFunc) {
            _glfmDisplay->surfaceResizedFunc(_glfmDisplay, self.drawableWidth, self.drawableHeight);
        }
    }
    
    if (_preRenderCallback) {
        _preRenderCallback();
    }
    
    [self prepareRender];
    if (self.refreshRequested) {
        self.refreshRequested = NO;
        if (_glfmDisplay->surfaceRefreshFunc) {
            _glfmDisplay->surfaceRefreshFunc(_glfmDisplay);
        }
    }
    if (_glfmDisplay->renderFunc) {
        _glfmDisplay->renderFunc(_glfmDisplay);
    }
}

- (void)swapBuffers {
    [self finishRender];
}

- (void)draw {
    if (self.displayLink) {
        [self render:self.displayLink];
    }
}

- (void)requestRefresh {
    self.refreshRequested = YES;
}

- (void)layoutSubviews {
    int newDrawableWidth;
    int newDrawableHeight;
    glfm__preferredDrawableSize(self.bounds, self.contentScaleFactor, &newDrawableWidth, &newDrawableHeight);

    if (self.drawableWidth != newDrawableWidth || self.drawableHeight != newDrawableHeight) {
        [self deleteDrawable];
        [self createDrawable];
        self.surfaceSizeChanged = self.surfaceCreatedNotified;
    }
    
    // First render as soon as safeAreaInsets are set
    if (!self.surfaceCreatedNotified) {
        [self requestRefresh];
        [self draw];
    }
}

@end

#pragma mark APLIndexedPosition
@interface APLIndexedPosition : UITextPosition

@property (nonatomic) NSUInteger index;
@property (nonatomic) id <UITextInputDelegate> inputDelegate;

+ (instancetype)positionWithIndex:(NSUInteger)index;

@end

@implementation APLIndexedPosition

#pragma mark IndexedPosition implementation

// Class method to create an instance with a given integer index.
+ (instancetype)positionWithIndex:(NSUInteger)index
{
    APLIndexedPosition *indexedPosition = [[self alloc] init];
    indexedPosition.index = index;
    return indexedPosition;
}

@end

#pragma mark APLIndexedRange
@interface APLIndexedRange : UITextRange

@property (nonatomic) NSRange range;
+ (instancetype)indexedRangeWithRange:(NSRange)range;

@end


@implementation APLIndexedRange

// Class method to create an instance with a given range
+ (instancetype)indexedRangeWithRange:(NSRange)range
{
    if (range.location == NSNotFound) {
        return nil;
    }

    APLIndexedRange *indexedRange = [[self alloc] init];
    indexedRange.range = range;
    return indexedRange;
}


// UITextRange read-only property - returns start index of range.
- (UITextPosition *)start
{
    return [APLIndexedPosition positionWithIndex:self.range.location];
}


// UITextRange read-only property - returns end index of range.
- (UITextPosition *)end
{
    return [APLIndexedPosition positionWithIndex:(self.range.location + self.range.length)];
}


// UITextRange read-only property - returns YES if range is zero length.
-(BOOL)isEmpty
{
    return (self.range.length == 0);
}

@end


#pragma mark - GLFMViewController

@interface GLFMViewController : UIViewController<UIKeyInput, UITextInputTraits, UITextInput/*gust add*/>

@property(nonatomic, assign) GLFMDisplay *glfmDisplay;
@property(nonatomic, assign) BOOL multipleTouchEnabled;
@property(nonatomic, assign) BOOL keyboardRequested;
@property(nonatomic, assign) BOOL keyboardVisible;

#if GLFM_INCLUDE_METAL
@property(nonatomic, strong) id<MTLDevice> metalDevice;
#endif
#if TARGET_OS_IOS
@property(nonatomic, strong) CMMotionManager *motionManager;
@property(nonatomic, assign) UIInterfaceOrientation orientation;
#endif

//================================= gust add 2 =======================
@property(nonatomic, assign) int pickerUid;
@property(nonatomic, assign) int pickerType;
//gust for mutitage input ,ex chinese
@property (nonatomic, retain) NSMutableString *text;
@property (nonatomic) NSRange inMarkedTextRange; // Marked text range (for input method marked text).
@property (nonatomic) NSRange inSelectedTextRange; // Selected text range.


- (void)takePhotoAction:(int) puid : (int) type;
- (void)browseAlbum:(int) puid : (int) type;
+ (UIImage *)cropImage:(UIImage *)image inRect:(CGRect)rect;
+ (UIImage *)resizeCropImage:(UIImage *)image toRect:(CGSize)size;
+ (UIImage *)resizeImage:(UIImage *)image toSize:(CGSize)reSize;
//================================= gust add 2 =======================

@end

@implementation GLFMViewController {
    const void *activeTouches[MAX_SIMULTANEOUS_TOUCHES];
}

- (id)init {
    if ((self = [super init])) {
        [self clearTouches];
        _glfmDisplay = calloc(1, sizeof(GLFMDisplay));
        _glfmDisplay->platformData = (__bridge void *)self;
        _glfmDisplay->supportedOrientations = GLFMInterfaceOrientationAll;
        //gust
        self.text = [[NSMutableString alloc] init];
    }
    return self;
}

#if GLFM_INCLUDE_METAL
- (id<MTLDevice>)metalDevice {
    if (!_metalDevice) {
        self.metalDevice = GLFM_AUTORELEASE(MTLCreateSystemDefaultDevice());
    }
    return _metalDevice;
}
#endif

#if TARGET_OS_IOS
- (BOOL)prefersStatusBarHidden {
    return _glfmDisplay->uiChrome != GLFMUserInterfaceChromeNavigationAndStatusBar;
}

- (UIRectEdge)preferredScreenEdgesDeferringSystemGestures {
    UIRectEdge edges =  UIRectEdgeLeft | UIRectEdgeRight;
    return _glfmDisplay->uiChrome == GLFMUserInterfaceChromeFullscreen ? UIRectEdgeBottom | edges : edges;
}
#endif

- (UIView<GLFMView> *)glfmView {
    return (UIView<GLFMView> *)self.view;
}

- (void)loadView {
    glfmMain(_glfmDisplay);

    GLFMAppDelegate *delegate = UIApplication.sharedApplication.delegate;
    CGRect frame = delegate.window.bounds;
    CGFloat scale = [UIScreen mainScreen].nativeScale;
    UIView<GLFMView> *glfmView = nil;
    
#if GLFM_INCLUDE_METAL
    if (_glfmDisplay->preferredAPI == GLFMRenderingAPIMetal && self.metalDevice) {
        glfmView = GLFM_AUTORELEASE([[GLFMMetalView alloc] initWithFrame:frame
                                                       contentScaleFactor:scale
                                                                   device:self.metalDevice
                                                              glfmDisplay:_glfmDisplay]);
    }
#endif
    if (!glfmView) {
        glfmView = GLFM_AUTORELEASE([[GLFMOpenGLView alloc] initWithFrame:frame
                                                        contentScaleFactor:scale
                                                               glfmDisplay:_glfmDisplay]);
    }
    GLFM_WEAK __typeof(self) weakSelf = self;
    glfmView.preRenderCallback = ^{
        [weakSelf preRenderCallback];
    };
    self.view = glfmView;
    self.view.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;


}
//================================= gust add 3 =======================
//强制转屏（这个方法最好放在BaseVController中）

- (void)setInterfaceOrientation:(UIInterfaceOrientation)orientation {
    if (@available(iOS 16.0, *)) {
        UIWindowScene *windowScene = self.view.window.windowScene;
        if (windowScene) {
            UIWindowSceneGeometryPreferencesIOS *preferences = [[UIWindowSceneGeometryPreferencesIOS alloc] 
                initWithInterfaceOrientations:1 << orientation];
            [windowScene requestGeometryUpdateWithPreferences:preferences errorHandler:^(NSError * _Nonnull error) {
                //NSLog(@"Failed to update interface orientation: %@", error);
            }];
        }
    } else {
        if ([[UIDevice currentDevice] respondsToSelector:@selector(setOrientation:)]) {
            SEL selector = NSSelectorFromString(@"setOrientation:");
            NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[UIDevice instanceMethodSignatureForSelector:selector]];
            [invocation setSelector:selector];
            [invocation setTarget:[UIDevice currentDevice]];
            [invocation setArgument:&orientation atIndex:2];
            [invocation invoke];
        }
    }
}
//================================= gust add 3 =======================

 

- (void)viewDidLoad {
    [super viewDidLoad];

    GLFMAppDelegate *delegate = UIApplication.sharedApplication.delegate;
    self.glfmView.animating = delegate.active;
    
#if TARGET_OS_IOS
    self.view.multipleTouchEnabled = self.multipleTouchEnabled;
    self.orientation = [[UIApplication sharedApplication] statusBarOrientation];

    [self setNeedsStatusBarAppearanceUpdate];

    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(keyboardFrameChanged:)
                                               name:UIKeyboardWillChangeFrameNotification
                                             object:self.view.window];
    
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(deviceOrientationChanged:)
                                               name:UIDeviceOrientationDidChangeNotification
                                             object:self.view.window];
#endif
}

#if TARGET_OS_IOS
//支持旋转
-(BOOL)shouldAutorotate{
   return YES;
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
    GLFMInterfaceOrientation orientations = self.glfmDisplay->supportedOrientations;
    BOOL portraitRequested = (orientations & (GLFMInterfaceOrientationPortrait |
                                              GLFMInterfaceOrientationPortraitUpsideDown)) != 0;
    BOOL landscapeRequested = (orientations & GLFMInterfaceOrientationLandscape) != 0;
    BOOL isTablet = [[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPad;
    if (portraitRequested && landscapeRequested) {
        return isTablet ? UIInterfaceOrientationMaskAll : UIInterfaceOrientationMaskAllButUpsideDown;
    }
    if (portraitRequested) {
        if (isTablet) {
            return (UIInterfaceOrientationMask)(UIInterfaceOrientationMaskPortrait |
                                                UIInterfaceOrientationMaskPortraitUpsideDown);
        }
        return UIInterfaceOrientationMaskPortrait;
    }
    return UIInterfaceOrientationMaskLandscape;
}

- (void)deviceOrientationChanged:(NSNotification *)notification {
    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
    if (self.orientation != orientation) {
        self.orientation = orientation;
        if (self.isViewLoaded) {
            [self.glfmView requestRefresh];
        }
        if (_glfmDisplay->orientationChangedFunc) {
            _glfmDisplay->orientationChangedFunc(_glfmDisplay,
                                                 glfmGetInterfaceOrientation(_glfmDisplay));
        }
    }
}

#endif

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    if (_glfmDisplay->lowMemoryFunc) {
        _glfmDisplay->lowMemoryFunc(_glfmDisplay);
    }
}

- (void)preRenderCallback {
#if TARGET_OS_IOS
    [self handleMotionEvents];
#endif
}

#if TARGET_OS_IOS

- (CMMotionManager *)motionManager {
    if (!_motionManager) {
        self.motionManager = GLFM_AUTORELEASE([CMMotionManager new]);
        self.motionManager.deviceMotionUpdateInterval = 0.01;
    }
    return _motionManager;
}

- (BOOL)isMotionManagerLoaded {
    return _motionManager != nil;
}

- (void)handleMotionEvents {
    if (!self.isMotionManagerLoaded || !self.motionManager.isDeviceMotionActive) {
        return;
    }
    CMDeviceMotion *deviceMotion = self.motionManager.deviceMotion;
    if (!deviceMotion) {
        // No readings yet
        return;
    }
    GLFMSensorFunc accelerometerFunc = self.glfmDisplay->sensorFuncs[GLFMSensorAccelerometer];
    if (accelerometerFunc) {
        GLFMSensorEvent event = { 0 };
        event.sensor = GLFMSensorAccelerometer;
        event.timestamp = deviceMotion.timestamp;
        event.vector.x = deviceMotion.userAcceleration.x + deviceMotion.gravity.x;
        event.vector.y = deviceMotion.userAcceleration.y + deviceMotion.gravity.y;
        event.vector.z = deviceMotion.userAcceleration.z + deviceMotion.gravity.z;
        accelerometerFunc(self.glfmDisplay, event);
    }
    
    GLFMSensorFunc magnetometerFunc = self.glfmDisplay->sensorFuncs[GLFMSensorMagnetometer];
    if (magnetometerFunc) {
        GLFMSensorEvent event = { 0 };
        event.sensor = GLFMSensorMagnetometer;
        event.timestamp = deviceMotion.timestamp;
        event.vector.x = deviceMotion.magneticField.field.x;
        event.vector.y = deviceMotion.magneticField.field.y;
        event.vector.z = deviceMotion.magneticField.field.z;
        magnetometerFunc(self.glfmDisplay, event);
    }
    
    GLFMSensorFunc gyroscopeFunc = self.glfmDisplay->sensorFuncs[GLFMSensorGyroscope];
    if (gyroscopeFunc) {
        GLFMSensorEvent event = { 0 };
        event.sensor = GLFMSensorGyroscope;
        event.timestamp = deviceMotion.timestamp;
        event.vector.x = deviceMotion.rotationRate.x;
        event.vector.y = deviceMotion.rotationRate.y;
        event.vector.z = deviceMotion.rotationRate.z;
        gyroscopeFunc(self.glfmDisplay, event);
    }
    
    GLFMSensorFunc rotationFunc = self.glfmDisplay->sensorFuncs[GLFMSensorRotationMatrix];
    if (rotationFunc) {
        GLFMSensorEvent event = { 0 };
        event.sensor = GLFMSensorRotationMatrix;
        event.timestamp = deviceMotion.timestamp;
        CMRotationMatrix matrix = deviceMotion.attitude.rotationMatrix;
        event.matrix.m00 = matrix.m11; event.matrix.m01 = matrix.m12; event.matrix.m02 = matrix.m13;
        event.matrix.m10 = matrix.m21; event.matrix.m11 = matrix.m22; event.matrix.m12 = matrix.m23;
        event.matrix.m20 = matrix.m31; event.matrix.m21 = matrix.m32; event.matrix.m22 = matrix.m33;
        rotationFunc(self.glfmDisplay, event);
    }
}

- (void)updateMotionManagerActiveState {
    BOOL enable = NO;
    GLFMAppDelegate *delegate = UIApplication.sharedApplication.delegate;
    if (delegate.active) {
        for (int i = 0; i < GLFM_NUM_SENSORS; i++) {
            if (self.glfmDisplay->sensorFuncs[i] != NULL) {
                enable = YES;
                break;
            }
        }
    }
    
    if (enable && !self.motionManager.deviceMotionActive) {
        CMAttitudeReferenceFrame referenceFrame;
        CMAttitudeReferenceFrame availableReferenceFrames = [CMMotionManager availableAttitudeReferenceFrames];
        if (availableReferenceFrames & CMAttitudeReferenceFrameXMagneticNorthZVertical) {
            referenceFrame = CMAttitudeReferenceFrameXMagneticNorthZVertical;
        } else if (availableReferenceFrames & CMAttitudeReferenceFrameXArbitraryCorrectedZVertical) {
            referenceFrame = CMAttitudeReferenceFrameXArbitraryCorrectedZVertical;
        } else {
            referenceFrame = CMAttitudeReferenceFrameXArbitraryZVertical;
        }
        [self.motionManager startDeviceMotionUpdatesUsingReferenceFrame:referenceFrame];
    } else if (!enable && self.isMotionManagerLoaded && self.motionManager.deviceMotionActive) {
        [self.motionManager stopDeviceMotionUpdates];
    }
}

#endif

- (void)dealloc {
    if (_glfmDisplay->surfaceDestroyedFunc) {
        _glfmDisplay->surfaceDestroyedFunc(_glfmDisplay);
    }
    free(_glfmDisplay);
    self.glfmView.preRenderCallback = nil;
#if !__has_feature(objc_arc)
    self.motionManager = nil;
#if GLFM_INCLUDE_METAL
    self.metalDevice = nil;
#endif
    [super dealloc];
#endif
}

#pragma mark - UIResponder

- (void)clearTouches {
    for (int i = 0; i < MAX_SIMULTANEOUS_TOUCHES; i++) {
        activeTouches[i] = NULL;
    }
}

- (void)addTouchEvent:(UITouch *)touch withType:(GLFMTouchPhase)phase {
    int firstNullIndex = -1;
    int index = -1;
    for (int i = 0; i < MAX_SIMULTANEOUS_TOUCHES; i++) {
        if (activeTouches[i] == (__bridge const void *)touch) {
            index = i;
            break;
        } else if (firstNullIndex == -1 && activeTouches[i] == NULL) {
            firstNullIndex = i;
        }
    }
    if (index == -1) {
        if (firstNullIndex == -1) {
            // Shouldn't happen
            return;
        }
        index = firstNullIndex;
        activeTouches[index] = (__bridge const void *)touch;
    }

    if (_glfmDisplay->touchFunc) {
        CGPoint currLocation = [touch locationInView:self.view];
        currLocation.x *= self.view.contentScaleFactor;
        currLocation.y *= self.view.contentScaleFactor;

        _glfmDisplay->touchFunc(_glfmDisplay, index, phase,
                                (double)currLocation.x, (double)currLocation.y);
    }

    if (phase == GLFMTouchPhaseEnded || phase == GLFMTouchPhaseCancelled) {
        activeTouches[index] = NULL;
    }
}

- (BOOL) isVideoPlayerShown {
    for (UIView *subView in self.view.subviews) {
        AVPlayer *avplayer = [subView.layer valueForKey:@"AVPLAYER"];
        if(avplayer != nil){
            return true;
        }
    }
    return false;
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    if ([self isVideoPlayerShown])return;
    for (UITouch *touch in touches) {
        [self addTouchEvent:touch withType:GLFMTouchPhaseBegan];
    }
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event {
    if ([self isVideoPlayerShown])return;
    for (UITouch *touch in touches) {
        [self addTouchEvent:touch withType:GLFMTouchPhaseMoved];
    }
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {
    if ([self isVideoPlayerShown])return;
    for (UITouch *touch in touches) {
        [self addTouchEvent:touch withType:GLFMTouchPhaseEnded];
    }
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event {
    if ([self isVideoPlayerShown])return;
    for (UITouch *touch in touches) {
        [self addTouchEvent:touch withType:GLFMTouchPhaseCancelled];
    }
}

#if TARGET_OS_TV

- (BOOL)handlePress:(UIPress *)press withAction:(GLFMKeyAction)action {
    if (_glfmDisplay->keyFunc) {
        GLFMKey key = (GLFMKey)0;
        switch (press.type) {
            case UIPressTypeUpArrow:
                key = GLFMKeyUp;
                break;
            case UIPressTypeDownArrow:
                key = GLFMKeyDown;
                break;
            case UIPressTypeLeftArrow:
                key = GLFMKeyLeft;
                break;
            case UIPressTypeRightArrow:
                key = GLFMKeyRight;
                break;
            case UIPressTypeSelect:
                key = GLFMKeyNavSelect;
                break;
            case UIPressTypeMenu:
                key = GLFMKeyNavMenu;
                break;
            case UIPressTypePlayPause:
                key = GLFMKeyPlayPause;
                break;
        }
        if (key != 0) {
            return _glfmDisplay->keyFunc(_glfmDisplay, key, action, 0);
        } else {
            return NO;
        }
    } else {
        return NO;
    }
}

- (void)pressesBegan:(NSSet<UIPress *> *)presses withEvent:(UIPressesEvent *)event {
    BOOL handled = YES;
    for (UIPress *press in presses) {
        handled &= [self handlePress:press withAction:GLFMKeyActionPressed];
    }
    if (!handled) {
        [super pressesBegan:presses withEvent:event];
    }
}

- (void)pressesChanged:(NSSet<UIPress *> *)presses withEvent:(UIPressesEvent *)event {
    [super pressesChanged:presses withEvent:event];
}

- (void)pressesEnded:(NSSet<UIPress *> *)presses withEvent:(UIPressesEvent *)event {
    BOOL handled = YES;
    for (UIPress *press in presses) {
        handled &= [self handlePress:press withAction:GLFMKeyActionReleased];
    }
    if (!handled) {
        [super pressesEnded:presses withEvent:event];
    }
}

- (void)pressesCancelled:(NSSet<UIPress *> *)presses withEvent:(UIPressesEvent *)event {
    [super pressesCancelled:presses withEvent:event];
}

#endif

#pragma mark - UIKeyInput

#if TARGET_OS_IOS

- (void)keyboardFrameChanged:(NSNotification *)notification {
    NSObject *value = notification.userInfo[UIKeyboardFrameEndUserInfoKey];
    if ([value isKindOfClass:[NSValue class]]) {
        NSValue *nsValue = (NSValue *)value;
        CGRect keyboardFrame = [nsValue CGRectValue];

        self.keyboardVisible = CGRectIntersectsRect(self.view.window.frame, keyboardFrame);
        if (!self.keyboardVisible) {
            // User hid keyboard (iPad)
            self.keyboardRequested = NO;
        }
        
        if (self.isViewLoaded) {
            [self.glfmView requestRefresh];
        }

        if (_glfmDisplay->keyboardVisibilityChangedFunc) {
            // Convert to view coordinates
            keyboardFrame = [self.view convertRect:keyboardFrame fromView:nil];

            // Convert to pixels
            keyboardFrame.origin.x *= self.view.contentScaleFactor;
            keyboardFrame.origin.y *= self.view.contentScaleFactor;
            keyboardFrame.size.width *= self.view.contentScaleFactor;
            keyboardFrame.size.height *= self.view.contentScaleFactor;

            _glfmDisplay->keyboardVisibilityChangedFunc(_glfmDisplay, self.keyboardVisible,
                                                        keyboardFrame.origin.x,
                                                        keyboardFrame.origin.y,
                                                        keyboardFrame.size.width,
                                                        keyboardFrame.size.height);

        }
    }
}


#endif

// UITextInputTraits - disable suggestion bar
- (UITextAutocorrectionType)autocorrectionType {
    return UITextAutocorrectionTypeNo;
}

- (BOOL)canBecomeFirstResponder {
    return self.keyboardRequested;
}

- (NSArray<UIKeyCommand *> *)keyCommands {
    static NSArray<UIKeyCommand *> *keyCommands = NULL;
    if (!keyCommands) {
        NSArray<NSString *> *keyInputs = @[
            UIKeyInputUpArrow, UIKeyInputDownArrow, UIKeyInputLeftArrow, UIKeyInputRightArrow,
            UIKeyInputEscape, UIKeyInputPageUp, UIKeyInputPageDown,
        ];
        if (@available(iOS 13.4, tvOS  13.4, *)) {
            keyInputs = [keyInputs arrayByAddingObjectsFromArray: @[
                UIKeyInputHome, UIKeyInputEnd,
            ]];
                           
        }
        NSMutableArray *mutableKeyCommands = GLFM_AUTORELEASE([NSMutableArray new]);
        [keyInputs enumerateObjectsUsingBlock:^(NSString *keyInput, NSUInteger idx, BOOL *stop) {
            (void)idx;
            (void)stop;
            [mutableKeyCommands addObject:[UIKeyCommand keyCommandWithInput:keyInput
                                                              modifierFlags:(UIKeyModifierFlags)0
                                                                     action:@selector(keyPressed:)]];
        }];
        keyCommands = GLFM_RETAIN([mutableKeyCommands copy]);
    }

    return keyCommands;
}

- (void)keyPressed:(UIKeyCommand *)keyCommand {
    if (_glfmDisplay->keyFunc) {
        NSString *key = [keyCommand input];
        GLFMKey keyCode = (GLFMKey)0;
        if (key == UIKeyInputUpArrow) {
            keyCode = GLFMKeyUp;
        } else if (key == UIKeyInputDownArrow) {
            keyCode = GLFMKeyDown;
        } else if (key == UIKeyInputLeftArrow) {
            keyCode = GLFMKeyLeft;
        } else if (key == UIKeyInputRightArrow) {
            keyCode = GLFMKeyRight;
        } else if (key == UIKeyInputEscape) {
            keyCode = GLFMKeyEscape;
        } else if (key == UIKeyInputPageUp) {
            keyCode = GLFMKeyPageUp;
        } else if (key == UIKeyInputPageDown) {
            keyCode = GLFMKeyPageDown;
        }
        
        if (@available(iOS 13.4, tvOS  13.4, *)) {
            if (key == UIKeyInputHome) {
                keyCode = GLFMKeyHome;
            } else if (key == UIKeyInputEnd) {
                keyCode = GLFMKeyEnd;
            }
        }

        if (keyCode != 0) {
            _glfmDisplay->keyFunc(_glfmDisplay, keyCode, GLFMKeyActionPressed, 0);
            _glfmDisplay->keyFunc(_glfmDisplay, keyCode, GLFMKeyActionReleased, 0);
        }
    }
}

#pragma mark - UITextInput
@synthesize beginningOfDocument;

@synthesize endOfDocument;

@synthesize inputDelegate;

@synthesize markedTextRange;

@synthesize markedTextStyle;

@synthesize selectedTextRange;

@synthesize tokenizer;


#pragma mark UITextInput - Replacing and Returning Text

/**
 UITextInput protocol required method.
 Called by text system to get the string for a given range in the text storage.
 */
- (NSString *)textInRange:(UITextRange *)range
{
    APLIndexedRange *r = (APLIndexedRange *)range;
    NSString *ts = ([self.text substringWithRange:r.range]);
    return ts;
}


/**
 UITextInput protocol required method.
 Called by text system to replace the given text storage range with new text.
 */
- (void)replaceRange:(UITextRange *)range withText:(NSString *)text
{
    APLIndexedRange *indexedRange = (APLIndexedRange *)range;
    // Determine if replaced range intersects current selection range
    // and update selection range if so.
    NSRange selectedNSRange = self.inSelectedTextRange;
    if ((indexedRange.range.location + indexedRange.range.length) <= selectedNSRange.location) {
        // This is the easy case.
        selectedNSRange.location -= (indexedRange.range.length - text.length);
    } else {
        // Need to also deal with overlapping ranges.  Not addressed
        // in this simplified sample.
    }

    // Now replace characters in text storage
    [self.text replaceCharactersInRange:indexedRange.range withString:text];

    // Update underlying APLSimpleCoreTextView
    self.inSelectedTextRange = selectedNSRange;
}


#pragma mark UITextInput - Working with Marked and Selected Text

/**
 UITextInput selectedTextRange property accessor overrides (access/update underlaying APLSimpleCoreTextView)
 */
- (UITextRange *)selectedTextRange
{
    return [APLIndexedRange indexedRangeWithRange:self.inSelectedTextRange];
}


- (void)setSelectedTextRange:(UITextRange *)range
{
    APLIndexedRange *indexedRange = (APLIndexedRange *)range;
    self.inSelectedTextRange = indexedRange.range;
}


/**
 UITextInput markedTextRange property accessor overrides (access/update underlaying APLSimpleCoreTextView).
 */
- (UITextRange *)markedTextRange
{
    /*
     Return nil if there is no marked text.
     */
    NSRange markedTextRange = self.inMarkedTextRange;
    if (markedTextRange.length == 0) {
        return nil;
    }
    
    return [APLIndexedRange indexedRangeWithRange:markedTextRange];
}


/**
 UITextInput protocol required method.
 Insert the provided text and marks it to indicate that it is part of an active input session.
 */
- (void)setMarkedText:(NSString *)markedText selectedRange:(NSRange)selectedRange
{
    NSRange selectedNSRange = self.inSelectedTextRange;
    NSRange markedTextRange = self.inMarkedTextRange;

    if (markedTextRange.location != NSNotFound) {
        if (!markedText)
            markedText = @"";
        // Replace characters in text storage and update markedText range length.
        [self.text replaceCharactersInRange:markedTextRange withString:markedText];
        markedTextRange.length = markedText.length;
    }
    else if (selectedNSRange.length > 0) {
        // There currently isn't a marked text range, but there is a selected range,
        // so replace text storage at selected range and update markedTextRange.
        [self.text replaceCharactersInRange:selectedNSRange withString:markedText];
        markedTextRange.location = selectedNSRange.location;
        markedTextRange.length = markedText.length;
    }
    else {
        // There currently isn't marked or selected text ranges, so just insert
        // given text into storage and update markedTextRange.
        [self.text insertString:markedText atIndex:selectedNSRange.location];
        markedTextRange.location = selectedNSRange.location;
        markedTextRange.length = markedText.length;
    }

    // Updated selected text range and underlying APLSimpleCoreTextView.

    selectedNSRange = NSMakeRange(selectedRange.location + markedTextRange.location, selectedRange.length);

    self.inMarkedTextRange = markedTextRange;
    self.inSelectedTextRange = selectedNSRange;
}


/**
 UITextInput protocol required method.
 Unmark the currently marked text.
 */
- (void)unmarkText
{
    NSRange markedTextRange = self.inMarkedTextRange;

    if (markedTextRange.location == NSNotFound) {
        return;
    }
    // Unmark the underlying APLSimpleCoreTextView.markedTextRange.
    markedTextRange.location = NSNotFound;
    self.inMarkedTextRange = markedTextRange;

    //这个插入动作主要是针对ios自带的拼音键盘输入的时候，多阶段输入最终选取文字之后，
    //ios自带拼音输入法并没有调用insertText()把文本传入jvm,因此在进行unmark时，把最终文本传给jvm
    //当把self.text传给jvm后，便清空之前的内容
    if (_glfmDisplay->charFunc) {
        _glfmDisplay->charFunc(_glfmDisplay, self.text.UTF8String, 0);
        //这里把self.contentText, self.text清空 , 把self.inSelectedTextRange, self.inMarkedTextRange设为0长度
        self.inSelectedTextRange = NSMakeRange(0, 0);
        self.inMarkedTextRange = NSMakeRange(0, 0);
        [self.text deleteCharactersInRange:NSMakeRange(0, self.text.length) ];
    }
}


#pragma mark UITextInput - Computing Text Ranges and Text Positions

// UITextInput beginningOfDocument property accessor override.
- (UITextPosition *)beginningOfDocument
{
    // For this sample, the document always starts at index 0 and is the full length of the text storage.
    return [APLIndexedPosition positionWithIndex:0];
}


// UITextInput endOfDocument property accessor override.
- (UITextPosition *)endOfDocument
{
    // For this sample, the document always starts at index 0 and is the full length of the text storage.
    return [APLIndexedPosition positionWithIndex:self.text.length];
}


/*
 UITextInput protocol required method.
 Return the range between two text positions using our implementation of UITextRange.
 */
- (UITextRange *)textRangeFromPosition:(UITextPosition *)fromPosition toPosition:(UITextPosition *)toPosition
{
    // Generate IndexedPosition instances that wrap the to and from ranges.
    APLIndexedPosition *fromIndexedPosition = (APLIndexedPosition *)fromPosition;
    APLIndexedPosition *toIndexedPosition = (APLIndexedPosition *)toPosition;
    NSRange range = NSMakeRange(MIN(fromIndexedPosition.index, toIndexedPosition.index), ABS(toIndexedPosition.index - fromIndexedPosition.index));

    return [APLIndexedRange indexedRangeWithRange:range];
}


/**
 UITextInput protocol required method.
 Returns the text position at a given offset from another text position using our implementation of UITextPosition.
 */
- (UITextPosition *)positionFromPosition:(UITextPosition *)position offset:(NSInteger)offset
{
    // Generate IndexedPosition instance, and increment index by offset.
    APLIndexedPosition *indexedPosition = (APLIndexedPosition *)position;
    NSInteger end = indexedPosition.index + offset;
    // Verify position is valid in document.
    if (end > self.text.length || end < 0) {
        return nil;
    }
    
    UITextPosition *tp = [APLIndexedPosition positionWithIndex:end];
    
    return tp;
}


/**
 UITextInput protocol required method.
 Returns the text position at a given offset in a specified direction from another text position using our implementation of UITextPosition.
 */
- (UITextPosition *)positionFromPosition:(UITextPosition *)position inDirection:(UITextLayoutDirection)direction offset:(NSInteger)offset
{
    // Note that this sample assumes left-to-right text direction.
    APLIndexedPosition *indexedPosition = (APLIndexedPosition *)position;
    NSInteger newPosition = indexedPosition.index;

    switch ((NSInteger)direction) {
        case UITextLayoutDirectionRight:
            newPosition += offset;
            break;
        case UITextLayoutDirectionLeft:
            newPosition -= offset;
            break;
        UITextLayoutDirectionUp:
        UITextLayoutDirectionDown:
            // This sample does not support vertical text directions.
            break;
    }

    // Verify new position valid in document.

    if (newPosition < 0)
        newPosition = 0;

    if (newPosition > self.text.length)
        newPosition = self.text.length;

    return [APLIndexedPosition positionWithIndex:newPosition];
}


#pragma mark UITextInput - Evaluating Text Positions

/**
 UITextInput protocol required method.
 Return how one text position compares to another text position.
 */
- (NSComparisonResult)comparePosition:(UITextPosition *)position toPosition:(UITextPosition *)other
{
    APLIndexedPosition *indexedPosition = (APLIndexedPosition *)position;
    APLIndexedPosition *otherIndexedPosition = (APLIndexedPosition *)other;

    // For this sample, simply compare position index values.
    if (indexedPosition.index < otherIndexedPosition.index) {
        return NSOrderedAscending;
    }
    if (indexedPosition.index > otherIndexedPosition.index) {
        return NSOrderedDescending;
    }
    return NSOrderedSame;
}


/**
 UITextInput protocol required method.
 Return the number of visible characters between one text position and another text position.
 */
- (NSInteger)offsetFromPosition:(UITextPosition *)from toPosition:(UITextPosition *)toPosition
{
    APLIndexedPosition *fromIndexedPosition = (APLIndexedPosition *)from;
    APLIndexedPosition *toIndexedPosition = (APLIndexedPosition *)toPosition;
    return (toIndexedPosition.index - fromIndexedPosition.index);
}


#pragma mark UITextInput - Text Layout, writing direction and position related methods

/**
 UITextInput protocol method.
 Return the text position that is at the farthest extent in a given layout direction within a range of text.
 */
- (UITextPosition *)positionWithinRange:(UITextRange *)range farthestInDirection:(UITextLayoutDirection)direction
{
    // Note that this sample assumes left-to-right text direction.
    APLIndexedRange *indexedRange = (APLIndexedRange *)range;
    NSInteger position;

    /*
     For this sample, we just return the extent of the given range if the given direction is "forward" in a left-to-right context (UITextLayoutDirectionRight or UITextLayoutDirectionDown), otherwise we return just the range position.
     */
    switch (direction) {
        case UITextLayoutDirectionUp:
        case UITextLayoutDirectionLeft:
            position = indexedRange.range.location;
            break;
        case UITextLayoutDirectionRight:
        case UITextLayoutDirectionDown:
            position = indexedRange.range.location + indexedRange.range.length;
            break;
    }

    // Return text position using our UITextPosition implementation.
    // Note that position is not currently checked against document range.
    return [APLIndexedPosition positionWithIndex:position];
}


/**
 UITextInput protocol required method.
 Return a text range from a given text position to its farthest extent in a certain direction of layout.
 */
- (UITextRange *)characterRangeByExtendingPosition:(UITextPosition *)position inDirection:(UITextLayoutDirection)direction
{
    // Note that this sample assumes left-to-right text direction.
    APLIndexedPosition *pos = (APLIndexedPosition *)position;
    NSRange result;

    switch (direction) {
        case UITextLayoutDirectionUp:
        case UITextLayoutDirectionLeft:
            result = NSMakeRange(pos.index - 1, 1);
            break;
        case UITextLayoutDirectionRight:
        case UITextLayoutDirectionDown:
            result = NSMakeRange(pos.index, 1);
            break;
    }

    // Return range using our UITextRange implementation.
    // Note that range is not currently checked against document range.
    return [APLIndexedRange indexedRangeWithRange:result];
}


/**
 UITextInput protocol required method.
 Return the base writing direction for a position in the text going in a specified text direction.
 */
- (UITextWritingDirection)baseWritingDirectionForPosition:(UITextPosition *)position inDirection:(UITextStorageDirection)direction
{
    // This sample assumes left-to-right text direction and does not support bi-directional or right-to-left text.
    return UITextWritingDirectionLeftToRight;
}


/**
 UITextInput protocol required method.
 Set the base writing direction for a given range of text in a document.
 */
- (void)setBaseWritingDirection:(UITextWritingDirection)writingDirection forRange:(UITextRange *)range
{
    // This sample assumes left-to-right text direction and does not support bi-directional or right-to-left text.
}


#pragma mark UITextInput - Geometry methods

/**
 UITextInput protocol required method.
 Return the first rectangle that encloses a range of text in a document.
 */
- (CGRect)firstRectForRange:(UITextRange *)range
{

    return CGRectMake(0, 0,1,1);
}


/*
 UITextInput protocol required method.
 Return a rectangle used to draw the caret at a given insertion point.
 */
- (CGRect)caretRectForPosition:(UITextPosition *)position
{

    return CGRectMake(0, 0,1,1);
}


#pragma mark UITextInput - Hit testing

/*
 For this sample, hit testing methods are not implemented because there is no implemented mechanism for letting user select text via touches. There is a wide variety of approaches for this (gestures, drag rects, etc) and any approach chosen will depend greatly on the design of the application.
 */

/*
 UITextInput protocol required method.
 Return the position in a document that is closest to a specified point.
 */
- (UITextPosition *)closestPositionToPoint:(CGPoint)point
{
    // Not implemented in this sample. Could utilize underlying APLSimpleCoreTextView:closestIndexToPoint:point.
    return nil;
}

/*
 UITextInput protocol required method.
 Return the position in a document that is closest to a specified point in a given range.
 */
- (UITextPosition *)closestPositionToPoint:(CGPoint)point withinRange:(UITextRange *)range
{
    // Not implemented in this sample. Could utilize underlying APLSimpleCoreTextView:closestIndexToPoint:point.
    return nil;
}

/*
 UITextInput protocol required method.
 Return the character or range of characters that is at a given point in a document.
 */
- (UITextRange *)characterRangeAtPoint:(CGPoint)point
{
    // Not implemented in this sample. Could utilize underlying APLSimpleCoreTextView:closestIndexToPoint:point.
    return nil;
}


/*
 UITextInput protocol required method.
 Return an array of UITextSelectionRects.
 */
- (NSArray *)selectionRectsForRange:(UITextRange *)range
{
    // Not implemented in this sample.
    return nil;
}


#pragma mark UITextInput - Returning Text Styling Information

/*
 UITextInput protocol required method.
 Return a dictionary with properties that specify how text is to be style at a certain location in a document.
 */
- (NSDictionary *)textStylingAtPosition:(UITextPosition *)position inDirection:(UITextStorageDirection)direction
{
    // This sample assumes all text is single-styled, so this is easy.
    //return @{ UITextInputTextFontKey : self.textView.font };
    return nil;
}


#pragma mark UIKeyInput methods

///**
// UIKeyInput protocol required method.
// A Boolean value that indicates whether the text-entry objects have any text.
// */
//- (BOOL)hasText
//{
//    return (self.text.length != 0);
//}
//
//
///**
// UIKeyInput protocol required method.
// Insert a character into the displayed text. Called by the text system when the user has entered simple text.
// */
//- (void)insertText:(NSString *)text
//{
//    NSRange selectedNSRange = self.inSelectedTextRange;
//    NSRange markedTextRange = self.inMarkedTextRange;
//
//    /*
//     While this sample does not provide a way for the user to create marked or selected text, the following code still checks for these ranges and acts accordingly.
//     */
//    if (markedTextRange.location != NSNotFound) {
//        // There is marked text -- replace marked text with user-entered text.
//        [self.text replaceCharactersInRange:markedTextRange withString:text];
//        selectedNSRange.location = markedTextRange.location + text.length;
//        selectedNSRange.length = 0;
//        markedTextRange = NSMakeRange(NSNotFound, 0);
//    } else if (selectedNSRange.length > 0) {
//        // Replace selected text with user-entered text.
//        [self.text replaceCharactersInRange:selectedNSRange withString:text];
//        selectedNSRange.length = 0;
//        selectedNSRange.location += text.length;
//    } else {
//        // Insert user-entered text at current insertion point.
//        [self.text insertString:text atIndex:selectedNSRange.location];
//        selectedNSRange.location += text.length;
//    }
//
//    // Update underlying APLSimpleCoreTextView.
//    self.contentText = self.text;
//    self.inMarkedTextRange = markedTextRange;
//    self.inSelectedTextRange = selectedNSRange;
//    
//        if (_glfmDisplay->charFunc) {
//            _glfmDisplay->charFunc(_glfmDisplay, text.UTF8String, 0);
//        }
//}
//
//
///**
// UIKeyInput protocol required method.
// Delete a character from the displayed text. Called by the text system when the user is invoking a delete (e.g. pressing the delete software keyboard key).
// */
//- (void)deleteBackward
//{
//    NSRange selectedNSRange = self.inSelectedTextRange;
//    NSRange markedTextRangeLocal = self.inMarkedTextRange;
//
//    /*
//     Note: While this sample does not provide a way for the user to create marked or selected text, the following code still checks for these ranges and acts accordingly.
//     */
//    if (markedTextRangeLocal.location != NSNotFound) {
//        // There is marked text, so delete it.
//        [self.text deleteCharactersInRange:markedTextRangeLocal];
//        selectedNSRange.location = markedTextRangeLocal.location;
//        selectedNSRange.length = 0;
//        markedTextRangeLocal = NSMakeRange(NSNotFound, 0);
//    }
//    else if (selectedNSRange.length > 0) {
//        // Delete the selected text.
//        [self.text deleteCharactersInRange:selectedNSRange];
//        selectedNSRange.length = 0;
//    }
//    else if (selectedNSRange.location > 0) {
//        // Delete one char of text at the current insertion point.
//        selectedNSRange.location--;
//        selectedNSRange.length = 1;
//        [self.text deleteCharactersInRange:selectedNSRange];
//        selectedNSRange.length = 0;
//    }
//
//    // Update underlying APLSimpleCoreTextView.
//    self.contentText = self.text;
//    self.inMarkedTextRange = markedTextRangeLocal;
//    self.inSelectedTextRange = selectedNSRange;
//    
//        if (_glfmDisplay->keyFunc) {
//            _glfmDisplay->keyFunc(_glfmDisplay, GLFMKeyBackspace, GLFMKeyActionPressed, 0);
//            _glfmDisplay->keyFunc(_glfmDisplay, GLFMKeyBackspace, GLFMKeyActionReleased, 0);
//        }
//}



- (BOOL)hasText {
    return YES;
}

- (void)insertText:(NSString *)text {
    if (_glfmDisplay->charFunc) {
        _glfmDisplay->charFunc(_glfmDisplay, text.UTF8String, 0);
    }
}

- (void)deleteBackward {
    if (_glfmDisplay->keyFunc) {
        _glfmDisplay->keyFunc(_glfmDisplay, GLFMKeyBackspace, GLFMKeyActionPressed, 0);
        _glfmDisplay->keyFunc(_glfmDisplay, GLFMKeyBackspace, GLFMKeyActionReleased, 0);
    }
}


#pragma mark - 拍照并保存
- (void)takePhotoAction:(int) puid:(int) type {
    //    BOOL isCamera = [UIImagePickerController isCameraDeviceAvailable:UIImagePickerControllerCameraDeviceRear];
    BOOL isCamera =  [UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera];
    if (!isCamera) { //若不可用，弹出警告框
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"No Camera" message:nil delegate:self cancelButtonTitle:@"Ok" otherButtonTitles:nil, nil];
        [alert show];
        return;
    }
    _pickerUid=puid;
    _pickerType=type;
    UIImagePickerController *imagePicker = [[UIImagePickerController alloc] init];
    imagePicker.sourceType = UIImagePickerControllerSourceTypeCamera;

    NSArray *availableMedia = [UIImagePickerController availableMediaTypesForSourceType:UIImagePickerControllerSourceTypeCamera];//Camera所支持的Media格式都有哪些,共有两个分别是@"public.image",@"public.movie"
    [imagePicker setMediaTypes:availableMedia];
    [self presentViewController:imagePicker animated:YES completion:nil];
    imagePicker.videoMaximumDuration = 30.0f;//30秒
    [imagePicker setAllowsEditing:YES];

    /**
     *      UIImagePickerControllerSourceTypePhotoLibrary  ->所有资源文件夹
     UIImagePickerControllerSourceTypeCamera        ->摄像头
     UIImagePickerControllerSourceTypeSavedPhotosAlbum ->内置相册
     */
    imagePicker.delegate = self;    //设置代理，遵循UINavigationControllerDelegate,UIImagePickerControllerDelegate协议

}

#pragma mark - 访问相册
- (void)browseAlbum:(int) puid : (int) type {
    UIImagePickerController *imagePicker = [[UIImagePickerController alloc] init];
    _pickerUid=puid;
    _pickerType=type;
    imagePicker.sourceType = UIImagePickerControllerSourceTypeSavedPhotosAlbum;
    NSArray *availableMedia = [UIImagePickerController availableMediaTypesForSourceType:UIImagePickerControllerSourceTypeCamera];//Camera所支持的Media格式都有哪些,共有两个分别是@"public.image",@"public.movie"
    [imagePicker setMediaTypes:availableMedia];
    [self presentViewController:imagePicker animated:YES completion:nil];
    imagePicker.delegate = self;
}

#pragma mark - 协议方法的实现
- (void)saveVideo:(NSURL *)outputFileURL
{
    //ALAssetsLibrary提供了我们对iOS设备中的相片、视频的访问。
    ALAssetsLibrary *library = [[ALAssetsLibrary alloc] init];
    [library writeVideoAtPathToSavedPhotosAlbum:outputFileURL completionBlock:^(NSURL *assetURL, NSError *error) {
        if (error) {
            NSLog(@"保存视频失败:%@",error);
        } else {
            NSLog(@"保存视频到相册成功");
        }
    }];
}

//协议方法，选择完毕以后，呈现在imageShow里面
- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info {
    //NSLog(@"%@",info);  //UIImagePickerControllerMediaType,UIImagePickerControllerOriginalImage,UIImagePickerControllerReferenceURL
    NSString *mediaType = info[@"UIImagePickerControllerMediaType"];
    if ([mediaType isEqualToString:@"public.image"]) {  //判断是否为图片

        UIImage *image = [info objectForKey:UIImagePickerControllerOriginalImage];
        if((_pickerType&0x100)!=0){//autofit to 1024
            float iw=image.size.width;
            float ih=image.size.height;
            float ratio=1024;
            float scale=iw/ratio>ih/ratio?(iw/ratio):(ih/ratio);
            if(scale>1){
                image = [GLFMViewController resizeImage:image toSize:(CGSize)CGSizeMake(iw/scale, ih/scale)];
            }
        }

        NSData *data=UIImageJPEGRepresentation(image, 0.75);
        //self.imageShow.image = image;
        const char *savePath=glfmGetSaveRoot();

        NSURL *nsurl=[info objectForKey:UIImagePickerControllerReferenceURL];
        NSString *nss=[nsurl path];
        NSString *fileName = [NSString stringWithFormat:@"%ld.jpg",time(NULL)];
        NSString *path=[NSString stringWithFormat:@"%s/%@",savePath,fileName];
        const char* url=[path UTF8String];
        
        [data writeToFile:path atomically:YES];

        char *cd=(char*)[data bytes];
        int len=(int)[data length];
        _glfmDisplay->pickerFunc(_glfmDisplay, _pickerUid, url, cd, len);//把路径和数据都返回到java层

        //通过判断picker的sourceType，如果是拍照则保存到相册去
        if (picker.sourceType == UIImagePickerControllerSourceTypeCamera) {
            UIImageWriteToSavedPhotosAlbum(image, self, @selector(image:didFinishSavingWithError:contextInfo:), nil);
        }
    } else if([mediaType isEqualToString:@"public.movie"]){
        NSURL *videoURL = [info objectForKey:UIImagePickerControllerMediaURL];
        const char *savePath=glfmGetSaveRoot();
//        NSString *path = [[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true) lastObject] stringByAppendingPathComponent:[NSString stringWithFormat:@"%ld_compressedVideo.mp4",time(NULL)]];
        NSString *fileName = [NSString stringWithFormat:@"%ld.mp4",time(NULL)];
        NSString *path=[NSString stringWithFormat:@"%s/%@",savePath,fileName];
        NSLog(@"compressedVideoSavePath : %@",path);
        //if(![path isEqualToString:@".mp4"]){
            //压缩
            AVURLAsset *avAsset = [[AVURLAsset alloc] initWithURL:videoURL options:nil];
            NSArray *compatiblePresets = [AVAssetExportSession exportPresetsCompatibleWithAsset:avAsset];
            if ([compatiblePresets containsObject:AVAssetExportPresetHighestQuality]) {
                AVAssetExportSession *exportSession = [[AVAssetExportSession alloc] initWithAsset:avAsset presetName:AVAssetExportPreset640x480];
                //输出URL
                exportSession.outputURL = [NSURL fileURLWithPath:path];
                //优化网络
                exportSession.shouldOptimizeForNetworkUse = true;
                //转换后的格式
                exportSession.outputFileType = AVFileTypeMPEG4;
                //异步导出
                [exportSession exportAsynchronouslyWithCompletionHandler:^{
                    // 如果导出的状态为完成
                    if ([exportSession status] == AVAssetExportSessionStatusCompleted) {
                        [self saveVideo:[NSURL fileURLWithPath: path]];
                        NSData *data = [NSData dataWithContentsOfURL:exportSession.outputURL];
                        char *cd = (char*)[data bytes];
                        int len = [data length];
                        NSLog(@"压缩完毕,压缩后大小 %f MB",(CGFloat)len / 1024 / 1024);
                        const char* url=[path UTF8String];
                        self->_glfmDisplay->pickerFunc(self->_glfmDisplay, self->_pickerUid, url, cd, len);
                    }else{
                        NSLog(@"当前压缩进度:%f",exportSession.progress);
                    }
                    NSLog(@"%@",exportSession.error);

                }];
            }
        //}
    }

    [picker dismissViewControllerAnimated:YES completion:nil];
}

//此方法就在UIImageWriteToSavedPhotosAlbum的上方
- (void)image:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo {
    NSLog(@"Image Saved");
}


+(UIImage *)cropImage:(UIImage *)image inRect:(CGRect)rect{

    //将UIImage转换成CGImageRef
    CGImageRef sourceImageRef = [image CGImage];

    //按照给定的矩形区域进行剪裁
    CGImageRef newImageRef = CGImageCreateWithImageInRect(sourceImageRef, rect);

    //将CGImageRef转换成UIImage
    UIImage *newImage = [UIImage imageWithCGImage:newImageRef];

    // 调用这个方法 否则会造成内存泄漏 楼主可以检测下
    CGImageRelease(newImageRef);

    //返回剪裁后的图片
    return newImage;
}

/**
 *根据给定的size的宽高比自动缩放原图片、自动判断截取位置,进行图片截取
 * UIImage image 原始的图片
 * CGSize size 截取图片的size
 */
+(UIImage *)resizeCropImage:(UIImage *)image toRect:(CGSize)size{

    //被切图片宽比例比高比例小 或者相等，以图片宽进行放大
    if (image.size.width*size.height <= image.size.height*size.width) {

        //以被剪裁图片的宽度为基准，得到剪切范围的大小
        CGFloat width  = image.size.width;
        CGFloat height = image.size.width * size.height / size.width;

        // 调用剪切方法
        // 这里是以中心位置剪切，也可以通过改变rect的x、y值调整剪切位置
        return [GLFMViewController cropImage:image inRect:CGRectMake(0, (image.size.height -height)/2, width, height)];

    }else{ //被切图片宽比例比高比例大，以图片高进行剪裁

        // 以被剪切图片的高度为基准，得到剪切范围的大小
        CGFloat width  = image.size.height * size.width / size.height;
        CGFloat height = image.size.height;

        // 调用剪切方法
        // 这里是以中心位置剪切，也可以通过改变rect的x、y值调整剪切位置
        return [GLFMViewController cropImage:image inRect:CGRectMake((image.size.width -width)/2, 0, width, height)];
    }
    return nil;
}

+ (UIImage *)resizeImage:(UIImage *)image toSize:(CGSize)reSize{

    UIGraphicsBeginImageContext(CGSizeMake(reSize.width, reSize.height));

    [image drawInRect:CGRectMake(0, 0, reSize.width, reSize.height)];

    UIImage *reSizeImage = UIGraphicsGetImageFromCurrentImageContext();

    UIGraphicsEndImageContext();

    return reSizeImage;

}

- (void)targetAction:(UIButton *)button {
    UIView *videoPanel = button.superview;
    AVPlayer *avplayer = [videoPanel.layer valueForKey:@"AVPLAYER"];
    
    switch (button.tag) {
        case 555: // CLOSE button
            if (avplayer) {
                [avplayer pause];
                [videoPanel removeFromSuperview];
                [[AVPlayerLayer playerLayerWithPlayer:avplayer] removeFromSuperlayer];
                avplayer = NULL;
            }
            break;
            
        case 556: // PAUSE button
            if (avplayer && avplayer.rate != 0) {
                [avplayer pause];
            }
            break;
            
        case 557: // PLAY button
            if (avplayer) {
                AVPlayerItem *playerItem = avplayer.currentItem;
                if (playerItem.currentTime.value == playerItem.duration.value) {
                    // Video has finished playing, seek to beginning and play
                    [avplayer seekToTime:CMTimeMake(0, 1)];
                    [avplayer play];
                } else if (avplayer.rate == 0) {
                    // Video is paused, resume playing
                    [avplayer play];
                }
                // If video is already playing, do nothing
            }
            break;
    }
}

@end

#pragma mark - Application Delegate

@implementation GLFMAppDelegate

- (BOOL)application:(UIApplication *)application
    didFinishLaunchingWithOptions:(NSDictionary<UIApplicationLaunchOptionsKey, id> *)launchOptions {
    _active = YES;
    self.window = GLFM_AUTORELEASE([[UIWindow alloc] init]);
    if (self.window.bounds.size.width <= 0.0 || self.window.bounds.size.height <= 0.0) {
        // Set UIWindow frame for iOS 8.
        // On iOS 9, the UIWindow frame may be different than the UIScreen bounds for iPad's
        // Split View or Slide Over.
        self.window.frame = [[UIScreen mainScreen] bounds];
    }
    self.window.rootViewController = GLFM_AUTORELEASE([[GLFMViewController alloc] init]);
    [self.window makeKeyAndVisible];


    if (@available(iOS 10.0, *)) {
        //iOS10特有
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        // 必须写代理，不然无法监听通知的接收与点击
        center.delegate = self;
        [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert | UNAuthorizationOptionBadge | UNAuthorizationOptionSound) completionHandler:^(BOOL granted, NSError * _Nullable error) {
            if (granted) {
                // 点击允许
                //NSLog(@"注册成功");
                [center getNotificationSettingsWithCompletionHandler:^(UNNotificationSettings * _Nonnull settings) {
                    //NSLog(@"%@", settings);
                }];
            } else {
                // 点击不允许
                //NSLog(@"注册失败");
            }
        }];
    }else if (@available(iOS 8.0, *)){
        //iOS8 - iOS10
        [application registerUserNotificationSettings:[UIUserNotificationSettings settingsForTypes:UIUserNotificationTypeAlert | UIUserNotificationTypeSound | UIUserNotificationTypeBadge categories:nil]];

    }else {
        //iOS8系统以下
        [application registerForRemoteNotificationTypes:UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeAlert | UIRemoteNotificationTypeSound];
    }
    // 注册获得device Token
    [[UIApplication sharedApplication] registerForRemoteNotifications];

    //register lock screen event
//    CFNotificationCenterAddObserver(CFNotificationCenterGetDarwinNotifyCenter(), NULL, screenLockStateChanged, NotificationLock, NULL, CFNotificationSuspensionBehaviorDeliverImmediately);
//    CFNotificationCenterAddObserver(CFNotificationCenterGetDarwinNotifyCenter(), NULL, screenLockStateChanged, NotificationChange, NULL, CFNotificationSuspensionBehaviorDeliverImmediately);


    //setScreenStateCb();

    return YES;
}
////lock screen
//static void screenLockStateChanged(CFNotificationCenterRef center,void* observer,CFStringRef name,const void*object,CFDictionaryRef userInfo)
//{
//    NSString* lockstate = (__bridge NSString*)name;
//    if ([lockstate isEqualToString:(__bridge NSString*)NotificationLock]) {
//        NSLog(@"locked.");
//    } else {
//        NSLog(@"lock state changed.");
//    }
//}

//gust {
void setDeviceToken(GLFMDisplay * display, const char *deviceToken) {
    static char *key="glfm.device.token";

    if(display->notifyFunc){
        display->notifyFunc(display,key,deviceToken);
    }
}

// 获得Device Token
- (void)application:(UIApplication *)application
didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    GLFMViewController *vc = (GLFMViewController *)[self.window rootViewController];
    //NSLog(@"%@", [NSString stringWithFormat:@"Device Token: %@", deviceToken]);

    
    NSData *data = deviceToken;
    if ([[[UIDevice currentDevice] systemVersion] floatValue] >= 13) {
        
        if (![data isKindOfClass:[NSData class]]) {
            setDeviceToken(vc.glfmDisplay,NULL);
        }
        NSUInteger len = [data length];
        char *chars = (char *)[data bytes];
        NSMutableString *hexString = [[NSMutableString alloc]init];
        for (NSUInteger i=0; i<len; i++) {
            [hexString appendString:[NSString stringWithFormat:@"%0.2hhx" , chars[i]]];
            if((i+1)%4==0){
                //[hexString appendString:@" "];
            }
        }
        
        const char * token=[hexString UTF8String];
        setDeviceToken(vc.glfmDisplay,token);
    } else {
        NSString *myToken = [[data description] stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@"<>"]];
        //myToken = [myToken stringByReplacingOccurrencesOfString:@" " withString:@""];
        
        const char * token=[myToken UTF8String];
        setDeviceToken(vc.glfmDisplay,token);
//        NSString * newStr = [[[[deviceToken description]
//                               stringByReplacingOccurrencesOfString:@"<" withString:@""]
//                              stringByReplacingOccurrencesOfString:@">" withString:@""]
//                             stringByReplacingOccurrencesOfString:@"" withString:@""];
//        //NSString* newStr = [[NSString alloc] initWithData:deviceToken encoding:NSUTF8StringEncoding];
//        if(newStr != nil){
//            const char * token=[newStr UTF8String];
//            setDeviceToken(vc.glfmDisplay,token);
//        }else{
//            setDeviceToken(vc.glfmDisplay,NULL);
//        }
    }
}
// 获得Device Token失败
- (void)application:(UIApplication *)application
didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
    GLFMViewController *vc = (GLFMViewController *)[self.window rootViewController];
    //NSLog(@"did Fail To Register For Remote Notifications With Error: %@", error);
    setDeviceToken(vc.glfmDisplay,NULL);
}


// iOS 10收到通知
- (void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler API_AVAILABLE(ios(10.0)){
    NSDictionary * userInfo = notification.request.content.userInfo;
    UNNotificationRequest *request = notification.request; // 收到推送的请求
    UNNotificationContent *content = request.content; // 收到推送的消息内容
    NSNumber *badge = content.badge;  // 推送消息的角标
    NSString *body = content.body;    // 推送消息体
    UNNotificationSound *sound = content.sound;  // 推送消息的声音
    NSString *subtitle = content.subtitle;  // 推送消息的副标题
    NSString *title = content.title;  // 推送消息的标题

    if([notification.request.trigger isKindOfClass:[UNPushNotificationTrigger class]]) {
        NSLog(@"iOS10 前台收到远程通知:%@", userInfo);

    }
    else {
        // 判断为本地通知
        //NSLog(@"iOS10 前台收到本地通知:{\\\\nbody:%@，\\\\ntitle:%@,\\\\nsubtitle:%@,\\\\nbadge：%@，\\\\nsound：%@，\\\\nuserInfo：%@\\\\n}",body,title,subtitle,badge,sound,userInfo);
    }
    completionHandler(UNNotificationPresentationOptionBadge|UNNotificationPresentationOptionSound|UNNotificationPresentationOptionAlert); // 需要执行这个方法，选择是否提醒用户，有Badge、Sound、Alert三种类型可以设置
}

// 通知的点击事件
- (void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void(^)(void))completionHandler API_AVAILABLE(ios(10.0)){

    NSDictionary * userInfo = response.notification.request.content.userInfo;
    UNNotificationRequest *request = response.notification.request; // 收到推送的请求
    UNNotificationContent *content = request.content; // 收到推送的消息内容
    NSNumber *badge = content.badge;  // 推送消息的角标
    NSString *body = content.body;    // 推送消息体
    UNNotificationSound *sound = content.sound;  // 推送消息的声音
    NSString *subtitle = content.subtitle;  // 推送消息的副标题
    NSString *title = content.title;  // 推送消息的标题
    if([response.notification.request.trigger isKindOfClass:[UNPushNotificationTrigger class]]) {
        //NSLog(@"iOS10 收到远程通知:%@", userInfo);

    }
    else {
        // 判断为本地通知
        //NSLog(@"iOS10 收到本地通知:{\\\\nbody:%@，\\\\ntitle:%@,\\\\nsubtitle:%@,\\\\nbadge：%@，\\\\nsound：%@，\\\\nuserInfo：%@\\\\n}",body,title,subtitle,badge,sound,userInfo);
    }

    // Warning: UNUserNotificationCenter delegate received call to -userNotificationCenter:didReceiveNotificationResponse:withCompletionHandler: but the completion handler was never called.
    completionHandler();  // 系统要求执行这个方法

}

- (void)application:(UIApplication *)application
didReceiveRemoteNotification:(NSDictionary *)userInfo {
    //NSLog(@"iOS6及以下系统，收到通知:%@", userInfo);
}

- (void)application:(UIApplication *)application
didReceiveRemoteNotification:(NSDictionary *)userInfo
fetchCompletionHandler:
(void (^)(UIBackgroundFetchResult))completionHandler {

    //NSLog(@"iOS7及以上系统，收到通知:%@", userInfo);
    completionHandler(UIBackgroundFetchResultNewData);
}
//gust }

- (void)setActive:(BOOL)active {
    if (_active != active) {
        _active = active;

        GLFMViewController *vc = (GLFMViewController *)[self.window rootViewController];
        if (vc.glfmDisplay && vc.glfmDisplay->focusFunc) {
            vc.glfmDisplay->focusFunc(vc.glfmDisplay, _active);
        }
        if (vc.isViewLoaded) {
            if (!active) {
                // Draw once when entering the background so that a game can show "paused" state.
                [vc.glfmView requestRefresh];
                [vc.glfmView draw];
            }
            vc.glfmView.animating = active;
        }
#if TARGET_OS_IOS
        if (vc.isMotionManagerLoaded) {
            [vc updateMotionManagerActiveState];
        }
#endif
        [vc clearTouches];
    }
}

- (void)applicationWillResignActive:(UIApplication *)application {
    self.active = NO;
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
    self.active = NO;
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    self.active = YES;
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
    self.active = YES;
}

- (void)applicationWillTerminate:(UIApplication *)application {
    self.active = NO;
}

- (void)dealloc {
#if !__has_feature(objc_arc)
    self.window = nil;
    [super dealloc];
#endif
}

@end

#pragma mark - Main

int main(int argc, char *argv[]) {
    @autoreleasepool {
        return UIApplicationMain(argc, argv, nil, NSStringFromClass([GLFMAppDelegate class]));
    }
}

#pragma mark - GLFM implementation

double glfmGetTime() {
    return CACurrentMediaTime();
}

GLFMProc glfmGetProcAddress(const char *functionName) {
    static void *handle = NULL;
    if (!handle) {
        handle = dlopen(NULL, RTLD_LAZY);
    }
    return handle ? (GLFMProc)dlsym(handle, functionName) : NULL;
}

void glfmSwapBuffers(GLFMDisplay *display) {
    if (display && display->platformData) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        if (vc.isViewLoaded) {
            [vc.glfmView swapBuffers];
        }
    }
}

void glfmSetSupportedInterfaceOrientation(GLFMDisplay *display, GLFMInterfaceOrientation supportedOrientations) {
    if (display) {
        if (display->supportedOrientations != supportedOrientations) {
            display->supportedOrientations = supportedOrientations;

            // HACK: Notify that the value of supportedInterfaceOrientations has changed
            GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
            if (vc.isViewLoaded && vc.view.window) {
                
                //================================= gust add =======================
                [vc setInterfaceOrientation:supportedOrientations];
                //================================= gust add =======================
                [vc.glfmView requestRefresh];
                UIViewController *dummyVC = GLFM_AUTORELEASE([[UIViewController alloc] init]);
                dummyVC.view = GLFM_AUTORELEASE([[UIView alloc] init]);
                [vc presentViewController:dummyVC animated:NO completion:^{
                    [vc dismissViewControllerAnimated:NO completion:NULL];
                }];
            }
        }
    }
}

GLFMInterfaceOrientation glfmGetInterfaceOrientation(GLFMDisplay *display) {
    (void)display;
#if TARGET_OS_IOS
    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
    switch (orientation) {
        case UIInterfaceOrientationPortrait:
            return GLFMInterfaceOrientationPortrait;
        case UIInterfaceOrientationPortraitUpsideDown:
            return GLFMInterfaceOrientationPortraitUpsideDown;
        case UIInterfaceOrientationLandscapeLeft:
            return GLFMInterfaceOrientationLandscapeLeft;
        case UIInterfaceOrientationLandscapeRight:
            return GLFMInterfaceOrientationLandscapeRight;
        case UIInterfaceOrientationUnknown: default:
            return GLFMInterfaceOrientationUnknown;
    }
#else
    return GLFMInterfaceOrientationUnknown;
#endif
}

static void glfm__preferredDrawableSize(CGRect bounds, CGFloat contentScaleFactor, int *width, int *height) {
    int newDrawableWidth = (int)(bounds.size.width * contentScaleFactor);
    int newDrawableHeight = (int)(bounds.size.height * contentScaleFactor);

    // On the iPhone 6 when "Display Zoom" is set, the size will be incorrect.
    if (contentScaleFactor == 2.343750) {
        if (newDrawableWidth == 750 && newDrawableHeight == 1331) {
            newDrawableHeight = 1334;
        } else if (newDrawableWidth == 1331 && newDrawableHeight == 750) {
            newDrawableWidth = 1334;
        }
    }
    *width = newDrawableWidth;
    *height = newDrawableHeight;
}

void glfmGetDisplaySize(GLFMDisplay *display, int *width, int *height) {
    if (display && display->platformData) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        if (vc.isViewLoaded) {
            *width = vc.glfmView.drawableWidth;
            *height = vc.glfmView.drawableHeight;
        } else {
            glfm__preferredDrawableSize(UIScreen.mainScreen.bounds, UIScreen.mainScreen.nativeScale, width, height);
        }
    } else {
        *width = 0;
        *height = 0;
    }
}

double glfmGetDisplayScale(GLFMDisplay *display) {
    (void)display;
    return [UIScreen mainScreen].nativeScale;
}

void glfmGetDisplayChromeInsets(GLFMDisplay *display, double *top, double *right, double *bottom,
                                double *left) {
    if (display && display->platformData) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        if (!vc.isViewLoaded) {
            *top = 0.0;
            *right = 0.0;
            *bottom = 0.0;
            *left = 0.0;
        } else if (@available(iOS 11, tvOS 11, *)) {
            UIEdgeInsets insets = vc.view.safeAreaInsets;
            *top = insets.top * vc.view.contentScaleFactor;
            *right = insets.right * vc.view.contentScaleFactor;
            *bottom = insets.bottom * vc.view.contentScaleFactor;
            *left = insets.left * vc.view.contentScaleFactor;
        } else {
#if TARGET_OS_IOS
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
            if (![vc prefersStatusBarHidden]) {
                *top = ([UIApplication sharedApplication].statusBarFrame.size.height *
                        vc.view.contentScaleFactor);
            } else {
                *top = 0.0;
            }
#pragma clang diagnostic pop
#else
            *top = 0.0;
#endif
            *right = 0.0;
            *bottom = 0.0;
            *left = 0.0;
        }
    } else {
        *top = 0.0;
        *right = 0.0;
        *bottom = 0.0;
        *left = 0.0;
    }
}

void glfm__displayChromeUpdated(GLFMDisplay *display) {
    if (display && display->platformData) {
#if TARGET_OS_IOS
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        if (vc.isViewLoaded) {
            [vc.glfmView requestRefresh];
        }
        [vc setNeedsStatusBarAppearanceUpdate];
        if (@available(iOS 11, *)) {
            [vc setNeedsUpdateOfScreenEdgesDeferringSystemGestures];
        }
#endif
    }
}

GLFMRenderingAPI glfmGetRenderingAPI(GLFMDisplay *display) {
    if (display && display->platformData) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        if (vc.isViewLoaded) {
            return vc.glfmView.renderingAPI;
        } else {
            return GLFMRenderingAPIOpenGLES2;
        }
    } else {
        return GLFMRenderingAPIOpenGLES2;
    }
}

bool glfmHasTouch(GLFMDisplay *display) {
    (void)display;
    return true;
}

void glfmSetMouseCursor(GLFMDisplay *display, GLFMMouseCursor mouseCursor) {
    (void)display;
    (void)mouseCursor;
    // Do nothing
}

void glfmSetMultitouchEnabled(GLFMDisplay *display, bool multitouchEnabled) {
#if TARGET_OS_IOS
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        vc.multipleTouchEnabled = (BOOL)multitouchEnabled;
        if (vc.isViewLoaded) {
            vc.view.multipleTouchEnabled = (BOOL)multitouchEnabled;
        }
    }
#else
    (void)display;
    (void)multitouchEnabled;
#endif
}

bool glfmGetMultitouchEnabled(GLFMDisplay *display) {
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        return vc.multipleTouchEnabled;
    } else {
        return false;
    }
}

void glfmSetKeyboardVisible(GLFMDisplay *display, bool visible) {
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        vc.keyboardRequested = visible;
        if (visible) {
            [vc becomeFirstResponder];
        } else {
            [vc resignFirstResponder];
        }
    }
}

bool glfmIsKeyboardVisible(GLFMDisplay *display) {
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        return vc.keyboardRequested;
    } else {
        return false;
    }
}

bool glfmIsSensorAvailable(GLFMDisplay *display, GLFMSensor sensor) {
#if TARGET_OS_IOS
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        switch (sensor) {
            case GLFMSensorAccelerometer:
                return vc.motionManager.deviceMotionAvailable && vc.motionManager.accelerometerAvailable;
            case GLFMSensorMagnetometer:
                return vc.motionManager.deviceMotionAvailable && vc.motionManager.magnetometerAvailable;
            case GLFMSensorGyroscope:
                return vc.motionManager.deviceMotionAvailable && vc.motionManager.gyroAvailable;
            case GLFMSensorRotationMatrix:
                return (vc.motionManager.deviceMotionAvailable &&
                        ([CMMotionManager availableAttitudeReferenceFrames] & CMAttitudeReferenceFrameXMagneticNorthZVertical));
        }
    }
    return false;
#else
    (void)display;
    (void)sensor;
    return false;
#endif
}

void glfm__sensorFuncUpdated(GLFMDisplay *display) {
#if TARGET_OS_IOS
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        [vc updateMotionManagerActiveState];
    }
#else
    (void)display;
#endif
}

bool glfmIsHapticFeedbackSupported(GLFMDisplay *display) {
    (void)display;
#if TARGET_OS_IOS
    if (@available(iOS 13, *)) {
        return [CHHapticEngine capabilitiesForHardware].supportsHaptics;
    } else {
        return false;
    }
#else
    return false;
#endif
}

void glfmPerformHapticFeedback(GLFMDisplay *display, GLFMHapticFeedbackStyle style) {
    (void)display;
#if TARGET_OS_IOS
    if (@available(iOS 10, *)) {
        UIImpactFeedbackStyle uiStyle;
        switch (style) {
            case GLFMHapticFeedbackLight: default:
                uiStyle = UIImpactFeedbackStyleLight;
                break;
            case GLFMHapticFeedbackMedium:
                uiStyle = UIImpactFeedbackStyleMedium;
                break;
            case GLFMHapticFeedbackHeavy:
                uiStyle = UIImpactFeedbackStyleHeavy;
                break;
        }
        UIImpactFeedbackGenerator *generator = [[UIImpactFeedbackGenerator alloc] initWithStyle:uiStyle];
        [generator impactOccurred];
        GLFM_RELEASE(generator);
    }
#else
    (void)style;
#endif
}

// MARK: Platform-specific functions

bool glfmIsMetalSupported(GLFMDisplay *display) {
#if GLFM_INCLUDE_METAL
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        return (vc.metalDevice != nil);
    }
#endif
    return false;
}

void *glfmGetMetalView(GLFMDisplay *display) {
#if GLFM_INCLUDE_METAL
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        if (vc.isViewLoaded) {
            UIView *view = vc.view;
            if ([view isKindOfClass:[MTKView class]]) {
                return (__bridge void *)view;
            }
        }
    }
#endif
    return NULL;
}

void *glfmGetUIViewController(GLFMDisplay *display) {
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        return (__bridge void *)vc;
    } else {
        return NULL;
    }
}




const char* glfmGetResRoot(){

    NSString *resPath= [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:@""];
    //printf("respath: %s\n\n",[resPath UTF8String]);

    return [resPath UTF8String];
}

const char* glfmGetSaveRoot(void){

    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    NSString *cachesDir = [paths objectAtIndex:0];

    return [cachesDir UTF8String];
}

const char* glfmGetUUID(void){
    NSString* Identifier = [[[UIDevice currentDevice] identifierForVendor] UUIDString];
    return [Identifier UTF8String];
}


const char* getClipBoardContent(void) {
    UIPasteboard* pBoard=[UIPasteboard generalPasteboard];
    if(pBoard!=NULL) {
        NSString* pNsStr=pBoard.string;
        if(pNsStr!=NULL) {
            return [pNsStr UTF8String];
        } else {
            return NULL;
        }
    } else {
        return NULL;
    }
}

void setClipBoardContent(const char *str){
    if(!str)return;

    NSString *nstr= [[NSString alloc] initWithCString:(const char*)str
                                             encoding:NSUTF8StringEncoding];
    UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
    pasteboard.string = nstr;
}

const char* getOsName(){
    return "iOS";
}

void getOsLanguage(char *buf, int bufSize) {
    CFArrayRef localeIDs = CFLocaleCopyPreferredLanguages();
    if (localeIDs&&bufSize > 0) {
        CFStringRef localeID = (CFStringRef)CFArrayGetValueAtIndex(localeIDs, 0);
        if (!CFStringGetCString(localeID, buf, bufSize, kCFStringEncodingUTF8)){
            buf[0] = 0;
        }
        CFRelease(localeIDs);
    } else {
        if(bufSize > 0){
            buf[0] = 0;
        }
    }
}

void pickPhotoAlbum(GLFMDisplay *display, int uid, int type){
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        [vc browseAlbum:uid:type];
    }
}

void pickPhotoCamera(GLFMDisplay *display, int uid, int type){
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        [vc takePhotoAction:uid:type];
    }

}

void imageCrop(GLFMDisplay *display, int uid, const char *cpath,int x,int y, int width, int height){
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        NSString *path = [[NSString alloc] initWithCString:cpath encoding:NSUTF8StringEncoding];
        NSURL *url = [NSURL URLWithString:path];
        NSData *data = [NSData dataWithContentsOfURL:url];
        UIImage *img = [[UIImage alloc] initWithData:data];
        UIImage *image=[GLFMViewController cropImage:(UIImage *)img inRect:(CGRect)CGRectMake(x,y,width,height)];
        NSData *datajpg=UIImageJPEGRepresentation(image, 0.75);

        char *cd=(char*)[datajpg bytes];
        int len=(int)[datajpg length];
        display->pickerFunc(display,vc.pickerUid, NULL, cd, len);
    }
}

void *playVideo(GLFMDisplay *display, char *cpath, char *mimeType) {
    if (display) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        int x = 0;
        int y = 0;
        int w = vc.view.frame.size.width;
        int h = vc.view.frame.size.height;
        //cpath = "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4";
        NSString *nspath = [[NSString alloc] initWithCString:cpath encoding:NSUTF8StringEncoding];
        NSURL *url = [NSURL fileURLWithPath:nspath];

        UIView *videoPanel = [[UIView alloc]initWithFrame:CGRectMake(0, 0, vc.view.bounds.size.width, vc.view.bounds.size.height)];
        videoPanel.backgroundColor = [UIColor whiteColor];

        NSString * nsMimeType = [[NSString alloc] initWithCString:mimeType encoding:NSUTF8StringEncoding];

        AVPlayer * avplayer;
        if(mimeType){
            AVURLAsset * asset = [[AVURLAsset alloc] initWithURL:url options:@{@"AVURLAssetOutOfBandMIMETypeKey": nsMimeType}];
            avplayer = [AVPlayer playerWithPlayerItem:[AVPlayerItem playerItemWithAsset:asset]];
        } else {
            AVPlayerItem *avplayerItem = [[AVPlayerItem alloc] initWithURL:url];
            //创建监听（这是一种KOV的监听模式）
            //[avplayerItem addObserver:self forKeyPath:@"status" options:NSKeyValueObservingOptionNew context:nil];
            avplayer = [AVPlayer playerWithPlayerItem:avplayerItem];
        }

        //指定显示的Layer
        AVPlayerLayer *layer = [AVPlayerLayer playerLayerWithPlayer:avplayer];
        layer.videoGravity = AVLayerVideoGravityResizeAspect;
        layer.frame = CGRectMake(x, y, w, h-40);
        [videoPanel.layer addSublayer:layer];

        videoPanel.frame = CGRectMake(x, y, w, h);

        //设置播放暂停按钮
        NSArray *titles = @[@"CLOSE",@"PAUSE",@"PLAY"];
        CGFloat gap = 10.f;

        for (int i = 0; i < 3; i++) {

            UIButton *btn = [UIButton buttonWithType:UIButtonTypeCustom];
            [btn setTitle:titles[i] forState:UIControlStateNormal];
            btn.backgroundColor = [UIColor whiteColor];
            btn.tag = 555+i;
            btn.frame = CGRectMake(w/4*(i+1)-30, h-100,  60, 40);
            btn.titleLabel.textAlignment = NSTextAlignmentCenter;
            btn.titleLabel.font = [UIFont systemFontOfSize:16.0f];
            [btn addTarget:vc action:@selector(targetAction:) forControlEvents:UIControlEventTouchUpInside];
            [btn setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
            [videoPanel addSubview:btn];
        }
        [vc.view addSubview: videoPanel];
        [avplayer play];

        [videoPanel.layer setValue:avplayer forKey:@"AVPLAYER"];
        return (__bridge void *)videoPanel;
    }
    return NULL;
}

void startVideo(GLFMDisplay *display, void *panel){
    UIView *videoPanel = (__bridge UIView *)panel;
    GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
    if([videoPanel isDescendantOfView: vc.view]){
        AVPlayer *avplayer = [videoPanel.layer.superlayer valueForKey:@"AVPLAYER"];
        if (avplayer.rate == 0) {
            [avplayer play];
        }
    }
}

void pauseVideo(GLFMDisplay *display, void *panel){
    UIView *videoPanel = (__bridge UIView *)panel;
    GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
    if([videoPanel isDescendantOfView: vc.view]){
        AVPlayer *avplayer = [videoPanel.layer.superlayer valueForKey:@"AVPLAYER"];
        if (avplayer.rate == 0) {
            [avplayer pause];
        }
    }
}

void stopVideo(GLFMDisplay *display, void *panel){
    UIView *videoPanel = (__bridge UIView *)panel;
    GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
    if([videoPanel isDescendantOfView: vc.view]){
        AVPlayer *avplayer = [videoPanel.layer.superlayer valueForKey:@"AVPLAYER"];
        if (avplayer.rate == 0) {
            [avplayer pause];
        }

        [videoPanel removeFromSuperview];
        [[AVPlayerLayer playerLayerWithPlayer:avplayer] removeFromSuperlayer];
        avplayer = NULL;
    }
}

int openOtherApp(const char *curl, const char *more, int detectAppInstalled){
    NSString *nspath = [[NSString alloc] initWithCString:curl encoding:NSUTF8StringEncoding];
    
    NSURL *url = [NSURL URLWithString:nspath];
    if ([[UIApplication sharedApplication] canOpenURL:url]||!detectAppInstalled) {
        //NSLog(@"%@ 有效" ,nspath);
        [[UIApplication sharedApplication] openURL:url];
        return 0;
    }else {
        //NSLog(@"%@ 无效" ,nspath);
        return 1;
    }
}

void remoteMethodCall(const char *inJsonStr, Utf8String *outJsonStr){
    
}


#pragma mark - IAP interface
//传入的cproductID和base64HandleScript是const char*类型的，在本函数执行结束后，会被jvm释放 
//所以需要在函数内部复制一份为NSString，避免被jvm释放
void buyAppleProductById(GLFMDisplay * display, const char *cproductID, const char *base64HandleScript) {
    // 复制 cproductID
    NSString *productID = [[NSString alloc] initWithCString:cproductID encoding:NSUTF8StringEncoding];
    // 复制 base64HandleScript
    NSString *script = [[NSString alloc] initWithCString:base64HandleScript encoding:NSUTF8StringEncoding];
 
    [[IAPManager shareIAPManager] startPurchaseWithID:productID completeHandle:^(IAPPurchType type,NSData *data) {
        switch (type) {
            case IAPPurchSuccess:
                NSLog(@"IAPPurchSuccess");
                break;
            case IAPPurchFailed:
                NSLog(@"IAPPurchFailed");
                break;
            case IAPPurchCancel:
                NSLog(@"IAPPurchCancel");
                break;
            case IAPPurchVerFailed:
                NSLog(@"IAPPurchVerFailed");
                break;
            case IAPPurchVerSuccess:
                NSLog(@"IAPPurchVerSuccess");
                break;
            case IAPPurchNotArrow:
                NSLog(@"IAPPurchNotArrow");
                break;
            default:
                break;
        }
        const char *replyMsg = NULL;
        NSString *nssd;
        if (data == nil) {
            nssd = @"";
        } else {
            nssd = [data base64EncodedStringWithOptions:0];
        }

        NSString *str = [NSString stringWithFormat:@"%d:%@:%@", (int)type, nssd, script];

        replyMsg = [str UTF8String];
        
        static char *key = "glfm.ios.purchase";
        
        if(display->notifyFunc){
            display->notifyFunc(display, key, replyMsg);
        }

    }];
}

#endif
