/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.junit.jupiter;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface BaseExtension {

  default ExtensionContext.Namespace getNamespace(ExtensionContext context) {
    return ExtensionContext.Namespace.create(context.getRequiredTestClass());
  }

  default ExtensionContext.Store getStore(ExtensionContext context) {
    return context.getRoot().getStore(getNamespace(context));
  }

  default Field[] extractFields(ExtensionContext context, Class<?> clazz, Predicate<Field>... predicates) {
    final List<Field> fields = new ArrayList<>();
    if (context.getTestClass().isPresent()) {
      Class<?> testClass = context.getTestClass().orElse(Object.class);
      fields.addAll(extractFields(testClass, clazz, predicates));
      Class<?> enclosingTestClass = testClass.getEnclosingClass();
      while (enclosingTestClass != null) {
        fields.addAll(extractFields(enclosingTestClass, clazz, predicates));
        enclosingTestClass = enclosingTestClass.getEnclosingClass();
      }
    }
    return fields.toArray(new Field[0]);
  }

  default List<Field> extractFields(Class<?> testClass, Class<?> clazz, Predicate<Field>... predicates) {
    final List<Field> fields = new ArrayList<>();
    do {
      fields.addAll(extractFieldsFromClass(testClass, clazz, predicates));
      testClass = testClass.getSuperclass();
    } while (testClass != Object.class);
    return fields;
  }

  /* private */static List<Field> extractFieldsFromClass(Class<?> classWhereFieldIs, Class<?> fieldType,
      Predicate<Field>... predicates) {
    if (classWhereFieldIs != null && classWhereFieldIs != Object.class) {
      Stream<Field> fieldStream = Arrays.stream(classWhereFieldIs.getDeclaredFields())
          .filter(f -> fieldType.isAssignableFrom(f.getType()));
      for (Predicate<Field> p : predicates) {
        fieldStream = fieldStream.filter(p);
      }
      return fieldStream.collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  default void setFieldValue(Field field, Object entity, Object value) throws IllegalAccessException {
    final boolean isAccessible = field.isAccessible();
    field.setAccessible(true);
    field.set(entity, value);
    field.setAccessible(isAccessible);
  }

  default <T extends Annotation> T findAnnotation(Class<?> clazz, Class<T> annotation) {
    T ret = null;
    if (clazz != null) {
      // Current
      if (clazz.isAnnotationPresent(annotation)) {
        ret = clazz.getAnnotation(annotation);
      }
      // Superclass
      if (ret == null && clazz.getSuperclass() != null) {
        ret = findAnnotation(clazz.getSuperclass(), annotation);
      }
      // Enclosing
      if (ret == null && clazz.getEnclosingClass() != null) {
        ret = findAnnotation(clazz.getEnclosingClass(), annotation);
      }
    }
    return ret;
  }

}
