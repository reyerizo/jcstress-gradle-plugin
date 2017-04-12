package pl.erizo.gradle.jcstress.reordering;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.LL_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

@JCStressTest
@Description("Tests safeIncrementValue is threadsafe")
@Outcome.Outcomes({
        @Outcome(id = "[0, 0]", expect = ACCEPTABLE, desc = "Object not constructed yet"),
        @Outcome(id = "[1, 0]", expect = ACCEPTABLE, desc = "Object half-way"),
        @Outcome(id = "[1, 2]", expect = ACCEPTABLE, desc = "Object fully constructed"),
        @Outcome(expect = ACCEPTABLE_INTERESTING, desc = "Reordered"),
})
@State()
public class TwoFieldTest {

    private pl.erizo.gradle.jcstress.reordering.TwoFieldClass twoFieldClass = new pl.erizo.gradle.jcstress.reordering.TwoFieldClass(0, 0);

    private pl.erizo.gradle.jcstress.reordering.TwoFieldSecondClass twoFieldSecondClass;

    @Actor
    public void actor1() {
        twoFieldClass.setX(1);
        twoFieldClass.setY(2);
    }

    @Actor
    public void actor2(LL_Result longResult) {
        longResult.r1 = twoFieldClass.x;
        longResult.r2 = twoFieldClass.y;
    }

}
