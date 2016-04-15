/*
 * Copyright 2010, Mysema Ltd
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
package com.mysema.codegen.support;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author uuidcode
 *
 */
public final class KotlinSyntaxUtils {

    private KotlinSyntaxUtils() {}

    private static final Set<String> reserved = new HashSet<String>(Arrays.asList("abstract", "do",
            "finally", "import", "object", "return", "data", "var", "val", "_", ":", "case", "else",
            "for", "lazy", "override", "open", "sealed", "try", "while", "=", "=>", "<-", "catch",
            "it", "package", "super", "true", "with", "<:", "class",
            "false", "if", "new", "private", "this", "type", "yield", "<%", ">:", "fun", "final",
            "implicit", "null", "protected", "throw", "#", "@", "constructor", "companion",
            "is", "when", "?:", "internal", "get", "set", "field", "inline"));

    public static boolean isReserved(String token) {
        return reserved.contains(token);
    }
}
