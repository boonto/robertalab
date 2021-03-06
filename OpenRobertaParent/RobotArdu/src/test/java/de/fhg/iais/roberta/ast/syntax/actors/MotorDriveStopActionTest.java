package de.fhg.iais.roberta.ast.syntax.actors;

import org.junit.Test;

import de.fhg.iais.roberta.util.test.ardu.HelperBotNrollForXmlTest;

public class MotorDriveStopActionTest {
    private final HelperBotNrollForXmlTest h = new HelperBotNrollForXmlTest();

    @Test
    public void stop() throws Exception {
        final String a = "\none.stop();";

        this.h.assertCodeIsOk(a, "/ast/actions/action_Stop.xml", false);
    }
}