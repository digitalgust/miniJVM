/*
 GLFM
 https://github.com/brackeen/glfm
 Copyright (c) 2014-2019 David Brackeen
 
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

void setDeviceToken(GLFMDisplay * display, const char *deviceToken);

#if defined(GLFM_PLATFORM_IOS) || defined(GLFM_PLATFORM_TVOS)

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import <AssetsLibrary/AssetsLibrary.h>
#import <AVFoundation/AVFoundation.h>


#ifdef NSFoundationVersionNumber_iOS_9_x_Max
#import <UserNotifications/UserNotifications.h>
#endif


#include <dlfcn.h>
#include "glfm_platform.h"

#import <notify.h>
#define NotificationLock CFSTR("com.apple.springboard.lockcomplete")
#define NotificationChange CFSTR("com.apple.springboard.lockstate")
#define NotificationPwdUI CFSTR("com.apple.springboard.hasBlankedScreen")

//
#define MAX_SIMULTANEOUS_TOUCHES 10

void CHECK_GL_ERROR() {do { GLenum error = glGetError();
    if (error != GL_NO_ERROR)
{
    NSLog(@"OpenGL error 0x%04x at glfm_platform_ios.m:%i", error, __LINE__);
    
}} while(0);}

#if __has_feature(objc_arc)
#define GLFM_AUTORELEASE(value) value
#else
#define GLFM_AUTORELEASE(value) [value autorelease]
#endif

@interface GLFMAppDelegate : NSObject <UIApplicationDelegate,UNUserNotificationCenterDelegate>

@property(nonatomic, strong) UIWindow *window;
@property(nonatomic, assign) BOOL active;

@end

#pragma mark - GLFMView

@interface GLFMView : UIView {
    GLint _drawableWidth;
    GLint _drawableHeight;
    GLuint _defaultFramebuffer;
    GLuint _colorRenderbuffer;
    GLuint _attachmentRenderbuffer;
    GLuint _msaaFramebuffer;
    GLuint _msaaRenderbuffer;
}

@property(nonatomic, strong) EAGLContext *context;
@property(nonatomic, strong) NSString *colorFormat;
@property(nonatomic, assign) BOOL preserveBackbuffer;
@property(nonatomic, assign) NSUInteger depthBits;
@property(nonatomic, assign) NSUInteger stencilBits;
@property(nonatomic, assign) BOOL multisampling;
@property(nonatomic, readonly) NSUInteger drawableWidth;
@property(nonatomic, readonly) NSUInteger drawableHeight;

- (void)createDrawable;
- (void)deleteDrawable;
- (void)prepareRender;
- (void)finishRender;

@end

@implementation GLFMView

@dynamic drawableWidth, drawableHeight;

+ (Class)layerClass {
    return [CAEAGLLayer class];
}

- (void)dealloc {
    [self deleteDrawable];
#if !__has_feature(objc_arc)
    self.context = nil;
    self.colorFormat = nil;
    [super dealloc];
#endif
}

- (NSUInteger)drawableWidth {
    return (NSUInteger)_drawableWidth;
}

- (NSUInteger)drawableHeight {
    return (NSUInteger)_drawableHeight;
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

- (void)layoutSubviews {
    CGSize size = self.preferredDrawableSize;
    NSUInteger newDrawableWidth = (NSUInteger)size.width;
    NSUInteger newDrawableHeight = (NSUInteger)size.height;

    if (self.drawableWidth != newDrawableWidth || self.drawableHeight != newDrawableHeight) {
        [self deleteDrawable];
        [self createDrawable];
    }
}

- (CGSize)preferredDrawableSize {
    NSUInteger newDrawableWidth = (NSUInteger)(self.bounds.size.width * self.contentScaleFactor);
    NSUInteger newDrawableHeight = (NSUInteger)(self.bounds.size.height * self.contentScaleFactor);

    // On the iPhone 6 when "Display Zoom" is set, the size will be incorrect.
    if (self.contentScaleFactor == 2.343750) {
        if (newDrawableWidth == 750 && newDrawableHeight == 1331) {
            newDrawableHeight = 1334;
        } else if (newDrawableWidth == 1331 && newDrawableHeight == 750) {
            newDrawableWidth = 1334;
        }
    }

    return CGSizeMake(newDrawableWidth, newDrawableHeight);
}

@end

#pragma mark - GLFMViewController

@interface GLFMViewController : UIViewController<UIKeyInput, UITextInput, UITextInputTraits, UIImagePickerControllerDelegate,UINavigationControllerDelegate> {
    const void *activeTouches[MAX_SIMULTANEOUS_TOUCHES];
}

@property(nonatomic, strong) EAGLContext *context;
@property(nonatomic, strong) CADisplayLink *displayLink;
@property(nonatomic, assign) GLFMDisplay *glfmDisplay;
@property(nonatomic, assign) CGSize drawableSize;
@property(nonatomic, assign) BOOL multipleTouchEnabled;
@property(nonatomic, assign) BOOL keyboardRequested;
@property(nonatomic, assign) BOOL keyboardVisible;
@property(nonatomic, assign) BOOL surfaceCreatedNotified;
@property(nonatomic, assign) int pickerUid;
@property(nonatomic, assign) int pickerType;
@property (nonatomic, retain) NSString *textStore;
@property (nonatomic, retain) UILabel *inputlabel;
//@property (nonatomic, retain) UIView *videoPanel;
//@property (nonatomic, retain) AVPlayer *avplayer;

- (void)takePhotoAction:(int) puid : (int) type;
- (void)browseAlbum:(int) puid : (int) type;
+ (UIImage *)cropImage:(UIImage *)image inRect:(CGRect)rect;
+ (UIImage *)resizeCropImage:(UIImage *)image toRect:(CGSize)size;
+ (UIImage *)resizeImage:(UIImage *)image toSize:(CGSize)reSize;
@end

@implementation GLFMViewController

- (id)init {
    if ((self = [super init])) {
        [self clearTouches];
        _glfmDisplay = calloc(1, sizeof(GLFMDisplay));
        _glfmDisplay->platformData = (__bridge void *)self;
    }
    return self;
}

- (BOOL)animating {
    return (self.displayLink != nil);
}

- (void)setAnimating:(BOOL)animating {
    if (self.animating != animating) {
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

- (BOOL)prefersStatusBarHidden {
    return _glfmDisplay->uiChrome != GLFMUserInterfaceChromeNavigationAndStatusBar;
}

- (BOOL)prefersHomeIndicatorAutoHidden {
    return _glfmDisplay->uiChrome == GLFMUserInterfaceChromeFullscreen;
}

- (void)loadView {
    GLFMAppDelegate *delegate = UIApplication.sharedApplication.delegate;
    self.view = GLFM_AUTORELEASE([[GLFMView alloc] initWithFrame:delegate.window.bounds]);
    self.view.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    self.view.contentScaleFactor = [UIScreen mainScreen].nativeScale;
    self.inputlabel = [[UILabel alloc]initWithFrame:CGRectMake(0, 0, self.view.bounds.size.width, 30)];
    //self.inputlabel.backgroundColor = [UIColor lightGrayColor ];//设置背景颜色
    self.inputlabel.textColor = [UIColor lightGrayColor];//设置Label上文字的颜色
}

- (void)viewDidLoad {
    [super viewDidLoad];

    GLFMView *view = (GLFMView *)self.view;
    self.drawableSize = [view preferredDrawableSize];

    glfmMain(_glfmDisplay);

    if (_glfmDisplay->preferredAPI >= GLFMRenderingAPIOpenGLES3) {
        self.context = GLFM_AUTORELEASE([[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES3]);
    }
    if (!self.context) {
        self.context = GLFM_AUTORELEASE([[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2]);
    }

    if (!self.context) {
        _glfmReportSurfaceError(_glfmDisplay, "Failed to create ES context");
        return;
    }

    view.context = self.context;
    
    [self.view addSubview:_inputlabel];

#if TARGET_OS_IOS
    view.multipleTouchEnabled = self.multipleTouchEnabled;

    [self setNeedsStatusBarAppearanceUpdate];
#endif

    switch (_glfmDisplay->colorFormat) {
        case GLFMColorFormatRGB565:
            view.colorFormat = kEAGLColorFormatRGB565;
            break;
        case GLFMColorFormatRGBA8888:
        default:
            view.colorFormat = kEAGLColorFormatRGBA8;
            break;
    }

    switch (_glfmDisplay->depthFormat) {
        case GLFMDepthFormatNone:
        default:
            view.depthBits = 0;
            break;
        case GLFMDepthFormat16:
            view.depthBits = 16;
            break;
        case GLFMDepthFormat24:
            view.depthBits = 24;
            break;
    }

    switch (_glfmDisplay->stencilFormat) {
        case GLFMStencilFormatNone:
        default:
            view.stencilBits = 0;
            break;
        case GLFMStencilFormat8:
            view.stencilBits = 8;
            break;
    }

    view.multisampling = _glfmDisplay->multisample != GLFMMultisampleNone;

    [view createDrawable];

    if (view.drawableWidth > 0 && view.drawableHeight > 0) {
        self.drawableSize = CGSizeMake(view.drawableWidth, view.drawableHeight);
        self.animating = YES;
    }

#if TARGET_OS_IOS
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(keyboardFrameChanged:)
                                               name:UIKeyboardWillChangeFrameNotification
                                             object:view.window];
#endif
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
    BOOL isTablet = (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad);
    GLFMUserInterfaceOrientation uiOrientations = _glfmDisplay->allowedOrientations;
    if (uiOrientations == GLFMUserInterfaceOrientationAny) {
        if (isTablet) {
            return UIInterfaceOrientationMaskAll;
        } else {
            return UIInterfaceOrientationMaskAllButUpsideDown;
        }
    } else if (uiOrientations == GLFMUserInterfaceOrientationPortrait) {
        if (isTablet) {
            return (UIInterfaceOrientationMask)(UIInterfaceOrientationMaskPortrait |
                    UIInterfaceOrientationMaskPortraitUpsideDown);
        } else {
            return UIInterfaceOrientationMaskPortrait;
        }
    } else {
        return UIInterfaceOrientationMaskLandscape;
    }
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    if (_glfmDisplay->lowMemoryFunc) {
        _glfmDisplay->lowMemoryFunc(_glfmDisplay);
    }
}

- (void)dealloc {
    [self setAnimating:NO];
    if ([EAGLContext currentContext] == self.context) {
        [EAGLContext setCurrentContext:nil];
    }
    if (_glfmDisplay->surfaceDestroyedFunc) {
        _glfmDisplay->surfaceDestroyedFunc(_glfmDisplay);
    }
    free(_glfmDisplay);
#if !__has_feature(objc_arc)
    self.context = nil;
    [super dealloc];
#endif
}

- (void)render:(CADisplayLink *)displayLink {
    GLFMView *view = (GLFMView *)self.view;

    if (!self.surfaceCreatedNotified) {
        self.surfaceCreatedNotified = YES;

        if (_glfmDisplay->surfaceCreatedFunc) {
            _glfmDisplay->surfaceCreatedFunc(_glfmDisplay, (int)self.drawableSize.width,
                                             (int)self.drawableSize.height);
        }
    }

    CGSize newDrawableSize = CGSizeMake(view.drawableWidth, view.drawableHeight);
    if (!CGSizeEqualToSize(self.drawableSize, newDrawableSize)) {
        self.drawableSize = newDrawableSize;
        if (_glfmDisplay->surfaceResizedFunc) {
            _glfmDisplay->surfaceResizedFunc(_glfmDisplay, (int)self.drawableSize.width,
                                             (int)self.drawableSize.height);
        }
    }

    [view prepareRender];
    if (_glfmDisplay->mainLoopFunc) {
        _glfmDisplay->mainLoopFunc(_glfmDisplay, displayLink.timestamp);
    }
    [view finishRender];
}

#pragma mark - Action Methods
- (void)targetAction:(UIButton*)sender
{
    switch (sender.tag) {
        case 555: { //close
            UIView *videoPanel = sender.superview;
            AVPlayer *avplayer = [videoPanel.layer valueForKey:@"AVPLAYER"];
            [avplayer pause];
                    [videoPanel removeFromSuperview];
            [[AVPlayerLayer playerLayerWithPlayer:avplayer] removeFromSuperlayer];
            avplayer = NULL;
            break;
        }

        case 557: { //播放
            UIView *videoPanel = sender.superview;
            AVPlayer *avplayer = [videoPanel.layer valueForKey:@"AVPLAYER"];
            [avplayer play];
            break;
        }
        case 556: { //暂停
            UIView *videoPanel = sender.superview;
            AVPlayer *avplayer = [videoPanel.layer valueForKey:@"AVPLAYER"];
            [avplayer pause];
            break;
        }
        default:
            break;
    }
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
            [self setInputLabelPos:keyboardFrame.origin.x+10 :[nsValue CGRectValue].origin.y-30];
        }
    }
}

-(void)setInputLabelPos:(CGFloat) nx : (CGFloat) ny{
    CGRect tempFrame = self.inputlabel.frame;
    tempFrame.origin.x=nx;
    tempFrame.origin.y=ny;
    self.inputlabel.frame=tempFrame;
}
#endif

// UITextInputTraits - disable suggestion bar
- (UITextAutocorrectionType)autocorrectionType {
    return UITextAutocorrectionTypeNo;
}

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

- (BOOL)canBecomeFirstResponder {
    return self.keyboardRequested;
}

- (NSArray<UIKeyCommand *> *)keyCommands {
    static NSArray<UIKeyCommand *> *keyCommands = NULL;
    if (!keyCommands) {
        keyCommands = @[ [UIKeyCommand keyCommandWithInput:UIKeyInputUpArrow
                                             modifierFlags:(UIKeyModifierFlags)0
                                                    action:@selector(keyPressed:)],
                         [UIKeyCommand keyCommandWithInput:UIKeyInputDownArrow
                                             modifierFlags:(UIKeyModifierFlags)0
                                                    action:@selector(keyPressed:)],
                         [UIKeyCommand keyCommandWithInput:UIKeyInputLeftArrow
                                             modifierFlags:(UIKeyModifierFlags)0
                                                    action:@selector(keyPressed:)],
                         [UIKeyCommand keyCommandWithInput:UIKeyInputRightArrow
                                             modifierFlags:(UIKeyModifierFlags)0
                                                    action:@selector(keyPressed:)],
                         [UIKeyCommand keyCommandWithInput:UIKeyInputEscape
                                             modifierFlags:(UIKeyModifierFlags)0
                                                    action:@selector(keyPressed:)] ];
#if !__has_feature(objc_arc)
        [keyCommands retain];
#endif
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

- (UITextWritingDirection)baseWritingDirectionForPosition:(nonnull UITextPosition *)position inDirection:(UITextStorageDirection)direction {
    return UITextWritingDirectionLeftToRight;
}

- (CGRect)caretRectForPosition:(nonnull UITextPosition *)position {
    return CGRectMake(0, 0, 10, 30);
}

- (nullable UITextRange *)characterRangeAtPoint:(CGPoint)point {
    return nil;
}

- (nullable UITextRange *)characterRangeByExtendingPosition:(nonnull UITextPosition *)position inDirection:(UITextLayoutDirection)direction {
    return nil;
}

- (nullable UITextPosition *)closestPositionToPoint:(CGPoint)point {
    return nil;
}

- (nullable UITextPosition *)closestPositionToPoint:(CGPoint)point withinRange:(nonnull UITextRange *)range {
    return nil;
}

- (NSComparisonResult)comparePosition:(nonnull UITextPosition *)position toPosition:(nonnull UITextPosition *)other {
    
    return NSOrderedSame;
}

- (CGRect)firstRectForRange:(nonnull UITextRange *)range {
    return CGRectMake(0, 0, 1, 1);
}

- (NSInteger)offsetFromPosition:(nonnull UITextPosition *)from toPosition:(nonnull UITextPosition *)toPosition {
    return NSIntegerMax;
}

- (nullable UITextPosition *)positionFromPosition:(nonnull UITextPosition *)position inDirection:(UITextLayoutDirection)direction offset:(NSInteger)offset {
    return nil;
}

- (nullable UITextPosition *)positionFromPosition:(nonnull UITextPosition *)position offset:(NSInteger)offset {
    return nil;
}

- (nullable UITextPosition *)positionWithinRange:(nonnull UITextRange *)range farthestInDirection:(UITextLayoutDirection)direction {
    return nil;
}

- (void)replaceRange:(nonnull UITextRange *)range withText:(nonnull NSString *)text {
    //NSLog(@"replaceRange %@",text);
}

- (nonnull NSArray<UITextSelectionRect *> *)selectionRectsForRange:(nonnull UITextRange *)range {
    NSArray *arr = NULL;
    arr = @[];
    return arr;
}

- (void)setBaseWritingDirection:(UITextWritingDirection)writingDirection forRange:(nonnull UITextRange *)range {
    //self.textStore = markedText;
}

- (void)setMarkedText:(nullable NSString *)markedText selectedRange:(NSRange)selectedRange {
    //NSLog(@"setMarkedText %@",markedText);
    self.textStore=markedText;
    self.inputlabel.text=markedText;
}

- (nullable NSString *)textInRange:(nonnull UITextRange *)range {
    //NSLog(@"textInRange ");
    return nil;
}

- (nullable UITextRange *)textRangeFromPosition:(nonnull UITextPosition *)fromPosition toPosition:(nonnull UITextPosition *)toPosition {
    return nil;
}


- (void)unmarkText {
    if (!self.textStore) return;
    [self insertText:self.textStore];
    self.textStore = nil;
    self.inputlabel.text = nil;
    //NSLog(@"unmarkText ");
}

- (void)encodeWithCoder:(nonnull NSCoder *)aCoder {
    
}

+ (nonnull instancetype)appearance {
    return nil;
}

+ (nonnull instancetype)appearanceForTraitCollection:(nonnull UITraitCollection *)trait {
    return nil;
}

+ (nonnull instancetype)appearanceForTraitCollection:(nonnull UITraitCollection *)trait whenContainedIn:(nullable Class<UIAppearanceContainer>)ContainerClass, ... {
    return nil;
}

+ (nonnull instancetype)appearanceForTraitCollection:(nonnull UITraitCollection *)trait whenContainedInInstancesOfClasses:(nonnull NSArray<Class<UIAppearanceContainer>> *)containerTypes {
    return nil;
}

+ (nonnull instancetype)appearanceWhenContainedIn:(nullable Class<UIAppearanceContainer>)ContainerClass, ... {
    return nil;
}

+ (nonnull instancetype)appearanceWhenContainedInInstancesOfClasses:(nonnull NSArray<Class<UIAppearanceContainer>> *)containerTypes {
    return nil;
}

- (void)traitCollectionDidChange:(nullable UITraitCollection *)previousTraitCollection {
    
}

- (CGPoint)convertPoint:(CGPoint)point fromCoordinateSpace:(nonnull id<UICoordinateSpace>)coordinateSpace {
    return CGPointMake(0, 0);
}

- (CGPoint)convertPoint:(CGPoint)point toCoordinateSpace:(nonnull id<UICoordinateSpace>)coordinateSpace {
    return CGPointMake(0, 0);
}

- (CGRect)convertRect:(CGRect)rect fromCoordinateSpace:(nonnull id<UICoordinateSpace>)coordinateSpace {
    return CGRectMake(0, 0,1,1);
}

- (CGRect)convertRect:(CGRect)rect toCoordinateSpace:(nonnull id<UICoordinateSpace>)coordinateSpace {
    return CGRectMake(0, 0,1,1);
}

- (void)didUpdateFocusInContext:(nonnull UIFocusUpdateContext *)context withAnimationCoordinator:(nonnull UIFocusAnimationCoordinator *)coordinator {
    
}

- (void)setNeedsFocusUpdate {
    
}

- (BOOL)shouldUpdateFocusInContext:(nonnull UIFocusUpdateContext *)context {
    return YES;
}

- (void)updateFocusIfNeeded {
    
}

- (nonnull NSArray<id<UIFocusItem>> *)focusItemsInRect:(CGRect)rect {
    NSArray *arr = NULL;
    arr = @[];
    return arr;
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
        if(_pickerType==0){//autofit
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
        
        NSURL *nsurl=[info objectForKey:UIImagePickerControllerReferenceURL];
        NSString *nss=[nsurl path];
        const char* url=[nss UTF8String];
        
        char *cd=(char*)[data bytes];
        int len=(int)[data length];
        _glfmDisplay->pickerFunc(_glfmDisplay, _pickerUid, url, cd, len);

        //通过判断picker的sourceType，如果是拍照则保存到相册去
        if (picker.sourceType == UIImagePickerControllerSourceTypeCamera) {
            UIImageWriteToSavedPhotosAlbum(image, self, @selector(image:didFinishSavingWithError:contextInfo:), nil);
        }
    } else if([mediaType isEqualToString:@"public.movie"]){
        NSURL *videoURL = [info objectForKey:UIImagePickerControllerMediaURL];
        NSString *path = [[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true) lastObject] stringByAppendingPathComponent:[NSString stringWithFormat:@"%ld_compressedVideo.mp4",time(NULL)]];
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
    CFNotificationCenterAddObserver(CFNotificationCenterGetDarwinNotifyCenter(), NULL, screenLockStateChanged, NotificationLock, NULL, CFNotificationSuspensionBehaviorDeliverImmediately);
    CFNotificationCenterAddObserver(CFNotificationCenterGetDarwinNotifyCenter(), NULL, screenLockStateChanged, NotificationChange, NULL, CFNotificationSuspensionBehaviorDeliverImmediately);


    //setScreenStateCb();
    
    return YES;
}
//lock screen
static void screenLockStateChanged(CFNotificationCenterRef center,void* observer,CFStringRef name,const void*object,CFDictionaryRef userInfo)
{
    NSString* lockstate = (__bridge NSString*)name;
    if ([lockstate isEqualToString:(__bridge NSString*)NotificationLock]) {
        NSLog(@"locked.");
    } else {
        NSLog(@"lock state changed.");
    }
}

// 获得Device Token
- (void)application:(UIApplication *)application
didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    GLFMViewController *vc = (GLFMViewController *)[self.window rootViewController];
    //NSLog(@"%@", [NSString stringWithFormat:@"Device Token: %@", deviceToken]);
    NSString * newStr = [[[[deviceToken description]
         stringByReplacingOccurrencesOfString:@"<" withString:@""]
         stringByReplacingOccurrencesOfString:@">" withString:@""]
         stringByReplacingOccurrencesOfString:@"" withString:@""];
    //NSString* newStr = [[NSString alloc] initWithData:deviceToken encoding:NSUTF8StringEncoding];
    if(newStr != nil){
        const char * token=[newStr UTF8String];
        setDeviceToken(vc.glfmDisplay,token);
    }else{
        setDeviceToken(vc.glfmDisplay,NULL);
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

- (void)setActive:(BOOL)active {
    if (_active != active) {
        _active = active;

        GLFMViewController *vc = (GLFMViewController *)[self.window rootViewController];
        [vc clearTouches];
        vc.animating = active;
        if (vc.glfmDisplay && vc.glfmDisplay->focusFunc) {
            vc.glfmDisplay->focusFunc(vc.glfmDisplay, _active);
        }
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

GLFMProc glfmGetProcAddress(const char *functionName) {
    static void *handle = NULL;
    if (!handle) {
        handle = dlopen(NULL, RTLD_LAZY);
    }
    return handle ? (GLFMProc)dlsym(handle, functionName) : NULL;
}

void glfmSetUserInterfaceOrientation(GLFMDisplay *display,
                                     GLFMUserInterfaceOrientation allowedOrientations) {
    if (display) {
        if (display->allowedOrientations != allowedOrientations) {
            display->allowedOrientations = allowedOrientations;

            // HACK: Notify that the value of supportedInterfaceOrientations has changed
            GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
            if (vc.view.window) {
                UIViewController *dummyVC = GLFM_AUTORELEASE([[UIViewController alloc] init]);
                dummyVC.view = GLFM_AUTORELEASE([[UIView alloc] init]);
                [vc presentViewController:dummyVC animated:NO completion:^{
                    [vc dismissViewControllerAnimated:NO completion:NULL];
                }];
            }
        }
    }
}

void glfmGetDisplaySize(GLFMDisplay *display, int *width, int *height) {
    if (display && display->platformData) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        *width = (int)vc.drawableSize.width;
        *height = (int)vc.drawableSize.height;
    } else {
        *width = 0;
        *height = 0;
    }
}

double glfmGetDisplayScale(GLFMDisplay *display) {
    if (display && display->platformData) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        return vc.view.contentScaleFactor;
    } else {
        return [UIScreen mainScreen].scale;
    }
}

void glfmGetDisplayChromeInsets(GLFMDisplay *display, double *top, double *right, double *bottom,
                                double *left) {
    if (display && display->platformData) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        if (@available(iOS 11, tvOS 11, *)) {
            UIEdgeInsets insets = vc.view.safeAreaInsets;
            *top = insets.top * vc.view.contentScaleFactor;
            *right = insets.right * vc.view.contentScaleFactor;
            *bottom = insets.bottom * vc.view.contentScaleFactor;
            *left = insets.left * vc.view.contentScaleFactor;
        } else {
#if TARGET_OS_IOS
            if (![vc prefersStatusBarHidden]) {
                *top = ([UIApplication sharedApplication].statusBarFrame.size.height *
                        vc.view.contentScaleFactor);
            } else {
                *top = 0.0;
            }
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

void _glfmDisplayChromeUpdated(GLFMDisplay *display) {
    if (display && display->platformData) {
#if TARGET_OS_IOS
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        [vc setNeedsStatusBarAppearanceUpdate];
        if (@available(iOS 11, *)) {
            [vc setNeedsUpdateOfHomeIndicatorAutoHidden];
        }
#endif
    }
}

GLFMRenderingAPI glfmGetRenderingAPI(GLFMDisplay *display) {
    if (display && display->platformData) {
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        if (vc.context.API == kEAGLRenderingAPIOpenGLES3) {
            return GLFMRenderingAPIOpenGLES3;
        } else {
            return GLFMRenderingAPIOpenGLES2;
        }
    } else {
        return GLFMRenderingAPIOpenGLES2;
    }
}

bool glfmHasTouch(GLFMDisplay *display) {
    return true;
}

void glfmSetMouseCursor(GLFMDisplay *display, GLFMMouseCursor mouseCursor) {
    // Do nothing
}

void glfmSetMultitouchEnabled(GLFMDisplay *display, bool multitouchEnabled) {
    if (display) {
#if TARGET_OS_IOS
        GLFMViewController *vc = (__bridge GLFMViewController *)display->platformData;
        vc.multipleTouchEnabled = (BOOL)multitouchEnabled;
        if (vc.isViewLoaded) {
            vc.view.multipleTouchEnabled = (BOOL)multitouchEnabled;
        }
#endif
    }
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

const char* glfmGetResRoot(){
    
    NSString *resPath= [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:@""];
    //printf("respath: %s\n\n",[resPath UTF8String]);
    
    return [resPath UTF8String];
}

const char* glfmGetSaveRoot(){
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    NSString *cachesDir = [paths objectAtIndex:0];
    
    return [cachesDir UTF8String];
}

const char* glfmGetUUID(){
    NSString* Identifier = [[[UIDevice currentDevice] identifierForVendor] UUIDString];
    return [Identifier UTF8String];
}

void setDeviceToken(GLFMDisplay * display, const char *deviceToken) {
    static char *key="glfm.device.token";

    if(display->notifyFunc){
        display->notifyFunc(display,key,deviceToken);
    }
}


const char* getClipBoardContent() {
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

// 获取视频第一帧
//void getVideoPreViewImage(const char *videopath, Utf8String *imagepath)
//{
//    NSURL *path;
//    AVURLAsset *asset = [[AVURLAsset alloc] initWithURL:path options:nil];
//    AVAssetImageGenerator *assetGen = [[AVAssetImageGenerator alloc] initWithAsset:asset];
//
//    assetGen.appliesPreferredTrackTransform = YES;
//    CMTime time = CMTimeMakeWithSeconds(0.0, 600);
//    NSError *error = nil;
//    CMTime actualTime;
//    CGImageRef image = [assetGen copyCGImageAtTime:time actualTime:&actualTime error:&error];
//    UIImage *videoImage = [[UIImage alloc] initWithCGImage:image];
//    CGImageRelease(image);
//    return videoImage;
//}

#endif
