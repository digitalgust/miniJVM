package test;

import org.mini.gl.GL;
import org.mini.glwrap.GLUtil;

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
public class Shader {

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

    int loadShader(int shaderType, String shaderStr) {
        int[] return_val = {0};
        int fragment_shader = glCreateShader(shaderType);
        glShaderSource(fragment_shader, 1, new byte[][]{GLUtil.toUtf8(shaderStr)}, null, 0);
        glCompileShader(fragment_shader);
        GL.glGetShaderiv(fragment_shader, GL.GL_COMPILE_STATUS, return_val, 0);
        if (return_val[0] == GL_FALSE) {
            GL.glGetShaderiv(fragment_shader, GL.GL_INFO_LOG_LENGTH, return_val, 0);
            byte[] szLog = new byte[return_val[0] + 1];
            GL.glGetShaderInfoLog(fragment_shader, szLog.length, return_val, 0, szLog);
            System.out.println("Compile Shader fail error :" + new String(szLog, 0, return_val[0]) + "\n" + shaderStr + "\n");
            return 0;
        }
        return fragment_shader;
    }

    int linkProgram(int vertexShader, int fragmentShader) {
        int[] return_val = {0};
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        GL.glGetProgramiv(program, GL.GL_LINK_STATUS, return_val, 0);
        if (return_val[0] == GL_FALSE) {
            GL.glGetProgramiv(program, GL.GL_INFO_LOG_LENGTH, return_val, 0);
            byte[] szLog = new byte[return_val[0] + 1];
            GL.glGetProgramInfoLog(program, szLog.length, return_val, 0, szLog);
            System.out.println("Link Shader fail error :" + new String(szLog, 0, return_val[0]) + "\n vertex shader:" + vertexShader + "\nfragment shader:" + fragmentShader + "\n");
            return 0;
        }
        return program;
    }

//---------------------------------------------------------------------  
//  
// init  
//  
// init()函数用于设置我们后面会用到的一些数据.例如顶点信息,纹理等  
//  
//
//    String s_v = "#version 330   \n"
//            + "layout(location = 0) in vec4 vPosition;  \n"
//            + "void  \n"
//            + "main()  \n"
//            + " {  \n"
//            + "     gl_Position = vPosition;  \n"
//            + "} ";
//    String s_f = "#version 330   \n"
//            + "out vec4 fColor;  \n"
//            + "void  \n"
//            + "main()  \n"
//            + "{  \n"
//            + "fColor = vec4(0.0, 0.0, 1.0, 1.0);  \n"
//            + "}  ";
    String s_v = "#version 330 \n"
            + "layout(location = 0) in vec4 vPosition; \n"
            + "\n"
            + "void main(){ \n"
            + "gl_Position=vPosition; \n"
            + "} \n";
    String s_f = "#version 330 \n"
            + "precision mediump float; \n"
            + "out vec4 fragColor; \n"
            + "void main(){ \n"
            + "fragColor=vec4(1.0,0.0,0.0,1.0); \n"
            + "} \n";

    int vaoIndex = 0, vaoCount = 1;
    int bufIndex = 0, bufCount = 1;
    int vPosition = 0;

    int[] VAOs = new int[vaoCount];
    int[] BOs = new int[bufCount];

    int vecCount = 6;
        // 我们首先指定了要渲染的两个三角形的位置信息.  
        float[] vertices = new float[]{
            -0.90f, -0.90f, 0, // Triangle 1  
            0.85f, -0.90f, 0,
            -0.90f, 0.85f, 0,
            0.90f, -0.85f, 0, // Triangle 2  
            0.90f, 0.90f, 0,
            -0.85f, 0.90f, 0
        };

        int program;

    void init() {
        glGenVertexArrays(vaoCount, VAOs, 0);
        glBindVertexArray(VAOs[vaoIndex]);


        glGenBuffers(bufCount, BOs, 0);
        glBindBuffer(GL_ARRAY_BUFFER, BOs[bufIndex]);
        GL.glBufferData(GL_ARRAY_BUFFER, (long) (vertices.length * 4), vertices, 0, GL_STATIC_DRAW);

        int vertex_shader, fragment_shader;
        vertex_shader = loadShader(GL_VERTEX_SHADER, s_v);

        fragment_shader = loadShader(GL_FRAGMENT_SHADER, s_f);

        program = linkProgram(vertex_shader, fragment_shader);

        glUseProgram(program);
        // 最后这部分我们成为shader plumbing,  
        // 我们把需要的数据和shader程序中的变量关联在一起,后面会详细讲述  
        glVertexAttribPointer(vPosition, 3, GL_FLOAT, GL_FALSE, 0, null, 0);
        glEnableVertexAttribArray(vPosition);

    }


//---------------------------------------------------------------------  
//  
// display  
//  
// 这个函数是真正进行渲染的地方.它调用OpenGL的函数来请求数据进行渲染.  
// 几乎所有的display函数都会进行下面的三个步骤.  
//  
    void display() {
        // 1. 调用glClear()清空窗口  
        glClear(GL_COLOR_BUFFER_BIT);

//          // 2. 发起OpenGL调用来请求渲染你的对象  
        glBindVertexArray(VAOs[vaoIndex]);
        glDrawArrays(GL_TRIANGLES, 0, vecCount);

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
//        glfwWindowHint(GLFW_DEPTH_BITS, 16);
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
        Shader gt = new Shader();
        gt.t1();

    }
}
