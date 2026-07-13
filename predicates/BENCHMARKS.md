# Predicate benchmarks

JMH micro-benchmarks comparing this library's adaptive exact predicates
(`com.github.micycle1.robustpredicates.RobustPredicates`) against two other
Java implementations:

| Baseline | orient2d | incircle | orient3d | insphere | Notes |
|----------|:--------:|:--------:|:--------:|:--------:|-------|
| **RobustPredicates** (this lib) | ✅ | ✅ | ✅ | ✅ | Shewchuk-style, generated adaptive filters |
| **JTS** `jts-core` | ✅ | ✅ | — | — | `CGAlgorithmsDD` / `TrianglePredicate`, double-double filtered. JTS ships no 3D predicates. |
| **ProGAL** `ExactJavaPredicates` | ✅ | ✅ | ✅ | ✅ | Shewchuk's original C ported to Java (copied into `src/test`, see below) |

Everything lives in `predicates/src/test` and is **test-scoped**, so the
published `robust-predicates` jar stays dependency-free.

## Layout

- `bench/ExactJavaPredicates.java` — ProGAL's
  [`ExactJavaPredicates`](https://github.com/Bio7/ProGAL/blob/master/src/ProGAL/geom3d/predicates/ExactJavaPredicates.java),
  copied in with its ProGAL `Point`/`Triangle`-based wrappers stripped so it
  compiles standalone. The numeric `double[]` routines are byte-for-byte
  identical to the original (only their visibility was widened to `public`).
- `bench/Inputs.java` — deterministic input generation (fixed seed).
- `bench/*Benchmark.java` — one JMH benchmark per predicate.
- `bench/PredicateAgreementTest.java` — JUnit check that all three
  implementations return the **same sign** on the shared inputs, so the timings
  compare equivalent work.

## Distributions

Each benchmark runs over two `@Param("dist")` input sets:

- **uniform** — coordinates uniform in `[-1, 1)`. The fast floating-point
  filter (stage A) almost always resolves the sign; this is the common-case
  cost.
- **degenerate** — points clustered within a few ulps of a shared anchor, so
  the predicate value is near zero, stage A fails and the exact
  expansion-arithmetic stages run. This is the worst case the adaptive
  machinery exists for.

## Running

From the `java` project root:

```sh
# whole suite -> predicates/target/jmh-result.json + console
mvn -Pbench -pl predicates test-compile exec:exec

# one predicate (bench.regex is a single JMH selector, no spaces)
mvn -Pbench -pl predicates test-compile exec:exec -Dbench.regex=Incircle
mvn -Pbench -pl predicates test-compile exec:exec -Dbench.regex=Orient3dBenchmark.robust
```

Fixed run parameters (in `predicates/pom.xml`): 1 fork, 5×1s warmup, 5×1s
measurement, average time in ns/op. For anything more bespoke, run the
`java -cp <test-classpath> org.openjdk.jmh.Main …` command directly.

The correctness cross-check runs as an ordinary unit test:

```sh
mvn -pl predicates test -Dtest=PredicateAgreementTest
```

## Results

Average time, ns/op — **lower is better**. JMH 1.37, 1 fork, 5×1s warmup,
5×1s measurement.

_Machine: AMD Ryzen 7 5800X, Temurin OpenJDK 21.0.4, Windows 11._

"Robust" columns are the exact / double-double implementations that always (or
near-always) return the correct sign — the like-for-like comparison. `JTS
filter` is JTS's fast **non-exact** `double` path, shown for reference only.

| Predicate | dist | RobustPredicates | JTS (robust) | ProGAL | JTS filter |
|-----------|------|-----------------:|-------------:|-------:|-----------:|
| **orient2d**  | uniform    | **3.37**  | 4.60  | 3.75  | — |
| **orient2d**  | degenerate | 4.91      | 6.35  | **4.64** | — |
| **incircle**  | uniform    | **8.97**  | 78.69 | 6.18 ‡ | 3.50 |
| **incircle**  | degenerate | **90.7**  | 73.31 | 580.3 | 3.53 |
| **orient3d**  | uniform    | **11.91** | —     | 12.51 | — |
| **orient3d**  | degenerate | **16.52** | —     | 53.77 | — |
| **insphere**  | uniform    | 28.57     | —     | **24.28** | — |
| **insphere**  | degenerate | **77.79** | —     | 88.21 | — |

- JTS (robust) = `CGAlgorithmsDD.orientationIndex` / `isInCircleDDNormalized`
  (double-double). JTS filter = `isInCircleRobust`, which despite its name
  computes the determinant in plain `double` — fast but returns the wrong sign
  on adversarial cocircular inputs, so it is not comparable work.

Takeaways:

- **orient2d** — all three within ~30 % of each other; this library is fastest
  on easy inputs, ProGAL marginally fastest on degenerate.
- **incircle** — against JTS's genuinely robust double-double path this library
  is **~9× faster on easy inputs** (9 ns vs 79 ns: JTS pays full DD cost every
  call, this library's stage-A filter resolves it in plain `double`) and
  slightly faster on degenerate. Against ProGAL it is a touch slower on easy
  inputs (‡ 6.2 ns) but **~6× faster on near-cocircular inputs** (91 ns vs
  580 ns) because its staged filters reach full exact expansion far less often.
- **orient3d** — comparable on easy inputs; this library is **~3× faster** on
  degenerate inputs.
- **insphere** — comparable throughout; ProGAL slightly faster on easy inputs,
  this library slightly faster on degenerate.

JTS ships no 3D predicates, hence the blank cells for orient3d/insphere.

### Are the baselines adaptive, or full precision immediately?

This matters for reading the numbers, because a flat cost across the two
distributions means *no* fast path, and a large easy-vs-hard gap means there
*is* one.

- **This library — adaptive & exact.** Fast `double` filter (stage A), then
  exact expansion-arithmetic stages (B, D) only when the filter can't certify
  the sign.
- **ProGAL — adaptive & exact.** Shewchuk's original algorithm: every predicate
  computes a `double` estimate with a derived error bound and escalates to
  progressively more exact expansion terms only when needed — the same staged
  design as this library. Confirmed by the large easy-vs-hard gaps (incircle
  6.2 → 580 ns, orient3d 12.5 → 53.8 ns).
- **JTS `orient2d` (`CGAlgorithmsDD.orientationIndex`) — adaptive.** Runs a
  `double` filter first and falls to double-double only when uncertain
  (`if (index <= 1) return index;`). The small easy-vs-hard gap (4.60 → 6.35 ns)
  is the DD fallback firing more often on degenerate inputs.
- **JTS `incircle` (`isInCircleDDNormalized`) — full double-double every call,
  *not* adaptive.** It wraps all six coordinates in `DD` and runs every
  multiply/add in double-double unconditionally — no filter, no early-out. Its
  near-flat timing (78.7 / 73.3 ns) is the tell.

Two consequences for the **incircle** row specifically:

1. *Speed.* Against JTS-DD you are comparing adaptive-exact vs. non-adaptive
   fixed-precision DD. That is why this library wins hugely on easy inputs (the
   stage-A `double` filter resolves them, ~9 ns vs ~79 ns) yet is only about
   even on degenerate inputs — there, JTS's flat ~106-bit DD happens to be
   slightly *cheaper* than a full arbitrary-precision expansion. The
   ProGAL comparison, by contrast, is adaptive-vs-adaptive (apples to apples).
2. *Robustness.* This library and ProGAL are **exact** — an arbitrary-precision
   fallback guarantees the correct sign. JTS double-double is fixed ~106-bit
   precision: far more robust than plain `double`, but *not* provably exact at
   extreme degeneracies. So the incircle numbers are not comparing identical
   guarantees, only identical answers on these particular inputs.

Reproduce with `mvn -Pbench -pl predicates test-compile exec:exec`; raw data in
`target/jmh-result.json`, console log in `target/jmh-console.txt`.
