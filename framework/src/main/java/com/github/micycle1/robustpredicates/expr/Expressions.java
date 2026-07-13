package com.github.micycle1.robustpredicates.expr;

import java.util.List;

/**
 * The standard predicate definitions, written in the textual expression
 * language of {@link ExpressionParser}. The written operation order is
 * semantically load-bearing: the forward error-bound derivation and the
 * exactness structure of the expansion stages depend on it, so no
 * algebraically equivalent rewrites are permitted.
 *
 * <p>Sign conventions follow Shewchuk's classic {@code predicates.c}:
 * <ul>
 *   <li>{@code orient2d(a, b, c)}: positive iff {@code c} lies to the left of
 *       the directed line {@code a -> b} (triangle {@code abc} winds
 *       counterclockwise), negative to the right, zero iff collinear.</li>
 *   <li>{@code orient3d(a, b, c, d)}: positive iff {@code d} lies below the
 *       plane through {@code a, b, c}, where "below" means {@code a, b, c}
 *       appear counterclockwise when viewed from above the plane; zero iff
 *       coplanar.</li>
 *   <li>{@code incircle(a, b, c, d)}: positive iff {@code d} lies inside the
 *       circle through {@code a, b, c} when {@code abc} winds
 *       counterclockwise; zero iff cocircular.</li>
 *   <li>{@code insphere(a, b, c, d, e)}: positive iff {@code e} lies inside
 *       the sphere through {@code a, b, c, d} when {@code orient3d(a, b, c, d)}
 *       is positive; zero iff cospherical.</li>
 * </ul>
 *
 * <p>The 2D conventions above assume the usual mathematical orientation with
 * the y axis pointing up. In a screen coordinate system with y pointing down,
 * the geometric reading flips: positive {@code orient2d} then means {@code c}
 * is to the <em>right</em> of {@code a -> b} (clockwise winding), and
 * "counterclockwise {@code abc}" reads as clockwise. The algebraic signs and
 * exactness guarantees are unaffected.
 */
public final class Expressions {

    private Expressions() {
    }

    public static final PredicateSpec ORIENT2D_SPEC = new PredicateSpec("orient2d",
            List.of("ax", "ay", "bx", "by", "cx", "cy"),
            """
            (bx - ax) * (cy - ay) - (cx - ax) * (by - ay)
            """,
            "Positive iff {@code c} lies to the left of the directed line"
                    + " {@code a -> b} (counterclockwise {@code abc}, y axis up);"
                    + " zero iff collinear.");

    public static final PredicateSpec ORIENT3D_SPEC = new PredicateSpec("orient3d",
            List.of("ax", "ay", "az", "bx", "by", "bz",
                    "cx", "cy", "cz", "dx", "dy", "dz"),
            """
            adx = ax - dx
            ady = ay - dy
            adz = az - dz
            bdx = bx - dx
            bdy = by - dy
            bdz = bz - dz
            cdx = cx - dx
            cdy = cy - dy
            cdz = cz - dz
            ab = det2(adx, bdx, ady, bdy)
            ac = det2(adx, cdx, ady, cdy)
            bc = det2(bdx, cdx, bdy, cdy)
            ab * cdz - ac * bdz + bc * adz
            """,
            "Positive iff {@code d} lies below the plane through {@code a, b, c},"
                    + " where \"below\" means {@code a, b, c} appear counterclockwise"
                    + " when viewed from above the plane; zero iff coplanar.");

    public static final PredicateSpec INCIRCLE_SPEC = new PredicateSpec("incircle",
            List.of("ax", "ay", "bx", "by", "cx", "cy", "dx", "dy"),
            """
            adx = ax - dx
            ady = ay - dy
            bdx = bx - dx
            bdy = by - dy
            cdx = cx - dx
            cdy = cy - dy
            alift = sumSq(adx, ady)
            blift = sumSq(bdx, bdy)
            clift = sumSq(cdx, cdy)
            bcdet = det2(bdx, bdy, cdx, cdy)
            acdet = det2(adx, ady, cdx, cdy)
            abdet = det2(adx, ady, bdx, bdy)
            alift * bcdet - blift * acdet + clift * abdet
            """,
            "Positive iff {@code d} lies inside the circle through"
                    + " {@code a, b, c} (counterclockwise {@code abc}, y axis up);"
                    + " zero iff cocircular.");

    public static final PredicateSpec INSPHERE_SPEC = new PredicateSpec("insphere",
            List.of("ax", "ay", "az", "bx", "by", "bz", "cx", "cy", "cz",
                    "dx", "dy", "dz", "ex", "ey", "ez"),
            """
            aex = ax - ex
            aey = ay - ey
            aez = az - ez
            alift = sumSq(aex, aey, aez)
            bex = bx - ex
            bey = by - ey
            bez = bz - ez
            blift = sumSq(bex, bey, bez)
            cex = cx - ex
            cey = cy - ey
            cez = cz - ez
            clift = sumSq(cex, cey, cez)
            dex = dx - ex
            dey = dy - ey
            dez = dz - ez
            dlift = sumSq(dex, dey, dez)
            ab = det2(bex, aex, bey, aey)
            ac = det2(cex, aex, cey, aey)
            ad = det2(dex, aex, dey, aey)
            bc = det2(cex, bex, cey, bey)
            bd = det2(dex, bex, dey, bey)
            cd = det2(dex, cex, dey, cey)
            abc = bc * aez - ac * bez + ab * cez
            abd = bd * aez - ad * bez + ab * dez
            acd = cd * aez - ad * cez + ac * dez
            bcd = cd * bez - bd * cez + bc * dez
            bcd * alift - acd * blift + abd * clift - abc * dlift
            """,
            "Positive iff {@code e} lies inside the sphere through"
                    + " {@code a, b, c, d}, where the four points are ordered so"
                    + " that {@code orient3d(a, b, c, d)} is positive; zero iff"
                    + " cospherical.");

    public static final PredicateSpec DIAMETRAL_CIRCLE_2D_SPEC =
            new PredicateSpec("diametralCircle2d",
                    List.of("ax", "ay", "bx", "by", "px", "py"),
                    """
                    apx = ax - px
                    apy = ay - py
                    bpx = bx - px
                    bpy = by - py
                    apx * bpx + apy * bpy
                    """,
                    "Sign of {@code (a-p) dot (b-p)}. Negative iff {@code p} lies inside"
                            + " the circle having {@code ab} as its diameter; zero iff"
                            + " {@code p} lies on that circle (equivalently,"
                            + " {@code angle apb} is a right angle).");

    /** All standard predicate definitions, in generation order. */
    public static final List<PredicateSpec> SPECS = List.of(
            ORIENT2D_SPEC, ORIENT3D_SPEC, INCIRCLE_SPEC,
            DIAMETRAL_CIRCLE_2D_SPEC, INSPHERE_SPEC);

    public static final Expression ORIENT2D = ORIENT2D_SPEC.expression();
    public static final Expression ORIENT3D = ORIENT3D_SPEC.expression();
    public static final Expression INCIRCLE = INCIRCLE_SPEC.expression();
    public static final Expression INSPHERE = INSPHERE_SPEC.expression();
    public static final Expression DIAMETRAL_CIRCLE_2D = DIAMETRAL_CIRCLE_2D_SPEC.expression();
}
