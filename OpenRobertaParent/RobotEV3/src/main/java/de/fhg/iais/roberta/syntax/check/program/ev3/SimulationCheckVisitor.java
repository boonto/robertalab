package de.fhg.iais.roberta.syntax.check.program.ev3;

import de.fhg.iais.roberta.components.Configuration;
import de.fhg.iais.roberta.syntax.action.light.LedAction;
import de.fhg.iais.roberta.syntax.check.program.RobotSimulationCheckVisitor;
import de.fhg.iais.roberta.syntax.sensor.generic.AugmentedRealitySensor;
import de.fhg.iais.roberta.syntax.sensor.generic.CompassSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.IRSeekerSensor;
import de.fhg.iais.roberta.typecheck.NepoInfo;

public class SimulationCheckVisitor extends RobotSimulationCheckVisitor {

    public SimulationCheckVisitor(Configuration brickConfiguration) {
        super(brickConfiguration);
    }

    @Override
    public Void visitIRSeekerSensor(IRSeekerSensor<Void> irSeekerSensor) {
        irSeekerSensor.addInfo(NepoInfo.warning("SIM_BLOCK_NOT_SUPPORTED"));
        return null;
    }

    @Override
    public Void visitCompassSensor(CompassSensor<Void> compassSensor) {
        super.visitCompassSensor(compassSensor);
        compassSensor.addInfo(NepoInfo.warning("SIM_BLOCK_NOT_SUPPORTED"));
        return null;
    }

    @Override
    public Void visitLedAction(LedAction<Void> ledAction) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitAugmentedRealitySensor(AugmentedRealitySensor<Void> augmentedRealitySensor) {
        super.visitAugmentedRealitySensor(augmentedRealitySensor);
        augmentedRealitySensor.addInfo(NepoInfo.warning("SIM_BLOCK_NOT_SUPPORTED"));
        return null;
    }
}
