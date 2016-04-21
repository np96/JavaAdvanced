package ru.ifmo.ctddev.poperechnyi.implementor;


import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class Implementor implements JarImpler {
    public static void main(String[] args) {
        if (args == null || args.length < 2 || args[0] == null ||
                (args[0].equals("-jar") && args.length < 3)) {
            System.out.println("Usage: $ Implementor [-jar] {target.java, target.jar} /path/to/root");
            return;
        }
        try {
            if (args[0].equals("-jar")) {
                args[2] += '/';
                new Implementor().implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                new Implementor().implement(Class.forName(args[0]), Paths.get(args[1]));
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + args[1]);
        } catch (ImplerException e) {
            System.out.println("Can't implement " + args[0] + "\n" + e.getMessage());
        }
    }

    private static final String TAB = "\t";

    /**
     * Returns implementing class declaration.
     *
     * @param inClass input class.
     * @return String: "class inClassImpl implements inClass".
     */
    private static String className(Class inClass) {
        return "class " + inClass.getSimpleName() + "Impl" + " implements " + inClass.getSimpleName();
    }

    /**
     * @param methods Array of methods.
     * @return ArrayList of implementing methods definitions. If a method from the array is not abstract, it's not added to the result.
     */

    private static ArrayList<String> getMethods(Method[] methods) {
        ArrayList<String> res = new ArrayList<>();
        for (Method method : methods) {
            if (Modifier.isAbstract(method.getModifiers())) {
                String implyingMethod = TAB + "public " + method.getReturnType().getCanonicalName() + " "
                        + method.getName().replace(" abstract", "")
                        + parameters(method.getParameterTypes())
                        + methodBody(method.getReturnType());
                res.add(implyingMethod);
            }
        }
        return res;
    }


    /** Converts array of parameter types to String.
     *
     * @param parameterTypes Argument types
     * @return String of parameters
     */

    private static String parameters(Class<?>[] parameterTypes) {
        StringBuilder builder = new StringBuilder("(");
        int i = 1;
        for (Class<?> param : parameterTypes) {
            builder.append(param.getCanonicalName()).append(" arg").append(i).append(", ");
            i++;
        }
        if (parameterTypes.length == 0) builder.append(')');
        else builder.setCharAt(builder.length() - 2, ')');

        return builder.toString();
    }

    /**
     * @param returnType Class to implement.
     * @return Body of generated method.
     */

    private static String methodBody(Class<?> returnType) {
        String def;
        if (returnType.equals(Void.TYPE)) {
            def = ";\n";
        } else if (returnType.isPrimitive()) {
            if (returnType.equals(Boolean.TYPE)) {
                def = " false;\n";
            } else def = " 0;\n";
        } else {
            def = " null;\n";
        }
        return " {\n" +
                TAB + TAB + "return" + def +
                TAB + "}\n\n";
    }

    /**
     * Implements interface specified by token, if it can be implemented .
     *
     * @param token Interface to implement
     * @param root  path where path to token implementation will be placed (e.g. /users/vasya/java/util/ListImpl.java)
     * @throws ImplerException if interface cannot be implemented.
     */


    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        Class inClass;
        try {
            inClass = token;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ImplerException("Cannot implement class " + token.getName());
        }
        if (Modifier.isFinal(inClass.getModifiers())) {
            throw new ImplerException("Cannot implement class" + inClass.getCanonicalName() + ".\n"
                    + "Class is final.");
        }
        if (inClass.isPrimitive()) {
            throw new ImplerException("Cannot implement primitive class");
        }

        String path = root.toAbsolutePath().toString() + File.separator.concat(inClass.getCanonicalName().replace(".", File.separator).concat("Impl.java"));

        File targetFile = new File(path);
        File parent = targetFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }

        try (Writer wr = Files.newBufferedWriter(targetFile.toPath(), StandardCharsets.UTF_8)) {
            wr.write("package " + inClass.getPackage().getName() + ";\n\n\n");
            wr.write(className(inClass) + " {\n");
            for (String field : getMethods(inClass.getMethods())) {
                wr.write(field);
            }
            wr.write("}");
        } catch (IOException e) {
            e.printStackTrace();
            throw new ImplerException("Problems with writing.");
        }

    }

    /** Same as {@link #implement(Class, Path)} but generates specified jarFile and places tokenImpl in it.
     *
     * @param token Interface to implement
     * @param jarFile jar file name
     * @throws ImplerException if interface cannot be implemented.
     */

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(null);
        } catch (IOException e) {
            throw new ImplerException("Cannot create temp directory.\n" + e.getMessage());
        }
        String tokenPath = token.getCanonicalName().replace(".", File.separator);
        implement(token, tempDir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, tempDir + File.separator + tokenPath + "Impl.java", "-cp", System.getProperty("java.class.path"));
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, tokenPath + "Impl");
        File outFile = new File(jarFile.toFile() + "");
        if (!outFile.exists()) {
            try {
                Files.createDirectories(outFile.getParentFile().toPath());
                Files.createFile(outFile.toPath());
            } catch (IOException e) {
                ImplerException exception = new ImplerException("Can't create jar file");
                exception.addSuppressed(e);
                throw exception;
            }
        }
        try (JarOutputStream jOS = new JarOutputStream(new FileOutputStream(outFile), manifest)) {
            try (FileInputStream inputStream = new FileInputStream(tempDir + File.separator + tokenPath + "Impl.class")) {
                jOS.putNextEntry(new JarEntry(tokenPath + "Impl.class"));
                byte[] buffer = new byte[4096];
                int size;
                while ((size = inputStream.read(buffer)) > 0) {
                    jOS.write(buffer, 0, size);
                }
                jOS.closeEntry();
            }
        } catch (IOException e) {
            ImplerException exception = new ImplerException("Can't create jar output stream");
            exception.addSuppressed(e);
            throw exception;
        } finally {
            try {
                deleteFolder(tempDir.toString());
            } catch (IOException e) {
                if (Files.exists(tempDir)) {
                    System.out.println("Warning!!! Couldn't delete temporary folder " + tempDir.toString() + '\n' + e.getMessage());
                }
            }
        }
    }

    /**
     * Utility function. Deletes temporary folder specified by path
     *
     * @param path to remove
     * @throws IOException if path can't be removed
     */
    private void deleteFolder(String path) throws IOException {
        Path directory = Paths.get(path);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}