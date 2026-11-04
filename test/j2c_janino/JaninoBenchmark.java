package probe;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.codehaus.janino.Compiler;

/** The same fresh-process batch compilation workload on miniJVM and j2c. */
public class JaninoBenchmark {
    public static void main(String[] args) throws Exception {
        long driverStart = System.nanoTime();
        File repo = new File(JaninoPaths.REPO);
        Properties config = new Properties();
        try (FileInputStream input = new FileInputStream(new File(repo,
                "j2c/build/janino-benchmark/current-run.properties"))) {
            config.load(input);
        }
        File out = new File(config.getProperty("output"));
        if (out.exists()) throw new IllegalStateException("Output must be fresh: " + out);
        if (!out.mkdirs()) throw new IllegalStateException("Cannot create " + out);
        File src = new File(repo, "extlib/xgui/src/main/java");
        List<File> files = new ArrayList<File>();
        JaninoNativeProbe.collect(src, files);
        Compiler compiler = new Compiler();
        compiler.setTargetVersion(8);
        compiler.setBootClassPath(new File[] {new File(repo, "binary/lib/minijvm_rt.jar")});
        compiler.setClassPath(new File[] {
            new File(repo, "binary/libex/glfw_gui.jar"),
            new File(repo, "mobile/assets/resfiles/glfm_gui.jar")
        });
        compiler.setSourcePath(new File[] {src});
        compiler.setDestinationDirectory(out, true);
        compiler.setCharacterEncoding("UTF-8");
        long initialHeap = Runtime.getRuntime().maxMemory();
        File[] units = files.toArray(new File[files.size()]);
        long compileStart = System.nanoTime();
        if (!compiler.compile(units)) throw new AssertionError("Janino compilation failed");
        long finish = System.nanoTime();
        System.out.println("JANINO_BENCHMARK_PASS units=" + files.size()
                + " compile_ms=" + (finish - compileStart) / 1000000
                + " driver_ms=" + (finish - driverStart) / 1000000
                + " initial_heap=" + initialHeap
                + " final_heap=" + Runtime.getRuntime().maxMemory());
    }
}
