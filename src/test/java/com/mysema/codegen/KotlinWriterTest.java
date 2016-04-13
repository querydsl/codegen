/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.codegen;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.Max;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.kotlin.maven.K2JVMCompileMojo;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Function;
import com.mysema.codegen.model.ClassType;
import com.mysema.codegen.model.Parameter;
import com.mysema.codegen.model.SimpleType;
import com.mysema.codegen.model.Type;
import com.mysema.codegen.model.TypeCategory;
import com.mysema.codegen.model.Types;

public class KotlinWriterTest {

    private static final Function<Parameter, Parameter> transformer = new Function<Parameter, Parameter>() {
        @Override
        public Parameter apply(Parameter input) {
            return input;
        }
    };

    private final Writer w = new StringWriter();

    private final KotlinWriter writer = new KotlinWriter(w, true);

    private Type testType, testType2, testSuperType, testInterface1, testInterface2;

    @Before
    public void setUp() {
        testType = new ClassType(JavaWriterTest.class);
        testType2 = new SimpleType("com.mysema.codegen.Test", "com.mysema.codegen", "Test");
        testSuperType = new SimpleType("com.mysema.codegen.Superclass", "com.mysema.codegen",
                "Superclass");
        testInterface1 = new SimpleType("com.mysema.codegen.TestInterface1", "com.mysema.codegen",
                "TestInterface1");
        testInterface2 = new SimpleType("com.mysema.codegen.TestInterface2", "com.mysema.codegen",
                "TestInterface2");
    }

    @Test
    public void Object() throws Exception {
        writer.beginObject("Test");
        writer.end();

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("object Test {"));
        this.compileTest(source);
    }

    @Test
    public void Class() throws Exception {
        writer.beginClass("Test");
        writer.end();

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("class Test {"));
        this.compileTest(source);
    }

    @Test
    public void Class_With_Interfaces() throws Exception {
        writer.line("open class Test");
        writer.line("interface TestInterface1");

        writer.beginClass(testType, testType2, testInterface1);
        writer.end();

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("class JavaWriterTest : Test(), TestInterface1 {"));
        this.compileTest(source);
    }

    @Test
    public void Interface_With_Superinterfaces() throws Exception {
        writer.line("interface Test");
        writer.line("interface TestInterface1");

        writer.beginInterface(testType, testType2, testInterface1);
        writer.end();

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("interface JavaWriterTest : Test, TestInterface1 {"));
        this.compileTest(source);
    }

    @Test
    public void DataParam() throws IOException {
        writer.dataParam(new Parameter("a", Types.STRING));
        writer.line();
        writer.dataParam(new Parameter("b", Types.INT));
        writer.line();
        writer.dataParam(new Parameter("is", Types.LONG));

        String source = w.toString();
        System.out.println(source);

        this.checkParameter();
    }

    @Test
    public void Param() throws IOException {
        writer.param(new Parameter("a", Types.STRING));
        writer.line();
        writer.param(new Parameter("b", Types.INT));
        writer.line();
        writer.param(new Parameter("is", Types.LONG));

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("a: String?"));
        assertTrue(source.contains("b: Int?"));
        assertTrue(source.contains("`is`: Long?"));
    }


    private void checkParameter() {
        assertTrue(w.toString().contains("var a: String?"));
        assertTrue(w.toString().contains("var b: Int?"));
        assertTrue(w.toString().contains("var `is`: Long?"));
    }

    @Test
    public void DataClass() throws Exception {
        writer.beginDataClass("User",
            Arrays.asList(
                new Parameter("a", Types.STRING),
                new Parameter("b", Types.INT),
                new Parameter("is", Types.LONG)));
        writer.end();

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("data class User"));
        this.checkParameter();
        this.compileTest(source);
    }

    @Test
    public void BeanAccessors() throws Exception {
        writer.beginClass(new SimpleType("Person"));

        writer.beginPublicMethod(Types.STRING, "getName");
        writer.line("return \"Daniel Spiewak\"");
        writer.end();

        writer.beginPublicMethod(Types.VOID, "setName", new Parameter("name", Types.STRING));
        writer.line("//");
        writer.end();

        writer.end();

        String source = w.toString();
        System.out.println(source);

        this.compileTest(source);
    }

    @Test
    public void Arrays() throws Exception {
        writer.beginClass(new SimpleType("Main"));
        writer.field(Types.STRING.asArrayType(), "stringArray");
        writer.beginPublicMethod(Types.VOID, "main",
            new Parameter("args", Types.STRING.asArrayType()));
        writer.line("//");
        writer.end();

        writer.beginPublicMethod(Types.VOID, "main2", new Parameter("args", new ClassType(
            TypeCategory.ARRAY, String[].class)));
        writer.line("//");
        writer.end();

        writer.end();

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("var stringArray: Array<String>"));
        assertTrue(source.contains("fun main(args: Array<String>?)"));
        assertTrue(source.contains("fun main2(args: Array<String>?)"));

        this.compileTest(source);
    }

    @Test
    public void Arrays2() throws Exception {
        writer.beginClass(new SimpleType("Main"));
        writer.field(Types.BYTE_P.asArrayType(), "byteArray");
        writer.end();

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("var byteArray: Array<Byte>"));

        this.compileTest(source);
    }

    @Test
    public void Field() throws Exception {
        writer.imports(List.class);
        writer.line("class Person");
        writer.nl();
        writer.beginClass(new SimpleType("Main"));
        writer.privateFinal(new SimpleType(Types.LIST, new SimpleType("Person")), "people");
        writer.end();

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("private val people: List<Person>? = null"));

        this.compileTest(source);
    }

    @Test
    public void Basic() throws Exception {
        writer.packageDecl("com.mysema.codegen");
        writer.imports(IOException.class, StringWriter.class, Test.class);
        writer.beginClass(testType);
        writer.annotation(Test.class);
        writer.beginPublicMethod(Types.VOID, "test");
        writer.line("// TODO");
        writer.end();
        writer.end();

        String source = w.toString();
        System.out.println(source);
    }

    @Test
    public void Extends() throws Exception {
        writer.line("open class Superclass");
        writer.nl();
        writer.beginClass(testType2, testSuperType);
        writer.end();

        String source = w.toString();
        System.out.println(source);

        this.compileTest(source);
    }

    @Test
    public void Implements() throws Exception {
        writer.line("interface TestInterface1");
        writer.nl();
        writer.line("interface TestInterface2");
        writer.nl();
        writer.beginClass(testType2, null, testInterface1, testInterface2);
        writer.end();

        String source = w.toString();
        System.out.println(source);

        this.compileTest(source);
    }

    @Test
    public void Interface() throws Exception {
        writer.packageDecl("com.mysema.codegen");
        writer.imports(IOException.class, StringWriter.class);
        writer.beginInterface(testType);
        writer.end();

        String source = w.toString();
        System.out.println(source);

        this.compileTest(source);
    }

    @Test
    public void Interface2() throws Exception {
        writer.line("interface TestInterface1");
        writer.nl();
        writer.beginInterface(testType2, testInterface1);
        writer.end();

        String source = w.toString();
        System.out.println(source);

        this.compileTest(source);
    }

    @Test
    public void Javadoc() throws Exception {
        writer.packageDecl("com.mysema.codegen");
        writer.imports(IOException.class, StringWriter.class);
        writer.javadoc("JavaWriterTest is a test class");
        writer.beginClass(testType);
        writer.end();

        String source = w.toString();
        System.out.println(source);

        this.compileTest(source);
    }

    @Test
    public void AnnotationConstant() throws Exception {
        Max annotation = new MaxImpl(0l) {
            @Override
            public Class<?>[] groups() {
                return new Class<?>[] { Object.class, String.class, int.class };
            }
        };
        writer.annotation(annotation);

        String source = w.toString();
        System.out.println(source);
    }

    @Test
    public void Annotation_With_ArrayMethod() throws IOException {
        Target annotation = new Target() {
            @Override
            public ElementType[] value() {
                return new ElementType[] { ElementType.FIELD, ElementType.METHOD };
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Target.class;
            }
        };

        writer.imports(Target.class.getPackage());
        writer.annotation(annotation);

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("@Target(arrayOf(FIELD, METHOD))"));
    }

    @Test
    public void Annotations() throws IOException {
        writer.packageDecl("com.mysema.codegen");
        writer.imports(IOException.class, StringWriter.class);
        writer.annotation(Entity.class);
        writer.beginClass(testType);
        writer.annotation(Test.class);
        writer.beginPublicMethod(Types.VOID, "test");
        writer.end();
        writer.end();

        String source = w.toString();
        System.out.println(source);
    }

    @Test
    public void Annotations2() throws IOException {
        writer.packageDecl("com.mysema.codegen");
        writer.imports(IOException.class.getPackage(), StringWriter.class.getPackage());
        writer.annotation(Entity.class);
        writer.beginClass(testType);
        writer.annotation(new Test() {
            @Override
            public Class<? extends Throwable> expected() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public long timeout() {

                return 0;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Test.class;
            }
        });
        writer.beginPublicMethod(Types.VOID, "test");
        writer.end();
        writer.end();

        String source = w.toString();
        System.out.println(source);
    }

    @Test
    public void Fields() throws Exception {
        writer.beginClass(testType);
        // private
        writer.privateField(Types.STRING, "privateField");
        writer.privateStaticFinal(Types.STRING, "privateStaticFinal", "\"val\"");
        // protected
        writer.protectedField(Types.STRING, "protectedField");
        // field
        writer.field(Types.STRING, "field");
        // public
        writer.publicField(Types.STRING, "publicField");
        writer.publicStaticFinal(Types.STRING, "publicStaticFinal", "\"val\"");
        writer.publicFinal(Types.STRING, "publicFinalField");
        writer.publicFinal(Types.STRING, "publicFinalField2", "\"val\"");

        writer.end();

        String source = w.toString();
        System.out.println(source);

        this.compileTest(source);
    }

    @Test
    public void Methods() throws Exception {
        writer.beginClass(testType);
        // private

        // protected

        // method

        // public
        writer.beginPublicMethod(Types.STRING, "publicMethod",
            Arrays.asList(new Parameter("a", Types.STRING)), transformer);
        writer.line("return null");
        writer.end();

        writer.beginStaticMethod(Types.STRING, "staticMethod",
            Arrays.asList(new Parameter("a", Types.STRING)), transformer);
        writer.line("return null");
        writer.end();

        writer.end();

        String source = w.toString();
        System.out.println(source);

        this.compileTest(source);
    }

    @Test
    public void Constructors() throws Exception {
        writer.beginClass(testType);

        writer.beginConstructor(
            Arrays.asList(new Parameter("a", Types.STRING), new Parameter("b", Types.STRING)),
            transformer);
        writer.end();

        writer.beginConstructor(new Parameter("a", Types.STRING));
        writer.end();

        writer.end();

        String source = w.toString();
        System.out.println(source);

        this.compileTest(source);
    }

    @Test
    public void Primitive() throws Exception {
        writer.beginClass(testType);

        writer.beginConstructor(new Parameter("a", Types.INT));
        writer.end();

        writer.end();

        String source = w.toString();
        System.out.println(source);

        assertTrue(source.contains("constructor("));
        assertTrue(source.contains("a: Int?) {"));

        this.compileTest(source);
    }

    @Test
    public void Primitive_Types() throws IOException {
        writer.field(Types.BOOLEAN_P, "field");
        writer.field(Types.BYTE_P, "field");
        writer.field(Types.CHAR, "field");
        writer.field(Types.INT, "field");
        writer.field(Types.LONG_P, "field");
        writer.field(Types.SHORT_P, "field");
        writer.field(Types.DOUBLE_P, "field");
        writer.field(Types.FLOAT_P, "field");

        String source = w.toString();
        System.out.println(source);

        for (String type : Arrays.asList("boolean", "byte", "char", "int", "long", "short",
                "double", "float")) {
            assertTrue(source.contains("`field`: " + StringUtils.capitalize(type)));
        }
    }

    @Test
    public void ReservedWords() throws Exception {
        writer.beginClass(testType);

        writer.beginConstructor(new Parameter("type", Types.INT));
        writer.end();

        writer.publicField(testType, "class");

        writer.beginPublicMethod(testType, "var");
        writer.end();

        writer.end();

        String source = w.toString();
        System.out.println(source);

        assertTrue(w.toString().contains("`type`: Int"));
        assertTrue(w.toString().contains("`class`: JavaWriterTest"));
        assertTrue(w.toString().contains("`var`(): JavaWriterTest"));
    }

    private void compileTest(String source) throws Exception {
        System.out.println(new RuntimeException().getStackTrace()[1]);
        String temp = String.valueOf(System.currentTimeMillis());
        File sourceDir = new File(System.getProperty("java.io.tmpdir"), "sourceDir/" + temp);
        File outputDir = new File(System.getProperty("java.io.tmpdir"), "outputDir/" + temp);
        File testOuputDir = new File(System.getProperty("java.io.tmpdir"), "testOuputDir/" + temp);

        sourceDir.mkdirs();
        outputDir.mkdirs();
        testOuputDir.mkdirs();

        setContent(new File(sourceDir, "Test.kt"), source);

        System.out.println(sourceDir.getCanonicalPath());
        System.out.println(outputDir.getCanonicalPath());
        System.out.println(testOuputDir.getCanonicalPath());

        K2JVMCompileMojo k2JVMCompileMojo = new K2JVMCompileMojo();
        writeField(k2JVMCompileMojo, "sourceDirs", Arrays.asList(sourceDir.getCanonicalPath()));
        writeField(k2JVMCompileMojo, "output", outputDir.getCanonicalPath());
        writeField(k2JVMCompileMojo, "sourceDirs", testOuputDir.getCanonicalPath());
        writeField(k2JVMCompileMojo, "classpath", Arrays.asList("."));
        writeField(k2JVMCompileMojo, "testClasspath", Arrays.asList("."));
        k2JVMCompileMojo.execute();

        sourceDir.delete();
        outputDir.delete();
        testOuputDir.delete();
    }

    public void writeField(Object target, String fieldName, Object value) {
        try {
            FieldUtils.writeField(target, fieldName, value, true);
        } catch (Exception e) {
        }
    }

    public static void setContent(File file, String data) {
        try {
            FileUtils.fileWrite(file, data);
        } catch (Exception e) {
        }
    }
}
