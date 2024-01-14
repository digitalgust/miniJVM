/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lwjgl.opengl;

/**
 * This is the abstract class for a Display in LWJGL. LWJGL displays have some
 * peculiar characteristics:
 *
 * - the display may be closeable by the user or operating system, and may be minimized
 * by the user or operating system
 * - only one display may ever be open at once
 * - the operating system may or may not be able to do fullscreen or windowed displays.
 *
 * @author foo
 */



import java.nio.ByteBuffer;
import java.nio.FloatBuffer;


public final class Display {

	/** The initial display mode */
	private static DisplayMode initial_mode;


	/** The current display mode, if created */
	private static DisplayMode current_mode;

	/** X coordinate of the window */
	private static int x = -1;

	/** Cached window icons, for when Display is recreated */
	private static ByteBuffer[] cached_icons;

	/**
	 * Y coordinate of the window. Y in window coordinates is from the top of the display down,
	 * unlike GL, where it is typically at the bottom of the display.
	 */
	private static int y = -1;

	/** the width of the Display window */
	private static int width = 0;

	/** the height of the Display window */
	private static int height = 0;

	/** Title of the window (never null) */
	private static String title = "Game";

	/** Fullscreen */
	private static boolean fullscreen;

	/** Swap interval */
	private static int swap_interval;

	private static boolean window_created;

	private static boolean parent_resized;

	private static boolean window_resized;

	private static boolean window_resizable;

	/** Initial Background Color of Display */
	private static float r, g, b;

	/**
	 * Fetch the Drawable from the Display.
	 *
	 * @return the Drawable corresponding to the Display context
	 */

	/** Only constructed by ourselves */
	private Display() {
        initial_mode = new DisplayMode(640, 480);
	}

	/**
	 * Returns the entire list of possible fullscreen display modes as an array, in no
	 * particular order. Although best attempts to filter out invalid modes are done, any
	 * given mode is not guaranteed to be available nor is it guaranteed to be within the
	 * current monitor specs (this is especially a problem with the frequency parameter).
	 * Furthermore, it is not guaranteed that create() will detect an illegal display mode.
	 * <p/>
	 * The only certain way to check
	 * is to call create() and make sure it works.
	 * Only non-palette-indexed modes are returned (ie. bpp will be 16, 24, or 32).
	 * Only DisplayModes from this call can be used when the Display is in fullscreen
	 * mode.
	 *
	 * @return an array of all display modes the system reckons it can handle.
	 */

	/**
	 * Return the initial desktop display mode.
	 *
	 * @return The desktop display mode
	 */
	public static DisplayMode getDesktopDisplayMode() {
		return initial_mode;
	}

	/**
	 * Return the current display mode, as set by setDisplayMode().
	 *
	 * @return The current display mode
	 */
	public static DisplayMode getDisplayMode() {
		return current_mode;
	}

	/**
	 * Set the current display mode. If no OpenGL context has been created, the given mode will apply to
	 * the context when create() is called, and no immediate mode switching will happen. If there is a
	 * context already, it will be resized according to the given mode. If the context is also a
	 * fullscreen context, the mode will also be switched immediately. The native cursor position
	 * is also reset.
	 *
	 * @param mode The new display mode to set
	 *
	 * @throws LWJGLException if the display mode could not be set
	 */
	public static void setDisplayMode(DisplayMode mode) {
        current_mode = mode;
	}

	/**
	 * Swap the display buffers. This method is called from update(), and should normally not be called by
	 * the application.
	 *
	 * @throws OpenGLException if an OpenGL error has occured since the last call to glGetError()
	 */
	// public static void swapBuffers() throws LWJGLException {
	// 	synchronized ( GlobalLock.lock ) {
	// 		if ( !isCreated() )
	// 			throw new IllegalStateException("Display not created");
 // 
	// 		if ( LWJGLUtil.DEBUG )
	// 			drawable.checkGLError();
	// 		drawable.swapBuffers();
	// 	}
	// }

	/**
	 * Update the window. If the window is visible clears
	 * the dirty flag and calls swapBuffers() and finally
	 * polls the input devices.
	 */
	public static native void update();

	/**
	 * Update the window. If the window is visible clears
	 * the dirty flag and calls swapBuffers() and finally
	 * polls the input devices if processMessages is true.
	 *
	 * @param processMessages Poll input devices if true
	 */
	// public static native void update(boolean processMessages);


	/**
	 * Create the OpenGL context. If isFullscreen() is true or if windowed
	 * context are not supported on the platform, the display mode will be switched to the mode returned by
	 * getDisplayMode(), and a fullscreen context will be created. If isFullscreen() is false, a windowed context
	 * will be created with the dimensions given in the mode returned by getDisplayMode(). If a context can't be
	 * created with the given parameters, a LWJGLException will be thrown.
	 * <p/>
	 * <p>The window created will be set up in orthographic 2D projection, with 1:1 pixel ratio with GL coordinates.
	 *
	 * @throws LWJGLException
	 */
	public static native void create();


	/**
	 * Set the initial color of the Display. This method is called before the Display is created and will set the
	 * background color to the one specified in this method.
	 *
	 * @param red   - color value between 0 - 1
	 * @param green - color value between 0 - 1
	 * @param blue  - color value between 0 - 1
	 */
	public static void setInitialBackground(float red, float green, float blue) {
		r = red;
		g = green;
		b = blue;
	}

	private static void initContext() {
	}



	private static void initControls() {

	}

	/**
	 * Destroy the Display. After this call, there will be no current GL rendering context,
	 * regardless of whether the Display was the current rendering context.
	 */
	public static void destroy() {
	}

	/*
	 * Reset display mode if fullscreen. This method is also called from the shutdown hook added
	 * in the static constructor
	 */

	private static void reset() {
		current_mode = initial_mode;
	}

	/** @return true if the window's native peer has been created */
	public static boolean isCreated() {
        return true;
	}

	/**
	 * Set the buffer swap interval. This call is a best-attempt at changing
	 * the monitor swap interval, which is the minimum periodicity of color buffer swaps,
	 * measured in video frame periods, and is not guaranteed to be successful.
	 * <p/>
	 * A video frame period is the time required to display a full frame of video data.
	 *
	 * @param value The swap interval in frames, 0 to disable
	 */
	public static void setSwapInterval(int value) {
	}

	/**
	 * Enable or disable vertical monitor synchronization. This call is a best-attempt at changing
	 * the vertical refresh synchronization of the monitor, and is not guaranteed to be successful.
	 *
	 * @param sync true to synchronize; false to ignore synchronization
	 */
	public static void setVSyncEnabled(boolean sync) {
	}

	/**
	 * Set the window's location. This is a no-op on fullscreen windows or when getParent() != null.
	 * The window is clamped to remain entirely on the screen. If you attempt
	 * to position the window such that it would extend off the screen, the window
	 * is simply placed as close to the edge as possible.
	 * <br><b>note</b>If no location has been specified (or x == y == -1) the window will be centered
	 *
	 * @param new_x The new window location on the x axis
	 * @param new_y The new window location on the y axis
	 */
	public static void setLocation(int new_x, int new_y) {
	}

	private static void reshape() {
	}

	/**
	 * Get the driver adapter string. This is a unique string describing the actual card's hardware, eg. "Geforce2", "PS2",
	 * "Radeon9700". If the adapter cannot be determined, this function returns null.
	 *
	 * @return a String
	 */
	public static String getAdapter() {
		return "";
	}

	/**
	 * Get the driver version. This is a vendor/adapter specific version string. If the version cannot be determined,
	 * this function returns null.
	 *
	 * @return a String
	 */
	public static String getVersion() {
		return "";
	}

	/**
	 * Sets one or more icons for the Display.
	 * <ul>
	 * <li>On Windows you should supply at least one 16x16 icon and one 32x32.</li>
	 * <li>Linux (and similar platforms) expect one 32x32 icon.</li>
	 * <li>Mac OS X should be supplied one 128x128 icon</li>
	 * </ul>
	 * The implementation will use the supplied ByteBuffers with image data in RGBA (size must be a power of two) and perform any conversions nescesarry for the specific platform.
	 * <p/>
	 * <b>NOTE:</b> The display will make a deep copy of the supplied byte buffer array, for the purpose
	 * of recreating the icons when you go back and forth fullscreen mode. You therefore only need to
	 * set the icon once per instance.
	 *
	 * @param icons Array of icons in RGBA mode. Pass the icons in order of preference.
	 *
	 * @return number of icons used, or 0 if display hasn't been created
	 */
	public static int setIcon(ByteBuffer[] icons) {
        return 0;
	}

	/**
	 * Enable or disable the Display window to be resized.
	 *
	 * @param resizable set to true to make the Display window resizable;
	 * false to disable resizing on the Display window.
	 */
	public static void setResizable(boolean resizable) {
	}

	/**
	 * @return true if the Display window is resizable.
	 */
	public static boolean isResizable() {
		return window_resizable;
	}

	/**
	 * @return true if the Display window has been resized.
	 * This value will be updated after a call to Display.update().
	 *
	 * This will return false if running in fullscreen or with Display.setParent(Canvas parent)
	 */
	public static boolean wasResized() {
		return window_resized;
	}


	/**
	 * @return this method will return the width of the Display window.
	 *
	 * If running in fullscreen mode it will return the width of the current set DisplayMode.
	 * If Display.setParent(Canvas parent) is being used, the width of the parent
	 * will be returned.
	 *
	 * This value will be updated after a call to Display.update().
	 */
	public static int getWidth() {

		return width;
	}

	/**
	 * @return this method will return the height of the Display window.
	 *
	 * If running in fullscreen mode it will return the height of the current set DisplayMode.
	 * If Display.setParent(Canvas parent) is being used, the height of the parent
	 * will be returned.
	 *
	 * This value will be updated after a call to Display.update().
	 */
	public static int getHeight() {

		return height;
	}

}
