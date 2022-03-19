package test;

import org.mini.gl.GL;
import org.mini.gl.GLMath;
import org.mini.gui.GToolkit;
import org.mini.glwrap.GLUtil;

import static org.mini.gl.GL.*;
import static org.mini.glfw.Glfw.*;

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

    public void init() {

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
        texture1[0] = GLUtil.gl_image_load(GToolkit.readFileFromJar("/res/logo128.png"), whd);
        // ===================
        // Texture 2
        // ===================

        texture2[0] = GLUtil.gl_image_load(GToolkit.readFileFromJar("/res/head.png"), whd);

        glEnable(GL_DEPTH_TEST);

    }


    //---------------------------------------------------------------------
    void display() {

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
        for (int i = 0; i < 10; i++) {
            // Calculate the model matrix for each object and pass it to shader before drawing
            GLMath.mat4x4_translate(model, cubePositions[i][0], cubePositions[i][1], cubePositions[i][2]);
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

    int WIDTH = 800, HEIGHT = 600;

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

            int w = glfwGetFramebufferWidth(win);
            int h = glfwGetFramebufferHeight(win);
            System.out.println("w=" + w + "  ,h=" + h);
            init();
            long last = System.currentTimeMillis(), now;
            int count = 0;
            while (!glfwWindowShouldClose(win)) {

                display();

                glfwPollEvents();
                glfwSwapBuffers(win);
                count++;
                now = System.currentTimeMillis();
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
}
