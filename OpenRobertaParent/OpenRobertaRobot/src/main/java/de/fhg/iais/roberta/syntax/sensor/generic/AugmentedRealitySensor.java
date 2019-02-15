package de.fhg.iais.roberta.syntax.sensor.generic;

import de.fhg.iais.roberta.blockly.generated.Block;
import de.fhg.iais.roberta.syntax.*;
import de.fhg.iais.roberta.syntax.sensor.ExternalSensor;
import de.fhg.iais.roberta.syntax.sensor.SensorMetaDataBean;
import de.fhg.iais.roberta.transformer.AbstractJaxb2Ast;
import de.fhg.iais.roberta.visitor.IVisitor;
import de.fhg.iais.roberta.visitor.hardware.sensor.ISensorVisitor;

public final class AugmentedRealitySensor<V> extends ExternalSensor<V> {

    private AugmentedRealitySensor(SensorMetaDataBean sensorMetaDataBean, BlocklyBlockProperties properties, BlocklyComment comment) {
        super(sensorMetaDataBean, BlockTypeContainer.getByName("AUGMENTEDREALITY_SENSING"), properties, comment);
        setReadOnly();
    }

    public static <V> AugmentedRealitySensor<V> make(SensorMetaDataBean sensorMetaDataBean, BlocklyBlockProperties properties, BlocklyComment comment) {
        return new AugmentedRealitySensor<V>(sensorMetaDataBean, properties, comment);
    }

    @Override
    protected V accept(IVisitor<V> visitor) {
        return ((ISensorVisitor<V>) visitor).visitAugmentedRealitySensor(this);
    }

    /**
     * Transformation from JAXB object to corresponding AST object.
     *
     * @param block for transformation
     * @param helper class for making the transformation
     * @return corresponding AST object
     */
    public static <V> Phrase<V> jaxbToAst(Block block, AbstractJaxb2Ast<V> helper) {
        SensorMetaDataBean sensorData = extractPortAndModeAndSlot(block, helper);
        return AugmentedRealitySensor.make(sensorData, helper.extractBlockProperties(block), helper.extractComment(block));
    }

}
