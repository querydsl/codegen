/*
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysema.codegen;

import static com.mysema.codegen.Symbols.ASSIGN;
import static com.mysema.codegen.Symbols.COMMA;
import static com.mysema.codegen.Symbols.DOT;
import static com.mysema.codegen.Symbols.QUOTE;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.mysema.codegen.model.Parameter;
import com.mysema.codegen.model.Type;
import com.mysema.codegen.model.Types;
import com.mysema.codegen.support.KotlinSyntaxUtils;

/**
 * @author uuidcode
 * 
 */
public class KotlinWriter extends AbstractCodeWriter<KotlinWriter> {

    private static final Set<String> PRIMITIVE_TYPES = new HashSet<String>(Arrays.asList("boolean",
            "byte", "char", "int", "long", "short", "double", "float"));

    private static final String FUN = "fun ";

    private static final String OVERRIDE_FUN = "override " + FUN;

    private static final String EXTENDS = " : ";

    private static final String IMPORT = "import ";

    private static final String PACKAGE = "package ";

    private static final String PRIVATE = "private ";

    private static final String PROTECTED = "protected ";

    private static final String PUBLIC = "public ";

    private static final String PUBLIC_CLASS = "class ";

    private static final String PUBLIC_DATA_CLASS = "data class ";

    private static final String PUBLIC_OBJECT = "object ";

    private static final String VAR = "var ";

    private static final String VAL = "val ";

    private static final String CONSTRUCTOR = "constructor";

    private static final String INTERFACE = "interface ";

    private final Set<String> classes = new HashSet<String>();

    private final Set<String> packages = new HashSet<String>();

    private Type type;

    private final boolean compact;

    public KotlinWriter(Appendable appendable) {
        this(appendable, false);
    }

    public KotlinWriter(Appendable appendable, boolean compact) {
        super(appendable, 2);
        this.classes.add("java.lang.String");
        this.classes.add("java.lang.Long");
        this.classes.add("java.lang.Object");
        this.classes.add("java.lang.Integer");
        this.classes.add("java.lang.Comparable");
        this.compact = compact;
    }

    @Override
    public KotlinWriter annotation(Annotation annotation) throws IOException {
        beginLine().append("@").appendType(annotation.annotationType());
        Method[] methods = annotation.annotationType().getDeclaredMethods();
        if (methods.length == 1 && methods[0].getName().equals("value")) {
            try {
                Object value = methods[0].invoke(annotation);
                append("(");
                annotationConstant(value);
                append(")");
            } catch (IllegalArgumentException e) {
                throw new CodegenException(e);
            } catch (IllegalAccessException e) {
                throw new CodegenException(e);
            } catch (InvocationTargetException e) {
                throw new CodegenException(e);
            }
        } else {
            boolean first = true;
            for (Method method : methods) {
                try {
                    Object value = method.invoke(annotation);
                    if (value == null
                            || value.equals(method.getDefaultValue())
                            || (value.getClass().isArray() && Arrays.equals((Object[]) value,
                                    (Object[]) method.getDefaultValue()))) {
                        continue;
                    } else if (!first) {
                        append(COMMA);
                    } else {
                        append("(");
                    }
                    append(escape(method.getName())).append("=");
                    annotationConstant(value);
                } catch (IllegalArgumentException e) {
                    throw new CodegenException(e);
                } catch (IllegalAccessException e) {
                    throw new CodegenException(e);
                } catch (InvocationTargetException e) {
                    throw new CodegenException(e);
                }
                first = false;
            }
            if (!first) {
                append(")");
            }
        }
        return nl();
    }

    @Override
    public KotlinWriter annotation(Class<? extends Annotation> annotation) throws IOException {
        return beginLine().append("@").appendType(annotation).nl();
    }

    @SuppressWarnings("unchecked")
    private void annotationConstant(Object value) throws IOException {
        if (value.getClass().isArray()) {
            append("arrayOf(");
            boolean first = true;
            for (Object o : (Object[]) value) {
                if (!first) {
                    append(", ");
                }
                annotationConstant(o);
                first = false;
            }
            append(")");
        } else if (value instanceof Class) {
            appendType((Class) value);
            append("::class");
        } else if (value instanceof Number || value instanceof Boolean) {
            append(value.toString());
        } else if (value instanceof Enum) {
            Enum<?> enumValue = (Enum<?>) value;
            if (classes.contains(enumValue.getClass().getName())
                    || packages.contains(enumValue.getClass().getPackage().getName())) {
                append(enumValue.name());
            } else {
                append(enumValue.getDeclaringClass().getName()).append(DOT).append(enumValue.name());
            }
        } else if (value instanceof String) {
            append(QUOTE).append(StringUtils.escapeJava(value.toString())).append(QUOTE);
        } else {
            throw new IllegalArgumentException("Unsupported annotation value : " + value);
        }
    }

    private KotlinWriter appendType(Class<?> type) throws IOException {
        if (type.isPrimitive()) {
            append(StringUtils.capitalize(type.getName()));
        } else if (type.getPackage() == null || classes.contains(type.getName())
                || packages.contains(type.getPackage().getName())) {
            append(type.getSimpleName());
        } else {
            append(type.getName());
        }
        return this;
    }

    public KotlinWriter beginObject(String header) throws IOException {
        line(PUBLIC_OBJECT, header, " {");
        goIn();
        return this;
    }

    public KotlinWriter beginClass(String header) throws IOException {
        line(PUBLIC_CLASS, header, " {");
        goIn();
        return this;
    }

    public KotlinWriter beginDataClass(String header, List<Parameter> parameterList) throws IOException {
        beginLine(PUBLIC_DATA_CLASS, header)
            .dataParams(parameterList.toArray(new Parameter[0]))
            .line(" {");
        goIn();
        return this;
    }

    @Override
    public KotlinWriter beginClass(Type type) throws IOException {
        return beginClass(type, null);
    }

    @Override
    public KotlinWriter beginClass(Type type, Type superClass, Type... interfaces)
            throws IOException {
        packages.add(type.getPackageName());
        beginLine(PUBLIC_CLASS, getGenericName(false, type));
        if (superClass != null) {
            append(EXTENDS).append(getGenericName(false, superClass));
            append("()");
        }

        if (interfaces.length > 0) {
            if (superClass == null) {
                append(EXTENDS);
                append(getGenericName(false, interfaces[0]));
                append(COMMA);
                for (int i = 1; i < interfaces.length; i++) {
                    if (i > 1) {
                        append(COMMA);
                    }
                    append(getGenericName(false, interfaces[i]));
                }
            } else {
                append(COMMA);
                for (int i = 0; i < interfaces.length; i++) {
                    if (i > 0) {
                        append(COMMA);
                    }
                    append(getGenericName(false, interfaces[i]));
                }
            }
        }
        append(" {").nl().nl();
        goIn();
        this.type = type;
        return this;
    }

    @Override
    public <T> KotlinWriter beginConstructor(Collection<T> parameters,
            Function<T, Parameter> transformer) throws IOException {
        beginLine(CONSTRUCTOR).params(parameters, transformer).append(" {").nl();
        return goIn();
    }

    @Override
    public KotlinWriter beginConstructor(Parameter... params) throws IOException {
        beginLine(CONSTRUCTOR).params(params).append(" {").nl();
        return goIn();
    }

    @Override
    public KotlinWriter beginInterface(Type type, Type... interfaces) throws IOException {
        packages.add(type.getPackageName());
        beginLine(INTERFACE, getGenericName(false, type));
        if (interfaces.length > 0) {
            append(EXTENDS);
            append(getGenericName(false, interfaces[0]));
            if (interfaces.length > 1) {
                append(COMMA);
                for (int i = 1; i < interfaces.length; i++) {
                    if (i > 1) {
                        append(COMMA);
                    }
                    append(getGenericName(false, interfaces[i]));
                }
            }

        }
        append(" {").nl().nl();
        goIn();
        this.type = type;
        return this;
    }

    private KotlinWriter beginMethod(String modifiers, Type returnType, String methodName,
            Parameter... args) throws IOException {
        if (returnType.equals(Types.VOID)) {
            beginLine(modifiers, escape(methodName)).params(args).append(" {").nl();
        } else {
            beginLine(modifiers, escape(methodName)).params(args)
                    .append(": ").append(getGenericName(true, returnType)).append("? {").nl();
        }

        return goIn();
    }

    @Override
    public <T> KotlinWriter beginPublicMethod(Type returnType, String methodName,
            Collection<T> parameters, Function<T, Parameter> transformer) throws IOException {
        return beginMethod(FUN, returnType, methodName, transform(parameters, transformer));
    }

    @Override
    public KotlinWriter beginPublicMethod(Type returnType, String methodName, Parameter... args)
            throws IOException {
        return beginMethod(FUN, returnType, methodName, args);
    }

    public <T> KotlinWriter beginOverridePublicMethod(Type returnType, String methodName,
                                                     Collection<T> parameters, Function<T, Parameter> transformer)
            throws IOException {
        return beginMethod(OVERRIDE_FUN, returnType, methodName, transform(parameters, transformer));
    }

    public KotlinWriter beginOverridePublicMethod(Type returnType, String methodName, Parameter... args)
            throws IOException {
        return beginMethod(OVERRIDE_FUN, returnType, methodName, args);
    }

    @Override
    public <T> KotlinWriter beginStaticMethod(Type returnType, String methodName,
            Collection<T> parameters, Function<T, Parameter> transformer) throws IOException {
        return beginMethod(FUN, returnType, methodName, transform(parameters, transformer));
    }

    @Override
    public KotlinWriter beginStaticMethod(Type returnType, String methodName, Parameter... args)
            throws IOException {
        return beginMethod(FUN, returnType, methodName, args);
    }

    @Override
    public KotlinWriter end() throws IOException {
        goOut();
        return line("}").nl();
    }

    public KotlinWriter field(Type type, String name) throws IOException {
        line(VAR, escape(name), ": ", getGenericName(true, type), "? = null");
        return compact ? this : nl();
    }

    private KotlinWriter field(String modifier, String variable, Type type, String name) throws IOException {
        line(modifier, variable, escape(name), ": ", getGenericName(true, type));
        return compact ? this : nl();
    }

    private KotlinWriter field(String modifier, String variable, Type type, String name, String value)
            throws IOException {
        line(modifier, variable, escape(name), ": ", getGenericName(true, type), "?", ASSIGN, value);
        return compact ? this : nl();
    }

    @Override
    public String getClassConstant(String className) {
        return className + "::class";
    }

    @Override
    public String getGenericName(boolean asArgType, Type type) {
        if (type.getParameters().isEmpty()) {
            return getRawName(type);
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(getRawName(type));
            builder.append("<");
            boolean first = true;
            String fullName = type.getFullName();
            for (Type parameter : type.getParameters()) {
                if (!first) {
                    builder.append(", ");
                }
                if (parameter == null || parameter.getFullName().equals(fullName)) {
                    builder.append("_");
                } else {
                    builder.append(getGenericName(false, parameter));
                }
                first = false;
            }
            builder.append(">? = null");
            return builder.toString();
        }
    }

    @Override
    public String getRawName(Type type) {
        String fullName = type.getFullName();
        if (PRIMITIVE_TYPES.contains(fullName)) {
            fullName = StringUtils.capitalize(fullName);
        }
        String packageName = type.getPackageName();
        if (packageName != null && packageName.length() > 0) {
            fullName = packageName + "." + fullName.substring(packageName.length()+1).replace('.', '$');
        } else {
            fullName = fullName.replace('.', '$');
        }
        String rv = fullName;
        if (type.isPrimitive() && packageName.isEmpty()) {
            rv = Character.toUpperCase(rv.charAt(0)) + rv.substring(1);
        }
        if (packages.contains(packageName) || classes.contains(fullName)) {
            if (packageName.length() > 0) {
                rv = fullName.substring(packageName.length() + 1);
            }
        }
        if (rv.endsWith("[]")) {
            rv = rv.substring(0, rv.length() - 2);
            if (PRIMITIVE_TYPES.contains(rv)) {
                rv = StringUtils.capitalize(rv);
            } else if (classes.contains(rv)) {
                rv = rv.substring(packageName.length() + 1);
            }
            return "Array<" + rv + ">";
        } else {
            return rv;
        }
    }

    @Override
    public KotlinWriter imports(Class<?>... imports) throws IOException {
        for (Class<?> cl : imports) {
            classes.add(cl.getName());
            line(IMPORT, cl.getName());
        }
        nl();
        return this;
    }

    @Override
    public KotlinWriter imports(Package... imports) throws IOException {
        for (Package p : imports) {
            packages.add(p.getName());
            line(IMPORT, p.getName(), ".*");
        }
        nl();
        return this;
    }

    @Override
    public KotlinWriter importClasses(String... imports) throws IOException {
        for (String cl : imports) {
            classes.add(cl);
            line(IMPORT, cl);
        }
        nl();
        return this;
    }

    @Override
    public KotlinWriter importPackages(String... imports) throws IOException {
        for (String p : imports) {
            packages.add(p);
            line(IMPORT, p, ".*");
        }
        nl();
        return this;
    }

    @Override
    public KotlinWriter javadoc(String... lines) throws IOException {
        line("/**");
        for (String line : lines) {
            line(" * ", line);
        }
        return line(" */");
    }

    @Override
    public KotlinWriter packageDecl(String packageName) throws IOException {
        packages.add(packageName);
        return line(PACKAGE, packageName).nl();
    }

    private <T> KotlinWriter params(Collection<T> parameters, Function<T, Parameter> transformer)
        throws IOException {
        append("(");
        boolean first = true;
        for (T param : parameters) {
            if (!first) {
                append(COMMA);
            }
            param(transformer.apply(param));
            first = false;
        }
        append(")");
        return this;
    }

    private KotlinWriter params(Parameter... params) throws IOException {
        append("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                append(COMMA);
                nl();
            }
            param(params[i]);
        }
        append(")");
        return this;
    }

    private KotlinWriter dataParams(Parameter... params) throws IOException {
        append("(");
        nl();

        goIn();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                append(COMMA);
                nl();
            }
            beginLine("");
            dataParam(params[i]);
        }
        append(")");
        goOut();
        return this;
    }

    public KotlinWriter dataParam(Parameter parameter) throws IOException {
        append(VAR);
        return this.param(parameter);
    }

    public KotlinWriter param(Parameter parameter) throws IOException {
        append(escape(parameter.getName()));
        append(": ");
        append(getGenericName(true, parameter.getType()));
        append("?");
        return this;
    }

    @Override
    public KotlinWriter privateField(Type type, String name) throws IOException {
        return field(PRIVATE, VAR, type, name, null);
    }

    @Override
    public KotlinWriter privateFinal(Type type, String name) throws IOException {
        return field(PRIVATE, VAL, type, name);
    }

    @Override
    public KotlinWriter privateFinal(Type type, String name, String value) throws IOException {
        return field(PRIVATE, VAL, type, name, value);
    }

    @Override
    public KotlinWriter privateStaticFinal(Type type, String name, String value) throws IOException {
        return field(PRIVATE, VAL, type, name, value);
    }

    @Override
    public KotlinWriter protectedField(Type type, String name) throws IOException {
        return field(PROTECTED, VAR, type, name, null);
    }

    @Override
    public KotlinWriter protectedFinal(Type type, String name) throws IOException {
        return field(PROTECTED, VAL, type, name);
    }

    @Override
    public KotlinWriter protectedFinal(Type type, String name, String value) throws IOException {
        return field(PROTECTED, VAL, type, name, value);
    }

    @Override
    public KotlinWriter publicField(Type type, String name) throws IOException {
        return field(type, name);
    }

    @Override
    public KotlinWriter publicField(Type type, String name, String value) throws IOException {
        return field("", VAR, type, name, value);
    }

    @Override
    public KotlinWriter publicFinal(Type type, String name) throws IOException {
        return field(type, name);
    }

    @Override
    public KotlinWriter publicFinal(Type type, String name, String value) throws IOException {
        return field(PUBLIC, VAL, type, name, value);
    }

    @Override
    public KotlinWriter publicStaticFinal(Type type, String name, String value) throws IOException {
        return field(PUBLIC, VAL, type, name, value);
    }

    @Override
    public KotlinWriter staticimports(Class<?>... imports) throws IOException {
        throw new UnsupportedOperationException("not support static imports");
    }

    @Override
    public KotlinWriter suppressWarnings(String type) throws IOException {
        return line("@SuppressWarnings(\"", type, "\")");
    }

    @Override
    public CodeWriter suppressWarnings(String... types) throws IOException {
        return annotation(new MultiSuppressWarnings(types));
    }

    private <T> Parameter[] transform(Collection<T> parameters,
            Function<T, Parameter> transformer) {
        Parameter[] rv = new Parameter[parameters.size()];
        int i = 0;
        for (T value : parameters) {
            rv[i++] = transformer.apply(value);
        }
        return rv;
    }

    private String escape(String token) {
        if (KotlinSyntaxUtils.isReserved(token)) {
            return "`" + token + "`";
        } else {
            return token;
        }
    }
}
