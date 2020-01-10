package division.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DivisionClassLoader extends ClassLoader {
    private List<String> paths = new ArrayList<>();

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for(String path:paths) {
            for(File file:getLibs(new File(path))) {

            }
        }


        byte[] b = loadClassFromFile(name);
        return defineClass(name, b, 0, b.length);
    }

    private List<File> getLibs(File dir) {
        List<File> libs = new ArrayList<>();
        for(File file:dir.listFiles()) {
            if(file.isDirectory())
                libs.addAll(getLibs(file));
            else libs.add(file);
        }
        return libs;
    }

    public void addPath(String path) {
        paths.add(path);
    }

    public void delPath(String path) {
        paths.remove(path);
    }

    private byte[] loadClassFromFile(String fileName)  {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName/*.replace('.', File.separatorChar) + ".class"*/);
        byte[] buffer = new byte[0];
        int nextValue = 0;

        try(ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            while ( (nextValue = inputStream.read()) != -1 ) {
                byteStream.write(nextValue);
            }
            buffer = byteStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }
}
