package division.server;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public class DivisionAgent {
    private static Instrumentation inst = null;

    public static void premain(String args, Instrumentation instrumentation) {
        System.out.println("Hello! I`m java agent");
        inst = instrumentation;
    }

    public static boolean addClassPath(File f) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        try {
            if (!(cl instanceof URLClassLoader)) {
                inst.appendToSystemClassLoaderSearch(new JarFile(f));
            }else {
                Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                m.setAccessible(true);
                m.invoke(cl, (Object) f.toURI().toURL());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }
}
