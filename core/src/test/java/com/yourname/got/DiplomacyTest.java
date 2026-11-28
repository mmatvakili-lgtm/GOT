package com.yourname.got;

import com.yourname.got.logic.system.Diplomacy;
import com.yourname.got.support.Check;
import org.junit.jupiter.api.Test;

/** Pacts block attacks for the right time, proposals expire, either side can cancel. */
class DiplomacyTest {
    @Test
    void peaceAllianceAndExpiry() {
        Diplomacy d = new Diplomacy();
        Check.that(d.propose(1, 2, Diplomacy.Type.PEACE, 5) == null, "offer is accepted for sending");
        Check.that(d.propose(2, 1, Diplomacy.Type.PEACE, 5) != null, "only one pending offer per pair");
        Check.that(d.pactBetween(1, 2, 5) == null, "no pact before the answer");
        Check.that(d.respond(1, 2, true, 6) == null, "answer works");
        Diplomacy.Pact p = d.pactBetween(2, 1, 6);
        Check.that(p != null && p.type == Diplomacy.Type.PEACE, "pact exists in both directions");
        Check.that(p.endDay > 6, "peace has an end");
        Check.that(d.pactBetween(1, 2, p.endDay + 1) == null, "peace ends by itself");
        d.prune(p.endDay + 1);
        Check.that(d.pactsOf(1, p.endDay + 1).isEmpty(), "expired pact is dropped");

        Check.that(d.propose(1, 3, Diplomacy.Type.ALLIANCE, 10) == null, "alliance offer");
        Check.that(d.respond(1, 3, true, 11) == null, "alliance accepted");
        Check.that(d.pactBetween(3, 1, 500) != null, "alliance has no end");
        Check.that(d.propose(3, 1, Diplomacy.Type.PEACE, 12) != null, "weaker pact is not offered on top");
        Check.that(d.cancel(3, 1, 12), "either side can cancel");
        Check.that(d.pactBetween(1, 3, 12) == null, "cancelled");

        Check.that(d.propose(4, 1, Diplomacy.Type.PEACE, 20) == null, "offer");
        Check.that(d.incoming(1, 21).size() == 1, "visible to the receiver");
        Check.that(d.respond(4, 1, true, 30) != null, "an old offer can no longer be accepted");
        Check.that(d.respond(4, 2, true, 21) != null, "unknown offer is refused");
        Check.assertAll();
    }
}
