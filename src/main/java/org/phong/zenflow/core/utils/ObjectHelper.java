//package org.phong.zenflow.core.utils;
//
//
//import org.apache.commons.lang3.builder.Diff;
//import org.apache.commons.lang3.builder.DiffBuilder;
//import org.apache.commons.lang3.builder.DiffResult;
//import org.apache.commons.lang3.builder.ReflectionDiffBuilder;
//import org.apache.commons.lang3.builder.ToStringStyle;
//import org.phong.zenflow.core.dtos.FieldValueChange;
//
//import java.util.Map;
//import java.util.stream.Collectors;
//
//public class ObjectHelper {
//    public static <T> Map<String, FieldValueChange> extractChangesWithCommonsLang(T oldObject, T newObject) {
//        if (oldObject == null || newObject == null) {
//            throw new IllegalArgumentException("Input objects cannot be null.");
//        }
//        if (!oldObject.getClass().equals(newObject.getClass())) {
//            throw new IllegalArgumentException("Objects must be of the same class to compare.");
//        }
//
//        DiffBuilder<T> builder = new DiffBuilder.Builder<T>()
//                .setLeft(oldObject)
//                .setRight(newObject)
//                .setStyle(ToStringStyle.SHORT_PREFIX_STYLE)
//                .setTestObjectsEquals(false)
//                .build();
//        DiffResult<T> diff = new ReflectionDiffBuilder.Builder<T>().setDiffBuilder(builder).build().build();
//
//
//        return diff.getDiffs().stream()
//                .collect(Collectors.toMap(
//                        Diff::getFieldName, // Key: Field name
//                        d -> new FieldValueChange(d.getLeft(), d.getRight()) // Value: Your custom object
//                ));
//    }
//}
