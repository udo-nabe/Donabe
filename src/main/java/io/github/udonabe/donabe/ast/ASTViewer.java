package io.github.udonabe.donabe.ast;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;

public class ASTViewer {
    private static final String CHILD = "├--";
    private static final String LAST_CHILD = "└─";
    private static final String CONTINUATION = "│  ";
    private static final String EMPTY = "    ";

    public static void view(ASTNode root, PrintStream stream) throws IllegalAccessException {
        stream.println(viewImpl(root, "", true));
    }

    public static String view(ASTNode root) throws IllegalAccessException {
        return viewImpl(root, "", true);
    }

    private static String viewImpl(Object node, String prefix, boolean isInsertPrefixBeforeClassName) throws IllegalAccessException {
        StringBuilder result = new StringBuilder();
        if (node == null) return result.toString();
        Class<?> clazz = node.getClass();

        if (isInsertPrefixBeforeClassName) result.append(prefix);
        result.append(clazz.getSimpleName() + "\n");

        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            boolean isLastField = i == fields.length - 1;
            if (isLastField) {
                result.append(prefix + LAST_CHILD);
            } else {
                result.append(prefix + CHILD);
            }
            field.setAccessible(true);
            result.append(field.getName() + ": ");
            Object value = field.get(node);

            String fieldPrefix = prefix + (!isLastField ? CONTINUATION : EMPTY);

            // 配列・Collectionは、見やすくするため展開する
            if (field.getType().isArray()) {
                result.append("[\n");
                Object[] objects = (Object[]) value;
                for (int j = 0; j < objects.length; j++) {
                    Object o = objects[j];
                    result.append(fieldPrefix + (j == objects.length - 1 ? LAST_CHILD : CHILD));
                    result.append("[" + j + "]: ");
                    result.append(viewImpl(o, fieldPrefix + CONTINUATION, false));
                }
                result.append(fieldPrefix);
                result.append("]\n");
                continue;
            }
            if (Collection.class.isAssignableFrom(field.getType())) {
                result.append("[\n");
                int j = 0;
                for (Iterator<?> iterator = ((Collection<?>) value).iterator(); iterator.hasNext(); ) {
                    Object o = iterator.next();
                    result.append(fieldPrefix + (iterator.hasNext() ? CHILD : LAST_CHILD));
                    result.append("[" + j++ + "]: ");
                    result.append(viewImpl(o, fieldPrefix + CONTINUATION, false));
                }
                result.append(fieldPrefix);
                result.append("]\n");
                continue;
            }

            if (!ASTNode.class.isAssignableFrom(field.getType())) { //不要な展開を防ぐため、ASTNodeを実装していないクラスはそのまま出力する
                result.append(value + "\n");
                continue;
            }
            result.append(viewImpl(value, fieldPrefix, false));
        }
        return result.toString();
    }
}
