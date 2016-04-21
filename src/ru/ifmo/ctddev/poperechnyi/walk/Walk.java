package ru.ifmo.ctddev.poperechnyi.walk;


import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Walk {
    private static BufferedWriter out;
    private static MD5Visitor md5;
    private static String ZEROES = "00000000000000000000000000000000 ";

    static class MD5Visitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                System.out.println("No md5 algorithm provided.");
                e.printStackTrace();
                return FileVisitResult.TERMINATE;
            }

            try (InputStream in = new BufferedInputStream(Files.newInputStream(path));
                 DigestInputStream din = new DigestInputStream(in, md)) {
                byte buf[] = new byte[1024];
                while (din.read(buf) > 0) ;

                final BigInteger res = new BigInteger(1, md.digest());
                out.write(String.format("%032X", res) + " " + path.toString() + '\n');
            } catch (IOException e) {
                System.out.println("Calculation error.");
                out.write(ZEROES + path.toString() + '\n');
                e.printStackTrace();
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path path, IOException exc) throws IOException {
            out.write(ZEROES + path.toString() + '\n');
            return FileVisitResult.CONTINUE;
        }
    }

    public static void main(String[] args) {
        if (args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Usage: " +
                    "$ RecursiveWalk input_file output_file");
            return;
        }
        md5 = new MD5Visitor();

        try (BufferedReader in = Files.newBufferedReader(Paths.get(args[0]), Charset.forName("UTF-8"))) {
            try {
                out = Files.newBufferedWriter(Paths.get(args[1]));
                String s = null;
                while (true) {
                    try {
                        s = in.readLine();
                    } catch (IOException e) {
                        System.out.println("Problems with file reading: " + '\n' + e.getMessage());
                    }
                    if (s == null) break;
                    Files.walkFileTree(Paths.get(s), md5);
                }
            } catch (IOException e) {
                System.out.println("Output file couldn't be processed: " + '\n' + e.getMessage());
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    System.out.println("Output file couldn't be closed" + '\n' + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Input file couldn't be processed: " + e.getMessage());
        }
    }
}