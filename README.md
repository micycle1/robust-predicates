# Generic Robust Predicates (Java)

A Java port of the framework from **“Fast Floating-Point Filters for Robust
Predicates”** (the C++ implementation in the parent repository), whose end
product is a set of **Shewchuk-like adaptive robust predicate routines**:
flat static methods `orient2d`, `orient3d`, `incircle`, `insphere` that always
return the exact sign while paying the cost of exact arithmetic only on
near-degenerate inputs.

## How it maps to the C++ framework

The C++ version encodes arithmetic expression trees in the type system and
derives everything at compile time. Java has no equivalent, so:

| C++ (compile time) | Java (construction time) |
|---|---|
| type-level expression trees, `std::is_same` dedup | interned immutable `Expression` nodes (`com.github.micycle1.robustpredicates.expr`) — structural equality *is* reference equality |
| `mp_unique<post_order<...>>` | `EvaluationPlan` (deduped post-order, one slot per node) |
| `forward_error_bound.hpp` constexpr rule chain | `ErrorBoundDeriver` (`com.github.micycle1.robustpredicates.errorbound`) run once per predicate |
| compile-time expansion sizes/offsets | `ExpansionSizes` + per-node offsets in `ExpansionEvaluator` |
| template-specialized routines | **generated Java source** (`com.github.micycle1.robustpredicates.generated.RobustPredicates`) |

## Pipeline

Each predicate is one expression tree (see `com.github.micycle1.robustpredicates.expr.Expressions`; the exact
operation order is load-bearing and must not be algebraically rewritten) from
which three stages are derived, chained via the `SIGN_UNCERTAIN = -2`
protocol (`com.github.micycle1.robustpredicates.filter.StagedPredicate`):

1. **Stage A — semi-static filter** (`SemiStaticFilter`): evaluates the
   expression plus an automatically derived error bound
   `constant * magnitude(inputs)`. The constant comes from an eps-polynomial
   analysis (`EpsCoefficients`, rules ported from `forward_error_bound.hpp`,
   including the Ozaki lemma 3.1 with `phi = 94906264` and merged
   underflow-guard terms `count * Double.MIN_NORMAL`). For orient2d the
   derived constant is `3u - 94906236u²` with `u = 2^-53` — slightly *tighter*
   than the classic `3u` bound.
2. **Stage B** (`com.github.micycle1.robustpredicates.expansion.StageB`): assumes all leaf-minus-leaf
   coordinate differences are exact (checks their rounding tails, deferring if
   any is nonzero), which permits much smaller expansions.
3. **Stage D** (`com.github.micycle1.robustpredicates.expansion.StageD`): full Shewchuk expansion arithmetic
   (`ExpansionArithmetic`: TwoSum/TwoDiff tails, FMA TwoProduct, grow /
   expansion-sum / fast-expansion-sum / scale / divide-and-conquer product,
   length-2 square special case). Exact; never uncertain.

Static filters (`StaticFilter`, `AlmostStaticFilter`) are also provided: the
error bound is precomputed once from global coordinate extrema via the
interval transform of the magnitude expression (`IntervalTransformer`, extrema
array layout `[max_1..max_n, min_1..min_n]`).

## Usage

Generated flat routines (recommended; only needs `ExpansionArithmetic` at
runtime):

```java
import com.github.micycle1.robustpredicates.generated.RobustPredicates;

int s = RobustPredicates.orient2d(ax, ay, bx, by, cx, cy);   // 1, 0, -1
int i = RobustPredicates.incircle(ax, ay, bx, by, cx, cy, dx, dy);
```

Interpreted chains (same results bit for bit, fully dynamic):

```java
import com.github.micycle1.robustpredicates.predicates.RobustPredicatesInterpreted;

int s = RobustPredicatesInterpreted.orient2d(ax, ay, bx, by, cx, cy);
```

Sign conventions (documented per method): `orient2d > 0` iff `c` is left of
`a -> b`; `orient3d` is `sign(det[q-p, r-p, s-p])`; `incircle > 0` iff `d` is
inside the circle through counterclockwise `a, b, c`; `insphere` follows the
CGAL `side_of_oriented_sphere_3` row order `p, r, q, s` of the C++ kernel.

## Building and testing

```sh
mvn test                              # full suite (BigDecimal-exact oracles)
mvn -Pcodegen compile exec:java       # regenerate com/github/micycle1/generated/RobustPredicates.java
```

The generated file is checked in; `CodegenDriftTest` fails if it is stale.
Verification includes: exactness of every expansion primitive against
BigDecimal; stage D vs. an independent exact oracle on random, exactly
degenerate (collinear/cocircular/coplanar/cospherical) and ulp-perturbed
near-degenerate inputs; empirical soundness of the derived error bounds
(`|approx - exact| <= bound`); a Kettner-style ulp-grid sign map; the
notebook's near-cocircular rational example; and per-stage bit-for-bit
agreement between interpreted and generated code.

## Design notes and deviations

- **Staged-independent stages**: as in the C++ framework, each stage
  recomputes from the raw arguments (no Shewchuk-style reuse of stage-A
  partial results). True cross-stage adaptivity is future work.
- **Domain restriction**: like Shewchuk's `predicates.c`, stages B/D are
  exact only when no intermediate product over- or underflows; coordinates in
  the denormal range are outside the supported domain. Stage A's underflow
  guards keep the *filter* sound everywhere.
- `double`-only (the C++ `CalculationType` genericity is dropped);
  `Math.fma` implements the TwoProduct tail.
- Zero-elimination policy (`length > 16`) and fast-expansion-sum policy
  (both operands longer than 2) match the C++ defaults; inside the
  divide-and-conquer product, policies are evaluated on dynamic rather than
  static lengths (value-preserving either way).
- The sign of an expansion is read from its last nonzero component instead of
  the C++ `MostSigOnly` accumulation mode (equivalent, simpler).
- The C++ scalar-minus-expansion overload negates the wrong operand
  (computes `f - e`); it is unreachable for these predicates, and the Java
  version implements it correctly.
- Sums/products in error-bound magnitudes share interned subexpressions with
  the main expression, so stage A evaluates common terms once.
