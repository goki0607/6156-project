package com.rvclass.labelchains.monitor;

// security visitor import
import com.rvclass.labelchains.rewriter.SecurityVisitor;

// java standard lib imports 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

// java parser imports
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;

public class App {

    public static void main(String[] args) {
      if (args.length > 0) {
        String[] filenames = args;
        for (String filename : filenames) {
          try {
            CompilationUnit cu = StaticJavaParser.parse(new File(filename));
            ModifierVisitor<Hashtable<String, String>> securityVisitor = new SecurityVisitor();
            securityVisitor.visit(cu, new Hashtable<String, String>());
            String out = cu.toString();
            List<String> lines = Arrays.asList(out.split("\\r?\\n"));
            String out_file = "out/" + filename.substring(0,filename.length()-5) + "Monitorable.java";
            Files.write(new File(out_file).toPath(), lines, StandardCharsets.UTF_8);
            //System.out.println(cu.toString());
          } catch (IOException ex) {
            System.out.println("IO error for file: " + filename + " with exception:" + ex + ".");
          }
        }
      } else {
        System.out.println("Please specify the files you would like to process.");
      }
    }
}
