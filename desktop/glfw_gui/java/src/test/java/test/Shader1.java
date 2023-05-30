package test;

import org.mini.gl.GL;

import static org.mini.gl.GL.*;
import static org.mini.glfw.Glfw.*;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author gust
 */
public class Shader1 {

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


    String VertexShaderT = "#version 330 \n" +
            "\n" +
            "in vec3 vertexPosition_modelspace;\n" +
            "\n" +
            "\n" +
            "void main(){\n" +
            "\n" +
            "    gl_Position = vec4(vertexPosition_modelspace,1);\n" +
            "\n" +
            "}\n" +
            "\0";
    String FragmentShaderT = "#version 330 \n" +
            "\n" +
            "out vec3 color;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "\n" +
            "\tcolor = vec3(1,0,0);\n" +
            "}\0";

    float[] g_VertexBufferDataT = {
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f, 0.0f,

    };

    int loadShader(String vss, String fss) {
        int[] return_val = {0};
        //编译顶点着色器
        int vertexShader;
        vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, 1, new byte[][]{GToolkit.toCstyleBytes(vss)}, null, 0);
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
        glShaderSource(fragmentShader, 1, new byte[][]{GToolkit.toCstyleBytes(fss)}, null, 0);
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

    int[] VertexArrayID = {0};
    int[] VertexBufferT = {0};
    int ProgramIDT;
    int VertexPositionIDT;

    public void init() {

        //===Generate the vertex array
        glGenVertexArrays(1, VertexArrayID, 0);
        glBindVertexArray(VertexArrayID[0]);


        glGenBuffers(1, VertexBufferT, 0);
        glBindBuffer(GL_ARRAY_BUFFER, VertexBufferT[0]);
        glBufferData(GL_ARRAY_BUFFER, (long) (g_VertexBufferDataT.length * 4), g_VertexBufferDataT, 0, GL_STATIC_DRAW);

        ProgramIDT = loadShader(VertexShaderT, FragmentShaderT);

    }



    //---------------------------------------------------------------------
    void display() {

        glClearColor(0, 0, 1, 1);
        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT|GL_STENCIL_BUFFER_BIT|GL_ACCUM_BUFFER_BIT);
        glEnable(GL_TEXTURE_2D);
        glUseProgram(ProgramIDT);

        glEnableVertexAttribArray(VertexPositionIDT);

        glBindBuffer(GL_ARRAY_BUFFER, VertexBufferT[0]);
        glVertexAttribPointer(VertexPositionIDT, 3, GL_FLOAT, GL_FALSE, 20, null, 0);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 3);

        glDisableVertexAttribArray(VertexPositionIDT);

        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
        }

    }

    void t1() {
        glfwInit();
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_DEPTH_BITS, 16);
//        glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);
        long win = glfwCreateWindow(640, 480, "hello glfw".getBytes(), 0, 0);
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
        Shader1 gt = new Shader1();
        gt.t1();

    }
}
