# Robust Predicates

A code-generation framework for **adaptive exact geometric predicates** — and
the generated predicates themselves.

A geometric predicate (`orient2d`, `incircle`, `insphere`, ...) is the sign of
a polynomial in point coordinates. Evaluated naively in floating point, the
sign can come out wrong near degeneracy, which breaks algorithms built on it
(Delaunay triangulation, convex hulls, mesh generation). The predicates here
**always return the exact sign**, and are *adaptive*: a fast floating-point
filter answers almost every call, and exact arithmetic runs only when the
input is too close to degenerate for the filter to certify.

Unlike hand-written adaptive predicates, everything is derived automatically
from the predicate's expression tree: the forward error bound of the filter,
the exact expansion-arithmetic stages, and the final straight-line Java code.
Write a new predicate as a formula; get a robust adaptive routine out. The
derived error bounds are as tight as — and sometimes slightly tighter than —
the classic hand-derived ones (for `orient2d` the derived filter constant is
`3u − 94906236u²` with `u = 2⁻⁵³`, just below Shewchuk's classic `3u`), so the
filter certifies at least as many calls.

## Modules

| Module | Artifact | Contents |
|---|---|---|
| `predicates` | `robust-predicates` | The generated predicates (`RobustPredicates`) and the expansion arithmetic they run on. Dependency-free — this is the only artifact needed to *use* the predicates. |
| `framework` | `robust-predicates-framework` | The machinery that produces them: expression trees, error-bound derivation, filters, exact stages, interpreted predicate chains, and the code generator. Only needed to *define* new predicates. |

## Using the predicates

```java
import com.github.micycle1.robustpredicates.RobustPredicates;

int o = RobustPredicates.orient2d(ax, ay, bx, by, cx, cy);            // 1, 0, -1
int i = RobustPredicates.incircle(ax, ay, bx, by, cx, cy, dx, dy);
int s = RobustPredicates.insphere(ax, ay, az, /* b, c, d */ ..., ex, ey, ez);
```

Sign conventions follow Shewchuk's widely used `predicates.c`:

| Predicate | Positive when |
|---|---|
| `orient2d(a, b, c)` | `c` is to the left of the directed line `a → b` (triangle `abc` winds counterclockwise); zero iff collinear. |
| `orient3d(a, b, c, d)` | `d` is below the plane through `a, b, c`, where "below" means `a, b, c` appear counterclockwise viewed from above; zero iff coplanar. |
| `incircle(a, b, c, d)` | `d` is inside the circle through counterclockwise `a, b, c`; zero iff cocircular. |
| `insphere(a, b, c, d, e)` | `e` is inside the sphere through `a, b, c, d` when `orient3d(a, b, c, d)` is positive; zero iff cospherical. |
| `diametralCircle2d(a, b, p)` | negative iff `p` is inside the circle with diameter `ab`; zero on the circle. |

**Y-axis direction.** The 2D descriptions assume the usual mathematical
orientation with the y axis pointing **up**. With screen coordinates (y
pointing down) the geometric reading flips: positive `orient2d` then means
`c` is to the *right* of `a → b` and the winding reads as clockwise. The
algebraic signs and exactness guarantees are unaffected.

**Domain.** Like Shewchuk's routines, the exact stages assume no intermediate
product overflows or underflows; coordinates in the denormal range are outside
the supported domain. The stage-A filter itself carries underflow guards and
stays sound everywhere.

## How it works

Each predicate is one expression tree, from which three stages are derived and
chained; a stage returns `SIGN_UNCERTAIN` when it cannot certify the sign, and
only then does the next (more expensive) stage run:

1. **Stage A — floating-point filter.** Evaluates the expression plus an
   automatically derived error bound of the form
   `constant * magnitude(inputs)`. The constant comes from an eps-polynomial
   analysis of the expression's rounding errors (including a lemma of Ozaki
   et al. for products of differences, and merged underflow-guard terms).
2. **Stage B — exact-difference expansions.** Assumes the leaf coordinate
   differences are exact (verifying their rounding tails, deferring if any is
   nonzero), which permits much smaller expansions.
3. **Stage D — full expansion arithmetic.** Shewchuk-style exact expansion
   evaluation (TwoSum/TwoDiff tails, FMA TwoProduct, grow / expansion-sum /
   fast-expansion-sum / scale / divide-and-conquer product). Exact; never
   uncertain.

The written operation order of an expression is load-bearing — the error
bound and the exactness structure depend on it — so expressions are never
algebraically rewritten or reassociated.

Two interchangeable back ends produce bit-for-bit identical results:

- **Generated** (`RobustPredicates`, recommended): flat straight-line Java
  emitted by the code generator, with the derived error-bound constants baked
  in as hex literals.
- **Interpreted** (`RobustPredicatesInterpreted` in the framework): the same
  staged chains executed directly over the expression tree — no codegen step,
  useful while developing a new predicate.

## Defining a new predicate

Predicates are written as plain text — named intermediate bindings followed by
a final result expression — together with their parameter names:

```java
PredicateSpec spec = new PredicateSpec("diametralCircle2d",
        List.of("ax", "ay", "bx", "by", "px", "py"),
        """
        apx = ax - px
        apy = ay - py
        bpx = bx - px
        bpy = by - py
        apx * bpx + apy * bpy
        """,
        "Negative iff {@code p} lies inside the circle with diameter {@code ab}.");
```

From a spec you can immediately run an interpreted chain:

```java
Expression e = spec.expression();
StagedPredicate p = new StagedPredicate(
        new SemiStaticFilter(e), new StageB(e), new StageD(e));
int sign = p.apply(new double[] {ax, ay, bx, by, px, py});
```

or add it to `Expressions.SPECS` and regenerate the flat routines:

```sh
mvn -Pcodegen -pl framework -am process-classes
```

The generated file is checked in; a drift test fails the build if it goes
stale.

### Static filters

For batch or incremental algorithms whose coordinates have known bounds, the
framework also derives **static** filters: the error bound is computed once
from global per-coordinate extrema (`StaticFilter`), or maintained
incrementally as inputs arrive (`AlmostStaticFilter`), making the per-call
filter just an evaluate-and-compare.

## Building and testing

```sh
mvn test                                     # full suite
mvn -Pcodegen -pl framework -am process-classes   # regenerate RobustPredicates.java
```

Verification includes: exactness of every expansion primitive against
BigDecimal; the exact stages against an independent unlimited-precision oracle
on random, exactly degenerate (collinear / cocircular / coplanar /
cospherical) and ulp-perturbed near-degenerate inputs; empirical soundness of
the derived error bounds (`|approx − exact| ≤ bound`); a Kettner-style
ulp-grid sign map; identity of the parsed expression trees with hand-built
references; and per-stage bit-for-bit agreement between the interpreted and
generated code.

## Design notes

- All arithmetic is `double`; `Math.fma` supplies the exact product tail.
- Each stage recomputes from the raw arguments; Shewchuk-style reuse of a
  stage's partial results by the next stage is future work.
- The expression trees are interned, so structurally equal subexpressions are
  the same object; the filter evaluates subexpressions shared between the
  predicate and its error bound only once.

This project implements, in Java, the predicate-generation approach of
*Fast Floating-Point Filters for Robust Predicates* (see the parent
repository).
