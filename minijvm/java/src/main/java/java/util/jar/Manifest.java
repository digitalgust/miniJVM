package java.util.jar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Manifest {
    private final Attributes mainAttributes = new Attributes();

    public Manifest() {
    }

    public Manifest(InputStream stream) throws IOException {
        read(stream);
    }

    public Attributes getMainAttributes() {
        return mainAttributes;
    }

    public void clear() {
        mainAttributes.clear();
    }

    public void read(InputStream stream) throws IOException {
        if (stream == null) throw new NullPointerException();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String currentName = null;
        StringBuilder currentValue = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                store(currentName, currentValue);
                break;
            }
            if (line.charAt(0) == ' ') {
                if (currentName != null) currentValue.append(line.substring(1));
                continue;
            }
            store(currentName, currentValue);
            int separator = line.indexOf(':');
            if (separator <= 0) {
                currentName = null;
                currentValue.setLength(0);
                continue;
            }
            currentName = line.substring(0, separator);
            currentValue.setLength(0);
            int valueStart = separator + 1;
            if (valueStart < line.length() && line.charAt(valueStart) == ' ') valueStart++;
            currentValue.append(line.substring(valueStart));
        }
        store(currentName, currentValue);
    }

    private void store(String name, StringBuilder value) {
        if (name != null) mainAttributes.putValue(name, value.toString());
    }
}