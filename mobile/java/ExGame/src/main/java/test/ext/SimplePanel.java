package test.ext;

import org.mini.gl.GL;
import org.mini.gl.GLMath;
import org.mini.glwrap.GLFrameBuffer;
import org.mini.gui.GForm;
import org.mini.gui.GOpenGLPanel;
import org.mini.gui.GToolkit;

import static org.mini.gl.GL.*;
import static org.mini.gl.GLMath.mat4x4_rotate;
import static org.mini.gl.GLMath.mat4x4_translate;
import static org.mini.glwrap.GLUtil.gl_image_load;
import static org.mini.glwrap.GLUtil.toUtf8;

//https://blog.csdn.net/qq_35294564/article/details/86288352

public class SimplePanel extends GOpenGLPanel {

    public SimplePanel(GForm form) {
        super(form, 0f, 0f, 1f, 1f);
    }

    public SimplePanel(GForm form, float left, float top, float w, float h) {
        super(form, left, top, w, h);
    }


    int loadShader(String vss, String fss) {
        int[] return_val = {0};
        //编译顶点着色器
        int vertexShader;
        vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, 1, new byte[][]{toUtf8(vss)}, null, 0);
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
        glShaderSource(fragmentShader, 1, new byte[][]{toUtf8(fss)}, null, 0);
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
        glDetachShader(shaderProgram, vertexShader);
        glDetachShader(shaderProgram, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return shaderProgram;
    }


    String VertexShaderT = "#version 330\n" +
            "#ifdef GL_ES\n" +
            "    precision lowp float;\n" +
            "#endif\n" +
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
    String FragmentShaderT = "#version 330\n" +
            "#ifdef GL_ES\n" +
            "    precision lowp float;\n" +
            "#endif\n" +
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
            "\tcolor = mix(texture(ourTexture1, TexCoord), texture(ourTexture2, TexCoord), 0.8);\n" +
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

    int[] texture1 = {0}, texture2 = {0};
    int[] VBO = {0}, VAO = {0};
    int ourShader;
    GLFrameBuffer glFrameBuffer;

    String getGLVersion() {
        byte[] OpenGLVersion = glGetString(GL_VERSION);
        String glVersion = new String(OpenGLVersion);
        return glVersion;
    }

    static public String gl3_to_gles3(String glversion, String shaderStr) {
        if (glversion.toLowerCase().contains("opengl es")) {
            String s1 = shaderStr.replace("version 330", "version 300 es");
            return s1;
        }
        return shaderStr;
    }

    public void gl_init() {
        glFrameBuffer = new GLFrameBuffer((int) getW(), (int) getH());
        glFrameBuffer.gl_init();
        setGlRendereredImg(glFrameBuffer.getFboimg());

        ourShader = loadShader(gl3_to_gles3(getGLVersion(), VertexShaderT), gl3_to_gles3(getGLVersion(), FragmentShaderT));

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
        texture1[0] = gl_image_load(GToolkit.readFileFromJar("/res/fern.png"), whd);

        texture2[0] = gl_image_load(GToolkit.readFileFromJar("/res/pine.png"), whd);

        glEnable(GL_DEPTH_TEST);

        //checkGlError("gl_init");
    }

    @Override
    public void gl_destroy() {
        glDeleteTextures(1, texture1, 0);
        glDeleteTextures(1, texture2, 0);
        glDeleteProgram(ourShader);
        glDeleteVertexArrays(1, VAO, 0);
    }

    float time = 0f;

    @Override
    public void gl_paint() {
        glFrameBuffer.begin();

        glEnable(GL_DEPTH_TEST);
        //glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);


        // Activate shader
        glUseProgram(ourShader);

        // Bind Textures using texture units
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture1[0]);
        int location = glGetUniformLocation(ourShader, toUtf8("ourTexture1"));
        glUniform1i(location, 0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, texture2[0]);
        location = glGetUniformLocation(ourShader, toUtf8("ourTexture2"));
        glUniform1i(location, 1);


        // Camera/View transformation
        float[] view = new float[16];
        float radius = 10.0f;
        time += 1f / 60f;
        float camX = (float) Math.sin(time) * radius;
        float camZ = (float) Math.cos(time) * radius;
        //view = glm::lookAt (glm::vec3 (camX, 0.0f, camZ),glm::vec3 (0.0f, 0.0f, 0.0f),glm::vec3 (0.0f, 1.0f, 0.0f));
        GLMath.mat4x4_look_at(view
                , new float[]{camX, 0.0f, camZ}
                , new float[]{0.0f, 0.0f, 0.0f}
                , new float[]{0.0f, 1.0f, 0.0f});
        // Projection
        float[] projection = new float[16];
        //projection = glm::perspective (45.0f, (GLfloat) WIDTH / (GLfloat) HEIGHT, 0.1f, 100.0f);
        GLMath.mat4x4_perspective(projection, 45.0f, getW() / getH(), 0.1f, 100.0f);
        // Get the uniform locations
        int modelLoc = glGetUniformLocation(ourShader, toUtf8("model"));
        int viewLoc = glGetUniformLocation(ourShader, toUtf8("view"));
        int projLoc = glGetUniformLocation(ourShader, toUtf8("projection"));
        // Pass the matrices to the shader
        glUniformMatrix4fv(viewLoc, 1, GL_FALSE, view, 0);
        glUniformMatrix4fv(projLoc, 1, GL_FALSE, projection, 0);

        float[] model = new float[16];
        float[] modelr = new float[16];
        glBindVertexArray(VAO[0]);
        for (int i = 0; i < 10; i++) {
            // Calculate the model matrix for each object and pass it to shader before drawing
            mat4x4_translate(model, cubePositions[i][0], cubePositions[i][1], cubePositions[i][2]);
            float angle = 0.f * 1;
            mat4x4_rotate(modelr, model, 1.0f, 1.0f, 1.0f, angle);
            glUniformMatrix4fv(modelLoc, 1, GL_FALSE, modelr, 0);

            glDrawArrays(GL_TRIANGLES, 0, 36);
        }
        glBindVertexArray(0);

        glFrameBuffer.end();
        GForm.flush();
    }

    public void reSize() {
        if (glFrameBuffer == null) {
            return;
        }
        glFrameBuffer = new GLFrameBuffer((int) getW(), (int) getH());
        glFrameBuffer.gl_init();
        setGlRendereredImg(glFrameBuffer.getFboimg());
    }
}
