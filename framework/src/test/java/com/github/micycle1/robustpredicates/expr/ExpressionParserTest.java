package com.github.micycle1.robustpredicates.expr;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.micycle1.robustpredicates.expr.Expression.arg;
import static com.github.micycle1.robustpredicates.expr.Expression.diff;
import static com.github.micycle1.robustpredicates.expr.Expression.product;
import static com.github.micycle1.robustpredicates.expr.Expression.sum;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The textual predicate definitions must parse to exactly the intended trees.
 * Because {@link Expression} nodes are interned, structural equality is
 * reference equality, so these are {@code assertSame} checks against
 * programmatically built references.
 */
class ExpressionParserTest {

    @Test
    void leftAssociativityAndPrecedence() {
        List<String> params = List.of("a", "b", "c", "d");
        // a - b + c parses as (a - b) + c.
        assertSame(sum(diff(arg(1), arg(2)), arg(3)),
                ExpressionParser.parse("a - b + c", params));
        // '*' binds tighter than '+'/'-'.
        assertSame(diff(arg(1), product(arg(2), arg(3))),
                ExpressionParser.parse("a - b * c", params));
        // Parentheses override.
        assertSame(product(diff(arg(1), arg(2)), sum(arg(3), arg(4))),
                ExpressionParser.parse("(a - b) * (c + d)", params));
    }

    @Test
    void bindingsShareInternedSubexpressions() {
        Expression e = ExpressionParser.parse("""
                s = a - b
                s * s
                """, List.of("a", "b"));
        Expression s = diff(arg(1), arg(2));
        assertSame(product(s, s), e);
        // A square of the same interned node, which downstream analyses detect.
        assertSame(e.left(), e.right());
    }

    @Test
    void rejectsMalformedBodies() {
        List<String> params = List.of("a", "b");
        assertThrows(IllegalArgumentException.class,
                () -> ExpressionParser.parse("a + q", params));
        assertThrows(IllegalArgumentException.class,
                () -> ExpressionParser.parse("x = a + b", params));           // no result line
        assertThrows(IllegalArgumentException.class,
                () -> ExpressionParser.parse("a +", params));
        assertThrows(IllegalArgumentException.class,
                () -> ExpressionParser.parse("a = a + b\na", params));        // shadows param
        assertThrows(IllegalArgumentException.class,
                () -> ExpressionParser.parse("a + b\na - b", params));        // two results
    }

    @Test
    void orient2dParsesToReferenceTree() {
        // (bx-ax)(cy-ay) - (cx-ax)(by-ay), args ax..cy = 1..6.
        Expression expected = diff(
                product(diff(arg(3), arg(1)), diff(arg(6), arg(2))),
                product(diff(arg(5), arg(1)), diff(arg(4), arg(2))));
        assertSame(expected, Expressions.ORIENT2D);
    }

    @Test
    void orient3dParsesToReferenceTree() {
        // det with rows a-d, b-d, c-d via 2x2 minors of the x/y columns;
        // args ax..dz = 1..12.
        Expression adx = diff(arg(1), arg(10));
        Expression ady = diff(arg(2), arg(11));
        Expression adz = diff(arg(3), arg(12));
        Expression bdx = diff(arg(4), arg(10));
        Expression bdy = diff(arg(5), arg(11));
        Expression bdz = diff(arg(6), arg(12));
        Expression cdx = diff(arg(7), arg(10));
        Expression cdy = diff(arg(8), arg(11));
        Expression cdz = diff(arg(9), arg(12));
        Expression ab = diff(product(adx, bdy), product(bdx, ady));
        Expression ac = diff(product(adx, cdy), product(cdx, ady));
        Expression bc = diff(product(bdx, cdy), product(cdx, bdy));
        Expression expected = sum(
                diff(product(ab, cdz), product(ac, bdz)), product(bc, adz));
        assertSame(expected, Expressions.ORIENT3D);
    }

    @Test
    void incircleParsesToReferenceTree() {
        // Lifted 3x3 determinant over rows a-d, b-d, c-d; args ax..dy = 1..8.
        Expression adx = diff(arg(1), arg(7));
        Expression ady = diff(arg(2), arg(8));
        Expression bdx = diff(arg(3), arg(7));
        Expression bdy = diff(arg(4), arg(8));
        Expression cdx = diff(arg(5), arg(7));
        Expression cdy = diff(arg(6), arg(8));
        Expression aLift = sum(product(adx, adx), product(ady, ady));
        Expression bLift = sum(product(bdx, bdx), product(bdy, bdy));
        Expression cLift = sum(product(cdx, cdx), product(cdy, cdy));
        Expression bcDet = diff(product(bdx, cdy), product(bdy, cdx));
        Expression acDet = diff(product(adx, cdy), product(ady, cdx));
        Expression abDet = diff(product(adx, bdy), product(ady, bdx));
        Expression expected = sum(
                diff(product(aLift, bcDet), product(bLift, acDet)),
                product(cLift, abDet));
        assertSame(expected, Expressions.INCIRCLE);
    }

    @Test
    void insphereParsesToReferenceTree() {
        // Lifted 4x4 determinant over rows a-e, b-e, c-e, d-e;
        // args ax..ez = 1..15.
        Expression aex = diff(arg(1), arg(13));
        Expression aey = diff(arg(2), arg(14));
        Expression aez = diff(arg(3), arg(15));
        Expression alift = sum(sum(product(aex, aex), product(aey, aey)), product(aez, aez));
        Expression bex = diff(arg(4), arg(13));
        Expression bey = diff(arg(5), arg(14));
        Expression bez = diff(arg(6), arg(15));
        Expression blift = sum(sum(product(bex, bex), product(bey, bey)), product(bez, bez));
        Expression cex = diff(arg(7), arg(13));
        Expression cey = diff(arg(8), arg(14));
        Expression cez = diff(arg(9), arg(15));
        Expression clift = sum(sum(product(cex, cex), product(cey, cey)), product(cez, cez));
        Expression dex = diff(arg(10), arg(13));
        Expression dey = diff(arg(11), arg(14));
        Expression dez = diff(arg(12), arg(15));
        Expression dlift = sum(sum(product(dex, dex), product(dey, dey)), product(dez, dez));
        Expression ab = diff(product(bex, aey), product(aex, bey));
        Expression ac = diff(product(cex, aey), product(aex, cey));
        Expression ad = diff(product(dex, aey), product(aex, dey));
        Expression bc = diff(product(cex, bey), product(bex, cey));
        Expression bd = diff(product(dex, bey), product(bex, dey));
        Expression cd = diff(product(dex, cey), product(cex, dey));
        Expression abc = sum(diff(product(bc, aez), product(ac, bez)), product(ab, cez));
        Expression abd = sum(diff(product(bd, aez), product(ad, bez)), product(ab, dez));
        Expression acd = sum(diff(product(cd, aez), product(ad, cez)), product(ac, dez));
        Expression bcd = sum(diff(product(cd, bez), product(bd, cez)), product(bc, dez));
        Expression expected = diff(
                sum(diff(product(bcd, alift), product(acd, blift)), product(abd, clift)),
                product(abc, dlift));
        assertSame(expected, Expressions.INSPHERE);
    }

    @Test
    void diametralCircleParsesToReferenceTree() {
        Expression apx = diff(arg(1), arg(5));
        Expression apy = diff(arg(2), arg(6));
        Expression bpx = diff(arg(3), arg(5));
        Expression bpy = diff(arg(4), arg(6));
        assertSame(sum(product(apx, bpx), product(apy, bpy)),
                Expressions.DIAMETRAL_CIRCLE_2D);
    }
}
