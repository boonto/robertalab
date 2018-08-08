package de.fhg.iais.roberta.syntax.sensor.generic;

import de.fhg.iais.roberta.blockly.generated.Block;
import de.fhg.iais.roberta.mode.sensor.AugmentedRealitySensorMode;
import de.fhg.iais.roberta.mode.sensor.SensorPort;
import de.fhg.iais.roberta.syntax.BlockTypeContainer;
import de.fhg.iais.roberta.syntax.BlocklyBlockProperties;
import de.fhg.iais.roberta.syntax.BlocklyComment;
import de.fhg.iais.roberta.syntax.Phrase;
import de.fhg.iais.roberta.syntax.sensor.ExternalSensor;
import de.fhg.iais.roberta.syntax.sensor.SensorMetaDataBean;
import de.fhg.iais.roberta.transformer.Jaxb2AstTransformer;
import de.fhg.iais.roberta.visitor.AstVisitor;
import de.fhg.iais.roberta.visitor.sensor.AstSensorsVisitor;

/**
 * This class represents the <b>robSensors_augmentedReality_getMode</b>, <b>robSensors_augmentedReality_getSample</b> and <b>robSensors_augmentedReality_setMode</b> blocks from
 * Blockly into the AST (abstract syntax tree). Object from this class will generate code for setting the mode of the sensor or getting a sample from the
 * sensor.<br/>
 * <br>
 * The client must provide the {@link SensorPort} and {@link AugmentedRealitySensorMode}. See enum {@link AugmentedRealitySensorMode} for all possible modes of the sensor.
 * <br>
 * <br>
 * To create an instance from this class use the method {@link #make(AugmentedRealitySensorMode, SensorPort, BlocklyBlockProperties, BlocklyComment)}.<br>
 */
public class AugmentedRealitySensor<V> extends ExternalSensor<V> {
    private AugmentedRealitySensor(SensorMetaDataBean sensorMetaDataBean, BlocklyBlockProperties properties, BlocklyComment comment) {
        super(sensorMetaDataBean, BlockTypeContainer.getByName("AUGMENTEDREALITY_SENSING"), properties, comment);
        setReadOnly();
    }

    /**
     * Create object of the class {@link AugmentedRealitySensor}.
     *
     * @param mode in which the sensor is operating; must be <b>not</b> null; see enum {@link AugmentedRealitySensorMode} for all possible modes that the sensor have,
     * @param port on where the sensor is connected; must be <b>not</b> null; see enum {@link SensorPort} for all possible sensor ports,
     * @param properties of the block (see {@link BlocklyBlockProperties}),
     * @param comment added from the user,
     * @return read only object of {@link AugmentedRealitySensor}
     */
    public static <V> AugmentedRealitySensor<V> make(SensorMetaDataBean sensorMetaDataBean, BlocklyBlockProperties properties, BlocklyComment comment) {
        return new AugmentedRealitySensor<V>(sensorMetaDataBean, properties, comment);
    }

    @Override
    protected V accept(AstVisitor<V> visitor) {
        return ((AstSensorsVisitor<V>) visitor).visitAugmentedRealitySensor(this);
    }

    /**
     * Transformation from JAXB object to corresponding AST object.
     *
     * @param block for transformation
     * @param helper class for making the transformation
     * @return corresponding AST object
     */
    public static <V> Phrase<V> jaxbToAst(Block block, Jaxb2AstTransformer<V> helper) {
        SensorMetaDataBean sensorData = extractSensorPortAndMode(block, helper, helper.getModeFactory()::getAugmentedRealitySensorMode);
        return AugmentedRealitySensor.make(sensorData, helper.extractBlockProperties(block), helper.extractComment(block));
    }

}
