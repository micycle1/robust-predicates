package com.github.micycle1.robustpredicates.expr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a small textual expression language into {@link Expression} trees, so
 * that predicates can be written the way they appear in the literature instead
 * of as nested factory calls.
 *
 * <p>A body is a sequence of lines: zero or more named bindings
 * ({@code name = expression}) followed by one final bare expression, which is
 * the result. Blank lines and {@code //} comments are ignored. Expressions use
 * {@code +}, {@code -}, {@code *} and parentheses; identifiers refer to the
 * predicate's parameters (bound to argument slots in declaration order) or to
 * earlier bindings. For example:
 *
 * <pre>{@code
 * // orient2d with params ax, ay, bx, by, cx, cy
 * (bx - ax) * (cy - ay) - (cx - ax) * (by - ay)
 * }</pre>
 *
 * <p>Two built-in functions expand common determinant patterns so predicates
 * read the way they appear in the literature. They are macros — expanded at
 * parse time into ordinary nodes, with a single fixed operation order and no
 * new node kinds:
 * <ul>
 *   <li>{@code det2(a, b, c, d)} → {@code a*d - b*c}, the determinant of the
 *       matrix {@code [[a, b], [c, d]]};</li>
 *   <li>{@code sumSq(x, y)} → {@code x*x + y*y}, and
 *       {@code sumSq(x, y, z)} → {@code (x*x + y*y) + z*z} (left-associative).</li>
 * </ul>
 * Arguments may themselves be arbitrary expressions, and each occurrence
 * composes from the same interned nodes, so a {@code sumSq} square reuses the
 * identical operand ({@code product(x, x)}), preserving square detection.
 *
 * <p>The parse is literal: {@code +} and {@code -} associate to the left,
 * {@code *} binds tighter, the two functions expand to their fixed forms, and
 * no algebraic simplification or reassociation of any kind is performed. This
 * matters — the exact operation order of a predicate expression is
 * semantically load-bearing for the derived error bounds and for the structure
 * of the exact stages, and a binding used twice refers to the identical
 * (interned) subexpression, which downstream analyses rely on (e.g. square
 * detection).
 */
public final class ExpressionParser {

    private ExpressionParser() {
    }

    /**
     * Parses {@code body}, binding {@code params.get(i)} to argument slot
     * {@code i + 1}, and returns the expression of the final line.
     */
    public static Expression parse(String body, List<String> params) {
        Map<String, Expression> scope = new HashMap<>();
        for (int i = 0; i < params.size(); i++) {
            String name = params.get(i);
            if (scope.put(name, Expression.arg(i + 1)) != null) {
                throw new IllegalArgumentException("duplicate parameter: " + name);
            }
        }
        Expression result = null;
        for (String rawLine : body.split("\n")) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (result != null) {
                throw new IllegalArgumentException(
                        "the bare result expression must be the last line: " + line);
            }
            int eq = line.indexOf('=');
            if (eq >= 0) {
                String name = line.substring(0, eq).trim();
                if (!isIdentifier(name)) {
                    throw new IllegalArgumentException("invalid binding name: " + name);
                }
                Expression value = parseExpression(line.substring(eq + 1), scope);
                if (scope.put(name, value) != null) {
                    throw new IllegalArgumentException("rebinding of: " + name);
                }
            } else {
                result = parseExpression(line, scope);
            }
        }
        if (result == null) {
            throw new IllegalArgumentException("body has no result expression");
        }
        return result;
    }

    private static String stripComment(String line) {
        int comment = line.indexOf("//");
        return comment >= 0 ? line.substring(0, comment) : line;
    }

    private static boolean isIdentifier(String s) {
        if (s.isEmpty() || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        return s.chars().allMatch(Character::isJavaIdentifierPart);
    }

    // ------------------------------------------------------------------
    // Recursive-descent parser over one line
    // ------------------------------------------------------------------

    private static Expression parseExpression(String text, Map<String, Expression> scope) {
        Cursor cursor = new Cursor(text);
        Expression e = additive(cursor, scope);
        cursor.skipWhitespace();
        if (!cursor.atEnd()) {
            throw cursor.error("unexpected trailing input");
        }
        return e;
    }

    /** {@code term (('+' | '-') term)*}, left-associative. */
    private static Expression additive(Cursor cursor, Map<String, Expression> scope) {
        Expression left = term(cursor, scope);
        while (true) {
            cursor.skipWhitespace();
            if (cursor.consume('+')) {
                left = Expression.sum(left, term(cursor, scope));
            } else if (cursor.consume('-')) {
                left = Expression.diff(left, term(cursor, scope));
            } else {
                return left;
            }
        }
    }

    /** {@code factor ('*' factor)*}, left-associative. */
    private static Expression term(Cursor cursor, Map<String, Expression> scope) {
        Expression left = factor(cursor, scope);
        while (true) {
            cursor.skipWhitespace();
            if (cursor.consume('*')) {
                left = Expression.product(left, factor(cursor, scope));
            } else {
                return left;
            }
        }
    }

    /** Number, identifier, or parenthesized expression. */
    private static Expression factor(Cursor cursor, Map<String, Expression> scope) {
        cursor.skipWhitespace();
        if (cursor.consume('(')) {
            Expression inner = additive(cursor, scope);
            cursor.skipWhitespace();
            if (!cursor.consume(')')) {
                throw cursor.error("expected ')'");
            }
            return inner;
        }
        if (cursor.atEnd()) {
            throw cursor.error("expected a value");
        }
        char c = cursor.peek();
        if (Character.isDigit(c) || c == '.') {
            return Expression.constant(cursor.number());
        }
        if (Character.isJavaIdentifierStart(c)) {
            String name = cursor.identifier();
            if (!cursor.atEnd() && cursor.peek() == '(') {
                return call(name, arguments(cursor, scope), cursor);
            }
            Expression bound = scope.get(name);
            if (bound == null) {
                throw cursor.error("unknown identifier: " + name);
            }
            return bound;
        }
        throw cursor.error("unexpected character: '" + c + "'");
    }

    /** Parses {@code '(' expr (',' expr)* ')'} into an argument list. */
    private static List<Expression> arguments(Cursor cursor, Map<String, Expression> scope) {
        cursor.consume('(');
        List<Expression> args = new ArrayList<>();
        args.add(additive(cursor, scope));
        cursor.skipWhitespace();
        while (cursor.consume(',')) {
            args.add(additive(cursor, scope));
            cursor.skipWhitespace();
        }
        if (!cursor.consume(')')) {
            throw cursor.error("expected ',' or ')' in argument list");
        }
        return args;
    }

    /** Expands a built-in function call into its fixed-order expression. */
    private static Expression call(String name, List<Expression> args, Cursor cursor) {
        switch (name) {
            case "det2":
                if (args.size() != 4) {
                    throw cursor.error("det2 takes 4 arguments, got " + args.size());
                }
                // |a b; c d| = a*d - b*c
                return Expression.diff(
                        Expression.product(args.get(0), args.get(3)),
                        Expression.product(args.get(1), args.get(2)));
            case "sumSq":
                if (args.size() == 2) {
                    return Expression.sum(square(args.get(0)), square(args.get(1)));
                }
                if (args.size() == 3) {
                    return Expression.sum(
                            Expression.sum(square(args.get(0)), square(args.get(1))),
                            square(args.get(2)));
                }
                throw cursor.error("sumSq takes 2 or 3 arguments, got " + args.size());
            default:
                throw cursor.error("unknown function: " + name);
        }
    }

    private static Expression square(Expression e) {
        return Expression.product(e, e);
    }

    private static final class Cursor {
        private final String text;
        private int pos;

        Cursor(String text) {
            this.text = text;
        }

        void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        boolean atEnd() {
            return pos >= text.length();
        }

        char peek() {
            return text.charAt(pos);
        }

        boolean consume(char c) {
            if (!atEnd() && text.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        double number() {
            int start = pos;
            while (!atEnd() && (Character.isDigit(peek()) || peek() == '.')) {
                pos++;
            }
            try {
                return Double.parseDouble(text.substring(start, pos));
            } catch (NumberFormatException e) {
                throw error("invalid number: " + text.substring(start, pos));
            }
        }

        String identifier() {
            int start = pos;
            while (!atEnd() && Character.isJavaIdentifierPart(peek())) {
                pos++;
            }
            return text.substring(start, pos);
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(
                    message + " at position " + pos + " in: " + text.trim());
        }
    }
}
