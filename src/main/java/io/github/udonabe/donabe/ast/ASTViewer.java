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

    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLO = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";

    public static void view(ASTNode root, PrintStream stream) throws IllegalAccessException {
        viewImpl(root, stream, "", true);
    }

    private static void viewImpl(Object node, PrintStream stream, String prefix, boolean isInsertPrefixBeforeClassName) throws IllegalAccessException {
        if (node == null) return;
        Class<?> clazz = node.getClass();

        if (isInsertPrefixBeforeClassName) stream.print(prefix);
        stream.println(CYAN + clazz.getSimpleName() + RESET);

        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            boolean isLastField = i == fields.length - 1;
            if (isLastField) {
                stream.print(prefix + LAST_CHILD);
            } else {
                stream.print(prefix + CHILD);
            }
            field.setAccessible(true);
            stream.print(YELLO + field.getName() + RESET + ": ");
            Object value = field.get(node);

            String fieldPrefix = prefix + (!isLastField ? CONTINUATION : EMPTY);

            // 配列・Collectionは、見やすくするため展開する
            if (field.getType().isArray()) {
                stream.println("[");
                Object[] objects = (Object[]) value;
                for (int j = 0; j < objects.length; j++) {
                    Object o = objects[j];
                    stream.print(fieldPrefix + (j == objects.length - 1 ? LAST_CHILD : CHILD));
                    stream.print("[" + j + "]: ");
                    viewImpl(o, stream, fieldPrefix + CONTINUATION, false);
                }
                stream.print(fieldPrefix);
                stream.println("]");
                continue;
            }
            if (Collection.class.isAssignableFrom(field.getType())) {
                stream.println("[");
                int j = 0;
                for (Iterator<?> iterator = ((Collection<?>) value).iterator(); iterator.hasNext(); ) {
                    Object o = iterator.next();
                    stream.print(fieldPrefix + (iterator.hasNext() ? CHILD : LAST_CHILD));
                    stream.print("[" + j++ + "]: ");
                    viewImpl(o, stream, fieldPrefix + CONTINUATION, false);
                }
                stream.print(fieldPrefix);
                stream.println("]");
                continue;
            }

            if (!ASTNode.class.isAssignableFrom(field.getType())) { //不要な展開を防ぐため、ASTNodeを実装していないクラスはそのまま出力する
                stream.println(GREEN + value + RESET);
                continue;
            }
            viewImpl(value, stream, fieldPrefix, false);
        }
    }
}
