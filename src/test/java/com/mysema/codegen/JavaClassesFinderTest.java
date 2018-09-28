/*
 * Copyright 2018, The Querydsl Team (http://www.querydsl.com/team)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysema.codegen;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import javax.tools.JavaFileObject;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static javax.tools.JavaFileObject.Kind.CLASS;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * JavaClassesFinder unit test
 *
 * @author matteo-gallo-bb
 */
@RunWith(JUnitParamsRunner.class)
public class JavaClassesFinderTest {

    private File tmpBaseDir;
    private File bootJarFile;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        tmpBaseDir = temporaryFolder.newFolder("javaClassesFinderTest");
        bootJarFile = createSampleJarFile();
    }

    @Parameters({"com.mysema.codegen.model, ClassType", ", ClassRoot"})
    @Test
    public void listAll(String packageName, String className) throws IOException {
        // given
        String bootJarFilePath = bootJarFile.getAbsolutePath();
        JavaClassesFinder finder = new JavaClassesFinder(new ClassLoaderMock(bootJarFilePath));

        // when
        List<JavaFileObject> javaFileObjectList = finder.listAll(packageName);

        // then
        assertNotNull(javaFileObjectList);
        assertEquals(1, javaFileObjectList.size());
        assertTrue(javaFileObjectList.get(0) instanceof CompiledJavaFileObject);
        CompiledJavaFileObject javaFileObject = (CompiledJavaFileObject) javaFileObjectList.get(0);
        assertEquals(getFullQualifiedClassName(packageName, className), javaFileObject.binaryName());
        assertEquals(bootJarFilePath + "!/" + fromPackageToFolder(packageName, className), javaFileObject.getName());
    }

    private File createSampleJarFile() throws IOException {
        File classTypeFile = createAndWriteTmpClassFile("ClassType.class");
        File classRootFile = createAndWriteTmpClassFile("ClassRoot.class");

        //create a boot jar file
        File bootJar = new File(tmpBaseDir,"exampleBoot.jar");
        FileOutputStream outputStream = new FileOutputStream(bootJar);
        ZipOutputStream sampleJar = new ZipOutputStream(outputStream);

        // adding ClassRoot file to boot jar file
        ZipEntry zeClassRoot = new ZipEntry(classRootFile.getName());
        sampleJar.putNextEntry(zeClassRoot);
        writeTmpClassFileInJar(classRootFile, sampleJar);

        // adding ClassType file and folders to boot jar file
        sampleJar.putNextEntry(new ZipEntry("com/"));
        sampleJar.putNextEntry(new ZipEntry("com/mysema/"));
        sampleJar.putNextEntry(new ZipEntry("com/mysema/codegen/"));
        sampleJar.putNextEntry(new ZipEntry("com/mysema/codegen/model/"));
        ZipEntry zeClassType = new ZipEntry("com/mysema/codegen/model/" + classTypeFile.getName());
        sampleJar.putNextEntry(zeClassType);
        writeTmpClassFileInJar(classTypeFile, sampleJar);

        sampleJar.close();

        return bootJar;
    }

    private File createAndWriteTmpClassFile(String fileName) throws IOException {
        //create a temp class file
        File classTypeFile = new File(tmpBaseDir, fileName);

        //write it
        BufferedWriter bw = new BufferedWriter(new FileWriter(classTypeFile));
        bw.write("This is the temporary file content");
        bw.close();

        return classTypeFile;
    }

    private void writeTmpClassFileInJar(File classTypeFile, ZipOutputStream sampleJar) throws IOException {
        // writing temp class file in given jar file
        FileInputStream inputStream = new FileInputStream(classTypeFile);
        byte[] buffer = new byte[1024];
        for (int len; (len = inputStream.read(buffer)) > 0;) {
            sampleJar.write(buffer, 0, len);
        }
        inputStream.close();
        sampleJar.closeEntry();
    }

    private String fromPackageToFolder(String packageName, String className) {
        if (packageName.equals("")) {
            return className + CLASS.extension;
        }
        return packageName.replaceAll("\\.", "/") + "/" + className + CLASS.extension;
    }

    private String getFullQualifiedClassName(String packageName, String className) {
        return packageName.equals("") ? className : packageName + "." + className;
    }

    private class ClassLoaderMock extends ClassLoader {

        private String libraryFilePath;

        private ClassLoaderMock(String libraryFilePath) {
            this.libraryFilePath = libraryFilePath;
        }

        @Override
        public Enumeration<URL> getResources(String name) {
            Vector<URL> urlVector = new Vector<URL>();
            try {
                String path = name.equals("") ? name : name + "/";
                urlVector.add(new URL("jar:file:" + libraryFilePath + "!/" + path));
            } catch (MalformedURLException e) {
                fail("File " + libraryFilePath + " not found!");
            }
            return urlVector.elements();
        }
    }
}