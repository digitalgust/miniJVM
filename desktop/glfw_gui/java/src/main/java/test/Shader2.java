package test;

import org.mini.gl.GL;
import org.mini.gl.GLMath;
import org.mini.glfw.Glfw;
import org.mini.gui.GToolkit;
import org.mini.glwrap.GLUtil;

import static org.mini.gl.GL.*;
import static org.mini.glfw.Glfw.*;
import static org.mini.nanovg.Nanovg.*;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author gust
 */
public class Shader2 {

    boolean exit = false;
    long curWin;
    int mx, my;

    class CallBack extends GlfwCallbackAdapter {

        @Override
        public void key(long window, int key, int scancode, int action, int mods) {
            System.out.println("key:" + key + " action:" + action);
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, GLFW_TRUE);
            }
        }

        @Override
        public void mouseButton(long window, int button, boolean pressed) {
            if (window == curWin) {
                String bt = button == GLFW_MOUSE_BUTTON_LEFT ? "LEFT" : button == GLFW_MOUSE_BUTTON_2 ? "RIGHT" : "OTHER";
                String press = pressed ? "pressed" : "released";
                System.out.println(bt + " " + mx + " " + my + "  " + press);
            }
        }

        @Override
        public void cursorPos(long window, int x, int y) {
            curWin = window;
            mx = x;
            my = y;
        }

        @Override
        public boolean windowClose(long window) {
            System.out.println("byebye");
            return true;
        }

        @Override
        public void windowSize(long window, int width, int height) {
            System.out.println("resize " + width + " " + height);
        }

        @Override
        public void framebufferSize(long window, int x, int y) {
        }
    }


    String VertexShaderT = "#version 330 core\n" +
            "layout(location = 0) in vec3 position;\n" +
            "//layout(location = 1) in vec3 color;\n" +
            "layout(location = 2) in vec2 texCoord;\n" +
            " \n" +
            "//out vec3 ourColor;\n" +
            "out vec2 TexCoord;\n" +
            " \n" +
            "uniform mat4 model;\n" +
            "uniform mat4 view;\n" +
            "uniform mat4 projection;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "\tgl_Position = projection * view * model * vec4(position, 1.0f);\n" +
            "\t//ourColor = color;\n" +
            "\tTexCoord = vec2(texCoord.x, 1.0 - texCoord.y);\n" +
            "}\n";
    String FragmentShaderT = "#version 330 core\n" +
            "//in vec3 ourColor;\n" +
            "in vec2 TexCoord;\n" +
            " \n" +
            "out vec4 color;\n" +
            " \n" +
            "// Texture samplers\n" +
            "uniform sampler2D ourTexture1;\n" +
            "uniform sampler2D ourTexture2;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "\t// Linearly interpolate between both textures (second texture is only slightly combined)\n" +
            "\tcolor = mix(texture(ourTexture1, TexCoord), texture(ourTexture2, TexCoord), 0.2);\n" +
            "}\n";

    float vertices[] = {
            -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,
            0.5f, -0.5f, -0.5f, 1.0f, 0.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,

            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
            0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
            -0.5f, 0.5f, 0.5f, 0.0f, 1.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,

            -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
            -0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
            -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,

            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
            0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,

            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            0.5f, -0.5f, -0.5f, 1.0f, 1.0f,
            0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
            0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,

            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
            -0.5f, 0.5f, 0.5f, 0.0f, 0.0f,
            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f
    };
    float[][] cubePositions = {
            {0.0f, 0.0f, 0.0f},
            {2.0f, 5.0f, -15.0f},
            {-1.5f, -2.2f, -2.5f},
            {-3.8f, -2.0f, -12.3f},
            {2.4f, -0.4f, -3.5f},
            {-1.7f, 3.0f, -7.5f},
            {1.3f, -2.0f, -2.5f},
            {1.5f, 2.0f, -2.5f},
            {1.5f, 0.2f, -1.5f},
            {-1.3f, 1.0f, -1.5f},
    };

    int loadShader(String vss, String fss) {
        int[] return_val = {0};
        //编译顶点着色器
        int vertexShader;
        vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, 1, new byte[][]{GLUtil.toUtf8(vss)}, null, 0);
        glCompileShader(vertexShader);
        int success;
        GL.glGetShaderiv(vertexShader, GL.GL_COMPILE_STATUS, return_val, 0);
        if (return_val[0] == GL_FALSE) {
            GL.glGetShaderiv(vertexShader, GL.GL_INFO_LOG_LENGTH, return_val, 0);
            byte[] szLog = new byte[return_val[0] + 1];
            GL.glGetShaderInfoLog(vertexShader, szLog.length, return_val, 0, szLog);
            System.out.println("Compile Shader fail error :" + new String(szLog, 0, return_val[0]) + "\n" + vss + "\n");
            return 0;
        }


        int fragmentShader;
        fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, 1, new byte[][]{GLUtil.toUtf8(fss)}, null, 0);
        glCompileShader(fragmentShader);
        GL.glGetShaderiv(fragmentShader, GL.GL_COMPILE_STATUS, return_val, 0);
        if (return_val[0] == GL_FALSE) {
            GL.glGetShaderiv(fragmentShader, GL.GL_INFO_LOG_LENGTH, return_val, 0);
            byte[] szLog = new byte[return_val[0] + 1];
            GL.glGetShaderInfoLog(fragmentShader, szLog.length, return_val, 0, szLog);
            System.out.println("Compile Shader fail error :" + new String(szLog, 0, return_val[0]) + "\n" + fss + "\n");
            return 0;
        }

        //着色器程序
        int shaderProgram;
        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        GL.glGetProgramiv(shaderProgram, GL.GL_LINK_STATUS, return_val, 0);
        if (return_val[0] == GL_FALSE) {
            GL.glGetProgramiv(shaderProgram, GL.GL_INFO_LOG_LENGTH, return_val, 0);
            byte[] szLog = new byte[return_val[0] + 1];
            GL.glGetProgramInfoLog(shaderProgram, szLog.length, return_val, 0, szLog);
            System.out.println("Link Shader fail error :" + new String(szLog, 0, return_val[0]) + "\n vertex shader:" + vertexShader + "\nfragment shader:" + fragmentShader + "\n");
            return 0;
        }
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return shaderProgram;
    }

    int[] texture1 = {0}, texture2 = {0};
    int[] VBO = {0}, VAO = {0};
    int ourShader;

    public void init1() {

        ourShader = loadShader(VertexShaderT, FragmentShaderT);

        glGenVertexArrays(1, VAO, 0);
        glGenBuffers(1, VBO, 0);

        glBindVertexArray(VAO[0]);

        glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
        glBufferData(GL_ARRAY_BUFFER, vertices.length * 4, vertices, 0, GL_STATIC_DRAW);

        int FLOATSIZE = 4;
        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 5 * FLOATSIZE, null, 0);
        glEnableVertexAttribArray(0);
        // TexCoord attribute
        glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 5 * FLOATSIZE, null, (3 * FLOATSIZE));
        glEnableVertexAttribArray(2);

        glBindVertexArray(0); // Unbind VAO


        int[] whd = {0, 0, 0};
        texture1[0] = GLUtil.gl_image_load(GToolkit.readFileFromJar("/res/h_0.png"), whd);
        // ===================
        // Texture 2
        // ===================

        texture2[0] = GLUtil.gl_image_load(GToolkit.readFileFromJar("/res/h_1.png"), whd);

        glEnable(GL_DEPTH_TEST);

    }

    //---------------------------------------------------------------------
    void display1() {

        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Activate shader
        glUseProgram(ourShader);

        // Bind Textures using texture units
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture1[0]);
        glUniform1i(glGetUniformLocation(ourShader, GLUtil.toUtf8("ourTexture1")), 0);
        GLUtil.checkGlError("gl_paint 1.3");
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, texture2[0]);
        glUniform1i(glGetUniformLocation(ourShader, GLUtil.toUtf8("ourTexture2")), 1);
        GLUtil.checkGlError("gl_paint 1.5");


        // Camera/View transformation
        float[] view = new float[16];
        float radius = 10.0f;
        float camX = (float) Math.sin(glfwGetTime()) * radius;
        float camZ = (float) Math.cos(glfwGetTime()) * radius;
        GLMath.mat4x4_look_at(view
                , new float[]{camX, 0.0f, camZ}
                , new float[]{0.0f, 0.0f, 0.0f}
                , new float[]{0.0f, 1.0f, 0.0f});
        //view = glm::lookAt (glm::vec3 (camX, 0.0f, camZ),glm::vec3 (0.0f, 0.0f, 0.0f),glm::vec3 (0.0f, 1.0f, 0.0f));
        // Projection
        float[] projection = new float[16];
        GLMath.mat4x4_perspective(projection
                , 45.0f, (float) WIDTH / (float) HEIGHT, 0.1f, 100.0f);
        //projection = glm::perspective (45.0f, (GLfloat) WIDTH / (GLfloat) HEIGHT, 0.1f, 100.0f);
        // Get the uniform locations
        int modelLoc = glGetUniformLocation(ourShader, GLUtil.toUtf8("model"));
        int viewLoc = glGetUniformLocation(ourShader, GLUtil.toUtf8("view"));
        int projLoc = glGetUniformLocation(ourShader, GLUtil.toUtf8("projection"));
        // Pass the matrices to the shader
        glUniformMatrix4fv(viewLoc, 1, GL_FALSE, view, 0);
        glUniformMatrix4fv(projLoc, 1, GL_FALSE, projection, 0);


        float[] model = new float[16];
        float[] modelr = new float[16];
        glBindVertexArray(VAO[0]);
        for (int i = 0; i < 10000; i++) {
            // Calculate the model matrix for each object and pass it to shader before drawing
            int idx = i % cubePositions.length;
            GLMath.mat4x4_translate(model, cubePositions[idx][0], cubePositions[idx][1], cubePositions[idx][2]);
            float angle = 52.1f * 1;
            GLMath.mat4x4_rotate(modelr, model, 1.0f, 0.3f, 0.5f, angle);
            glUniformMatrix4fv(modelLoc, 1, GL_FALSE, model, 0);

            glDrawArrays(GL_TRIANGLES, 0, 36);
        }
        glBindVertexArray(0);


        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
        }

    }

    long vg;
    int WIDTH = 800, HEIGHT = 600;
    long curPeriod;
    float[] textColor = {1.0f, 1.0f, 1.0f, 1.0f};
    int winWidth;
    int winHeight;
    int fbWidth;
    int fbHeight;

    void t1() {
        glfwInit();
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_DEPTH_BITS, 16);
        glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);
        long win = glfwCreateWindow(WIDTH, HEIGHT, "hello glfw".getBytes(), 0, 0);
        if (win != 0) {
            glfwSetCallback(win, new CallBack());
            glfwMakeContextCurrent(win);
            //glfwSwapInterval(1);

            vg = nvgCreateGL3(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
            winWidth = Glfw.glfwGetWindowWidth(win);
            winHeight = Glfw.glfwGetWindowHeight(win);
            fbWidth = glfwGetFramebufferWidth(win);
            fbHeight = glfwGetFramebufferHeight(win);
            int w = winWidth;
            int h = winHeight;
            float pxRatio = (float) fbWidth / (float) winWidth;
            System.out.println("w=" + w + "  ,h=" + h);
            init();
            long last = System.currentTimeMillis(), now;
            int count = 0;
            while (!glfwWindowShouldClose(win)) {

                display();
//                nvgBeginFrame(vg, w, h, pxRatio);
//                //drawDebugInfo(vg);
//                Nanovg.nvgReset(vg);
//                Nanovg.nvgResetScissor(vg);
//                Nanovg.nvgScissor(vg, 0, 0, winWidth, winHeight);
//
//
//                GToolkit.drawText(vg, 100, 10, 100, 30, "" + curPeriod, 18, textColor);
//
//                nvgEndFrame(vg);

                glfwPollEvents();
                glfwSwapBuffers(win);
                count++;
                now = System.currentTimeMillis();
                curPeriod = now - last;
                if (now - last > 1000) {
                    System.out.println("fps:" + count);
                    last = now;
                    count = 0;
                }
            }
            glfwTerminate();
        }
    }

    public static void main(String[] args) {
        Shader2 gt = new Shader2();
        gt.t1();

    }


    ShapeData makeCube() {
        ShapeData ret = new ShapeData();
        float[] stackVerts =
                {
                        -1.0f, +1.0f, +1.0f, //0
                        +1.0f, 0.0f, 0.0f,   //Color
                        +1.0f, +1.0f, +1.0f, //1
                        0.0f, +1.0f, 0.0f,    //Color
                        +1.0f, +1.0f, -1.0f, //2
                        0.0f, 0.0f, +1.0f, //Color
                        -1.0f, +1.0f, -1.0f, //3
                        +1.0f, +1.0f, +1.0f, //Color

                        -1.0f, +1.0f, -1.0f, //4
                        +1.0f, 0.0f, +1.0f,   //Color
                        +1.0f, +1.0f, -1.0f, //5
                        0.0f, 0.5f, 0.2f,    //Color
                        +1.0f, -1.0f, -1.0f, //6
                        0.8f, 0.6f, 0.4f, //Color
                        -1.0f, -1.0f, -1.0f, //7
                        0.3f, +1.0f, +0.5f, //Color

                        +1.0f, +1.0f, -1.0f, //8
                        0.2f, 0.5f, 0.2f,  //Color
                        +1.0f, +1.0f, +1.0f, //9
                        0.9f, 0.3f, 0.7f,    //Color
                        +1.0f, -1.0f, +1.0f, //10
                        0.3f, 0.7f, 0.5f,    //Color
                        +1.0f, -1.0f, -1.0f, //11
                        0.5f, 0.7f, 0.5f,  //Color

                        -1.0f, +1.0f, +1.0f, //12
                        0.7f, 0.8f, 0.2f,   //Color
                        -1.0f, +1.0f, -1.0f, //13
                        0.5f, 0.7f, 0.3f,    //Color
                        -1.0f, -1.0f, -1.0f, //14
                        0.8f, 0.6f, 0.4f, //Color
                        -1.0f, -1.0f, +1.0f, //15
                        0.3f, +1.0f, +0.5f, //Color

                        +1.0f, +1.0f, +1.0f, //16
                        0.7f, 0.8f, 0.2f,   //Color
                        -1.0f, +1.0f, +1.0f, //17
                        0.5f, 0.7f, 0.3f,    //Color
                        -1.0f, -1.0f, +1.0f, //18
                        0.8f, 0.6f, 0.4f, //Color
                        +1.0f, -1.0f, +1.0f, //19
                        0.3f, +1.0f, +0.5f, //Color

                        +1.0f, -1.0f, -1.0f, //20
                        0.7f, 0.8f, 0.2f,   //Color
                        -1.0f, -1.0f, -1.0f, //21
                        0.5f, 0.7f, 0.3f,    //Color
                        -1.0f, -1.0f, +1.0f, //22
                        0.8f, 0.6f, 0.4f, //Color
                        +1.0f, -1.0f, +1.0f, //23
                        0.3f, +1.0f, +0.5f //Color
                };
        ret.vertices = stackVerts;

        short stackIndices[] =
                {
                        0, 1, 2, 0, 2, 3,
                        4, 5, 6, 4, 6, 7,
                        8, 9, 10, 8, 10, 11,
                        12, 13, 14, 12, 14, 15,
                        16, 17, 18, 16, 18, 19,
                        20, 22, 21, 20, 23, 22,
                };

        ret.indices = stackIndices;
        return ret;
    }

    static class vec3 {
        float x;
        float y;
        float z;

        public vec3(float px, float py, float pz) {
            x = px;
            y = py;
            z = pz;
        }

        static final int SIZE = FLOAT_SIZE * 4;
    }

    static class mat4 {
        float[] mat = new float[16];

        public mat4() {
            identfy();
        }

        public void identfy() {
            GLMath.mat4x4_identity(mat);
        }

        public void copyTo(float[] arr, int offset) {
            if (arr == null || offset + 16 > arr.length) throw new ArrayIndexOutOfBoundsException();
            System.arraycopy(mat, 0, arr, offset, 16);
        }

        static final int SIZE = FLOAT_SIZE * 16;
    }

    static class Vertex {
        vec3 position;
        vec3 color;

        public Vertex(vec3 p, vec3 c) {
            position = p;
            color = c;
        }

        static final int SIZE = vec3.SIZE * 2;
    }

    static class ShapeData {
        ShapeData() {
        }

        float[] vertices;
        short[] indices;

        int vertexBufferSize() {
            return vertices.length * FLOAT_SIZE;
        }

        int indexBufferSize() {
            return indices.length * SHORT_SIZE;
        }


    }

    ;

    String vertexShaderCode16 = "" +
            "#version 410                                \r\n" +
            " layout(location=0) in vec3 position;\n" +
            " layout(location=1) in vec3 color;\n" +
            " layout(location=2) in mat4 fullTransformMatrix;\n" +
            "\n" +
            "out vec3 passingColor;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "  gl_Position = fullTransformMatrix * vec4(position,1);\n" +
            "  passingColor= color;\n" +
            "}";

    String vertexShaderCode15 = "" +
            "#version 400                                \r\n" +
            "layout(location=0) in vec2 position;\n" +
            "layout(location=1) in float offset;\n" +
            "\n" +
            "out vec3 passingColor;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "\n" +
            "  gl_Position = vec4(position.x + offset, + position.y,0,1);\n" +
            "  passingColor= vec3(1,0,0);\n" +
            "}";

    String vertexShaderCode14 = "" +
            "#version 400                                \r\n" +
            "  layout(location=0) in vec3 position;\n" +
            " layout(location=1) in vec3 vertexColor;\n" +
            "\n" +
            "uniform mat4 fullTransformMatrix;\n" +
            "\n" +
            "out vec3 passingColor;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "  vec4 v =  vec4(position,1.0);\n" +
            "  gl_Position = fullTransformMatrix * v;\n" +
            "  passingColor= vertexColor;\n" +
            "}";

    String fragmentShaderCode = ""
            + "    #version 400                            \r\n"
            + "                                            \r\n"
            + "    in vec3 passingColor;                   \r\n"
            + "    out vec4 finalColor;                    \r\n"
            + "                                            \r\n"
            + "                                            \r\n"
            + "    void main()                             \r\n"
            + "    {                                       \r\n"
            + "      finalColor = vec4(passingColor,1.0);  \r\n"
            + "    }                                       \r\n"
            + "                                            \r\n";

    float verts[] =
            {
                    -1.0f, -1.0f, +0.5f,//Vertex 0
                    +1.0f, +0.0f, +0.0f,//Color  0
                    +0.0f, +1.0f, -0.5f,//Vertex 1
                    +0.0f, +1.0f, +0.0f,//Color  1
                    +1.0f, -1.0f, +0.5f,//Vertex 2
                    +0.0f, +0.0f, +1.0f,//Color  2

                    -1.0f, +1.0f, +0.5f,//Vertex 3
                    +0.5f, +0.3f, +0.1f,//Color  3
                    +0.0f, -1.0f, -0.5f,//Vertex 4
                    +0.1f, +0.4f, +0.2f,//Color  4
                    +1.0f, +1.0f, +0.5f,//Vertex 5
                    +1.0f, +0.5f, +0.2f,//Color  5
            };
    ;
    short indices[] =
            {
                    0, 1, 2,
                    3, 4, 5,
            };
    float tri15[] = {
            -1.0f, +0.0f,
            -1.0f, +1.0f,
            -0.9f, +0.0f
    };
    short indices15[] = {0, 1, 2};
    float offsets15[] = {0.0f, 0.5f, 1.0f, 1.2f, 1.6f};

    int[] vao = {0};
    int[] vertexBufferID = {0};
    int[] indexBufferID = {0};
    int[] offsetsBufferID = {0};
    int[] transformMatrixBufferID = {0};

    static final int FLOAT_SIZE = 4;
    static final int SHORT_SIZE = 2;


    int programID;
    ShapeData shape;

    void sendDataToOpenGL14() {

        programID = loadShader(vertexShaderCode14, fragmentShaderCode);
        shape = makeCube();

        glGenVertexArrays(1, vao, 0);
        glBindVertexArray(vao[0]);
        GLUtil.checkGlError("1");

        glGenBuffers(1, vertexBufferID, 0);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferID[0]);
        glBufferData(GL_ARRAY_BUFFER, shape.vertexBufferSize(), shape.vertices, 0, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, FLOAT_SIZE * 6, null, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, FLOAT_SIZE * 6, null, FLOAT_SIZE * 3);
        GLUtil.checkGlError("5");
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glGenBuffers(1, indexBufferID, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferID[0]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, shape.indexBufferSize(), shape.indices, 0, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        GLUtil.checkGlError("6");
    }

    void display14() {
        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, fbWidth, fbHeight);
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(programID);
        GLUtil.checkGlError("disp 1");

        mat4 projectionMatrix = new mat4();
        mat4 modelTransformMatrix = new mat4();
        projectionMatrix.identfy();
        modelTransformMatrix.identfy();
        GLMath.mat4x4_translate(modelTransformMatrix.mat, 0.0f, 0.0f, -3.0f);
        GLMath.mat4x4_rotate(modelTransformMatrix.mat, modelTransformMatrix.mat, 1.f, 0.f, 0.f, 54f);
        GLMath.mat4x4_perspective(projectionMatrix.mat, 30.0f, ((float) fbWidth) / fbHeight, 0.1f, 10.0f);
        GLMath.mat4x4_mul(projectionMatrix.mat, projectionMatrix.mat, modelTransformMatrix.mat);

        int fullTransformMatrixLocation = glGetUniformLocation(programID, GLUtil.toUtf8("fullTransformMatrix"));
        glUniformMatrix4fv(fullTransformMatrixLocation, 1, GL_FALSE, projectionMatrix.mat, 0);
        GLUtil.checkGlError("disp 1.5");

        glBindVertexArray(vao[0]);
        GLUtil.checkGlError("disp 2");


        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferID[0]);
        GLUtil.checkGlError("disp 5");

        glDrawElements(GL_TRIANGLES, shape.indices.length, GL_UNSIGNED_SHORT, null, 0);
        GLUtil.checkGlError("disp 6");

        //cube2
        projectionMatrix.identfy();
        modelTransformMatrix.identfy();
        GLMath.mat4x4_translate(modelTransformMatrix.mat, 2.0f, 0.0f, -4.0f);
        GLMath.mat4x4_rotate(modelTransformMatrix.mat, modelTransformMatrix.mat, 0.0f, 1.0f, 0.0f, 54f);
        GLMath.mat4x4_perspective(projectionMatrix.mat, 30.0f, ((float) fbWidth) / fbHeight, 0.1f, 10.0f);
        GLMath.mat4x4_mul(projectionMatrix.mat, projectionMatrix.mat, modelTransformMatrix.mat);
        fullTransformMatrixLocation = glGetUniformLocation(programID, GLUtil.toUtf8("fullTransformMatrix"));
        glUniformMatrix4fv(fullTransformMatrixLocation, 1, GL_FALSE, projectionMatrix.mat, 0);
        glDrawElements(GL_TRIANGLES, shape.indices.length, GL_UNSIGNED_SHORT, null, 0);

        glBindVertexArray(0);
        glUseProgram(0);
    }

    void sendDataToOpenGL15() {
        programID = loadShader(vertexShaderCode15, fragmentShaderCode);

        glGenVertexArrays(1, vao, 0);
        glBindVertexArray(vao[0]);
        GLUtil.checkGlError("1");

        glGenBuffers(1, vertexBufferID, 0);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferID[0]);
        glBufferData(GL_ARRAY_BUFFER, tri15.length * FLOAT_SIZE, tri15, 0, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 0, null, 0);
        GLUtil.checkGlError("5");
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glGenBuffers(1, indexBufferID, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferID[0]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices15.length * SHORT_SIZE, indices15, 0, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        glGenBuffers(1, offsetsBufferID, 0);
        glBindBuffer(GL_ARRAY_BUFFER, offsetsBufferID[0]);
        glBufferData(GL_ARRAY_BUFFER, (offsets15.length * FLOAT_SIZE), offsets15, 0, GL_STATIC_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 1, GL_FLOAT, GL_FALSE, 0, null, 0);
        glVertexAttribDivisor(1, 1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        GLUtil.checkGlError("6");
        glBindVertexArray(0);
    }


    void display15() {
        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, fbWidth, fbHeight);
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(programID);
        GLUtil.checkGlError("disp 1");


        glBindVertexArray(vao[0]);
        GLUtil.checkGlError("disp 2");


        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferID[0]);
        GLUtil.checkGlError("disp 4");


        glDrawElementsInstanced(GL_TRIANGLES, indices15.length, GL_UNSIGNED_SHORT, null, 0, 5);
        GLUtil.checkGlError("disp 6");
        glBindVertexArray(0);
        glUseProgram(0);
    }

    void sendDataToOpenGL16() {

        programID = loadShader(vertexShaderCode16, fragmentShaderCode);
        shape = makeCube();

        glGenVertexArrays(1, vao, 0);
        glBindVertexArray(vao[0]);
        GLUtil.checkGlError("1");


        glGenBuffers(1, vertexBufferID, 0);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferID[0]);
        glBufferData(GL_ARRAY_BUFFER, shape.vertexBufferSize(), shape.vertices, 0, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, FLOAT_SIZE * 6, null, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, FLOAT_SIZE * 6, null, FLOAT_SIZE * 3);
        glEnableVertexAttribArray(1);
        GLUtil.checkGlError("5");
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glGenBuffers(1, indexBufferID, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferID[0]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, shape.indexBufferSize(), shape.indices, 0, GL_STATIC_DRAW);


        float[][][] transform = {
                // 0=translate, 1=rotate
                {new float[]{0.0f, 0.0f, -3.0f}, new float[]{1.f, 0.f, 0.f, 54f}},
                {new float[]{2.0f, 0.0f, -4.0f}, new float[]{0.0f, 1.0f, 0.0f, 126}},
        };
        float[] result = new float[transform.length * 16];
        mat4 model = new mat4();
        mat4 projection = new mat4();
        mat4 r = new mat4();
        for (int i = 0; i < transform.length; i++) {
            model.identfy();
            GLMath.mat4x4_translate(model.mat, transform[i][0][0], transform[i][0][1], transform[i][0][2]);
            GLMath.mat4x4_rotate(model.mat, model.mat, transform[i][1][0], transform[i][1][1], transform[i][1][2], transform[i][1][3]);
            projection.identfy();
            GLMath.mat4x4_perspective(projection.mat, 30.0f, ((float) fbWidth) / fbHeight, 0.1f, 10.0f);
            GLMath.mat4x4_mul(r.mat, projection.mat, model.mat);
            r.copyTo(result, i * 16);
            GLUtil.printMat4(r.mat);
        }

        for (int m = 0; m < transform.length; m++) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    System.out.print(" " + result[m * 16 + i * 4 + j]);
                }
                System.out.println();
            }
            System.out.println();
        }


        glGenBuffers(1, transformMatrixBufferID, 0);
        glBindBuffer(GL_ARRAY_BUFFER, transformMatrixBufferID[0]);
        glBufferData(GL_ARRAY_BUFFER, result.length * FLOAT_SIZE, result, 0, GL_STATIC_DRAW);
        glVertexAttribPointer(2, 4, GL_FLOAT, GL_FALSE, mat4.SIZE, null, FLOAT_SIZE * 0);
        glVertexAttribPointer(3, 4, GL_FLOAT, GL_FALSE, mat4.SIZE, null, FLOAT_SIZE * 4);
        glVertexAttribPointer(4, 4, GL_FLOAT, GL_FALSE, mat4.SIZE, null, FLOAT_SIZE * 8);
        glVertexAttribPointer(5, 4, GL_FLOAT, GL_FALSE, mat4.SIZE, null, FLOAT_SIZE * 12);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);
        glEnableVertexAttribArray(4);
        glEnableVertexAttribArray(5);
        glVertexAttribDivisor(2, 1);
        glVertexAttribDivisor(3, 1);
        glVertexAttribDivisor(4, 1);
        glVertexAttribDivisor(5, 1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glBindVertexArray(0);
        GLUtil.checkGlError("6");
    }

    void display16() {
        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, fbWidth, fbHeight);
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(programID);
        GLUtil.checkGlError("disp 1");

        glBindVertexArray(vao[0]);
        GLUtil.checkGlError("disp 2");

        //glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferID[0]);
        GLUtil.checkGlError("disp 5");

        glDrawElementsInstanced(GL_TRIANGLES, shape.indices.length, GL_UNSIGNED_SHORT, null, 0, 2);
        GLUtil.checkGlError("disp 6");

        //glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glUseProgram(0);
    }


    void init() {
        sendDataToOpenGL16();
    }

    void display() {
        display16();
    }
}
