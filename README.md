[![](https://jitpack.io/v/micycle1/robust-predicates.svg)](https://jitpack.io/#micycle1/robust-predicates)

# Robust Predicates

A Java framework for generating **adaptive exact geometric predicates**.

This project implements the approach described in [*Fast Floating-Point Filters for Robust Predicates*](https://doi.org/10.1007/s10543-023-00975-x): predicates are evaluated first with fast floating-point filters, then fall back to expansion arithmetic only when the sign cannot be certified.

The generated predicates return the exact sign of their defining expression:

- `1` — positive
- `0` — exactly zero
- `-1` — negative

This is useful for geometric algorithms such as Delaunay triangulation, convex hull construction, and mesh generation, where an incorrect sign near a degeneracy can cause failures.

## What this repository contains

The repository has two Maven modules.

`predicates` is the dependency-free runtime library. It contains the generated `RobustPredicates` class and its expansion-arithmetic support code. This is the only module needed when using the predicates in an application.

`framework` is the predicate-generation framework. It parses predicate expressions, derives floating-point error bounds, builds exact expansion-arithmetic fallbacks, and generates the flat Java routines included in `predicates`. Use it when defining or modifying predicates.

## Included predicates

The generated `RobustPredicates` class currently provides:

- `orient2d`
- `orient3d`
- `incircle`
- `insphere`
- `diametralCircle2d`

```java
import com.github.micycle1.robustpredicates.RobustPredicates;

int orientation = RobustPredicates.orient2d(ax, ay, bx, by, cx, cy);
int circle = RobustPredicates.incircle(ax, ay, bx, by, cx, cy, dx, dy);
int sphere = RobustPredicates.insphere(
    ax, ay, az,
    bx, by, bz,
    cx, cy, cz,
    dx, dy, dz,
    ex, ey, ez
);
```

Sign conventions follow Shewchuk's `predicates.c` conventions:

- `orient2d(a, b, c)` is positive when `c` is to the left of the directed line `a → b`; zero means the points are collinear.
- `orient3d(a, b, c, d)` is positive when `d` lies below the oriented plane through `a`, `b`, and `c`; zero means the points are coplanar.
- `incircle(a, b, c, d)` is positive when `d` lies inside the circumcircle of counterclockwise `a`, `b`, and `c`; zero means cocircular.
- `insphere(a, b, c, d, e)` is positive when `e` lies inside the sphere through `a`, `b`, `c`, and `d`, provided `orient3d(a, b, c, d)` is positive; zero means cospherical.
- `diametralCircle2d(a, b, p)` is negative when `p` lies inside the circle with diameter `ab`; zero means `p` lies on that circle.

The 2D descriptions assume a conventional coordinate system with the y-axis pointing upward. For screen coordinates, where y increases downward, the geometric interpretation of positive and negative orientation is reversed.

## How it works

Each predicate is defined as an expression tree. From that expression, the framework derives a staged evaluator:

1. **Floating-point filter**  
   Evaluates the expression in `double` arithmetic and compares the result with an automatically derived forward error bound. Most inputs are resolved here.

2. **Exact-difference expansion stage**  
   Handles cases where coordinate differences are exactly representable, using smaller expansions.

3. **Full expansion-arithmetic stage**  
   Uses Shewchuk-style floating-point expansions to determine the exact sign.

Only uncertain inputs proceed to the next stage. The final stage is exact and always returns `-1`, `0`, or `1`.

The generated implementation is straight-line Java code with its derived error-bound constants embedded directly in the source.

## Defining a predicate

Predicates are written as simple expressions with optional named intermediate values.

```java
PredicateSpec spec = new PredicateSpec(
    "incircle",
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
    "Positive iff d lies inside the circumcircle through a, b, and c."
);
```

Expressions support `+`, `-`, `*`, and parentheses. Two helpers are also available:

- `det2(a, b, c, d)` expands to `a * d - b * c`
- `sumSq(x, y)` expands to `x * x + y * y`
- `sumSq(x, y, z)` expands to `(x * x + y * y) + z * z`

The written operation order is preserved. Expressions are not algebraically simplified or reassociated, because evaluation order affects both the derived error bound and the exact fallback structure.

Add a specification to `Expressions.SPECS`, then regenerate the runtime predicates:

```sh
mvn -Pcodegen -pl framework -am process-classes
```

This writes the generated `RobustPredicates.java` file into the `predicates` module.

## Static filters

The framework also supports static filters for algorithms with known coordinate bounds.

A `StaticFilter` computes one error bound for a known coordinate domain. An `AlmostStaticFilter` maintains coordinate extrema incrementally and refreshes that bound as needed. These filters can reduce per-call work in batch or incremental geometric algorithms.

## Building

```sh
mvn test
```

Regenerate the checked-in predicate source:

```sh
mvn -Pcodegen -pl framework -am process-classes
```

## Using via JitPack

This repository is published on [JitPack](https://jitpack.io/#micycle1/Fast-Floating-Point-Filters-for-Robust-Predicates). Because it's a multi-module Maven project, add JitPack's repository once, then depend on the single module you need — you don't have to (and shouldn't) pull in the parent POM or the `framework` module unless you're regenerating predicates.

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.micycle1.Fast-Floating-Point-Filters-for-Robust-Predicates</groupId>
  <artifactId>robust-predicates</artifactId>
  <version>x.x</version>
</dependency>
```

## Notes

- All arithmetic uses `double`.
- `Math.fma` is used to compute exact product tails where available.
- Exact fallback stages assume intermediate products do not overflow or underflow, matching the domain restrictions of Shewchuk-style expansion predicates.
- The stage-A floating-point filter includes underflow protection and remains sound outside that narrower exact-expansion domain.
