package de.fhg.iais.roberta.javaServer.restServices.all;

import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.fhg.iais.roberta.blockly.generated.BlockSet;
import de.fhg.iais.roberta.blockly.generated.Instance;
import de.fhg.iais.roberta.components.ev3.Ev3Configuration;
import de.fhg.iais.roberta.javaServer.provider.OraData;
import de.fhg.iais.roberta.persistence.AccessRightProcessor;
import de.fhg.iais.roberta.persistence.ProgramProcessor;
import de.fhg.iais.roberta.persistence.UserProcessor;
import de.fhg.iais.roberta.persistence.bo.Program;
import de.fhg.iais.roberta.persistence.bo.Robot;
import de.fhg.iais.roberta.persistence.bo.User;
import de.fhg.iais.roberta.persistence.dao.RobotDao;
import de.fhg.iais.roberta.persistence.util.DbSession;
import de.fhg.iais.roberta.persistence.util.HttpSessionState;
import de.fhg.iais.roberta.persistence.util.SessionFactoryWrapper;
import de.fhg.iais.roberta.robotCommunication.ev3.Ev3Communicator;
import de.fhg.iais.roberta.robotCommunication.ev3.Ev3CompilerWorkflow;
import de.fhg.iais.roberta.syntax.BlockType;
import de.fhg.iais.roberta.syntax.Phrase;
import de.fhg.iais.roberta.syntax.blocksequence.Location;
import de.fhg.iais.roberta.syntax.codegen.ev3.Ast2Ev3JavaScriptVisitor;
import de.fhg.iais.roberta.syntax.hardwarecheck.ev3.UsedPortsCheckVisitor;
import de.fhg.iais.roberta.transformer.Jaxb2BlocklyProgramTransformer;
import de.fhg.iais.roberta.util.AliveData;
import de.fhg.iais.roberta.util.ClientLogger;
import de.fhg.iais.roberta.util.Key;
import de.fhg.iais.roberta.util.Util;

@Path("/program")
public class ClientProgram {
    private static final Logger LOG = LoggerFactory.getLogger(ClientProgram.class);

    private final SessionFactoryWrapper sessionFactoryWrapper;
    private final Ev3Communicator brickCommunicator;
    private final Ev3CompilerWorkflow compilerWorkflow;

    @Inject
    public ClientProgram(SessionFactoryWrapper sessionFactoryWrapper, Ev3Communicator brickCommunicator, Ev3CompilerWorkflow compilerWorkflow) {
        this.sessionFactoryWrapper = sessionFactoryWrapper;
        this.brickCommunicator = brickCommunicator;
        this.compilerWorkflow = compilerWorkflow;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response command(@OraData HttpSessionState httpSessionState, JSONObject fullRequest) throws Exception {
        AliveData.rememberClientCall();
        new ClientLogger().log(LOG, fullRequest);
        final int userId = httpSessionState.getUserId();
        final int robotId = httpSessionState.getRobotId();
        JSONObject response = new JSONObject();
        DbSession dbSession = this.sessionFactoryWrapper.getSession();
        try {
            JSONObject request = fullRequest.getJSONObject("data");
            String cmd = request.getString("cmd");
            LOG.info("command is: " + cmd + ", userId is " + userId);
            response.put("cmd", cmd);
            ProgramProcessor pp = new ProgramProcessor(dbSession, httpSessionState);
            AccessRightProcessor upp = new AccessRightProcessor(dbSession, httpSessionState);
            UserProcessor up = new UserProcessor(dbSession, httpSessionState);

            if ( cmd.equals("saveP") ) {
                String programName = request.getString("name");
                String programText = request.getString("program");
                Long timestamp = request.getLong("timestamp");
                Timestamp programTimestamp = new Timestamp(timestamp);
                boolean isShared = request.optBoolean("shared", false);
                pp.updateProgram(programName, userId, robotId, programText, programTimestamp, true, !isShared);
                if ( pp.isOk() ) {
                    Program program = pp.getProgram(programName, userId, robotId);
                    if ( program != null ) {
                        response.put("newProgramTimestamp", program.getLastChanged());
                    }
                }
                Util.addResultInfo(response, pp);

            } else if ( cmd.equals("saveAsP") ) {
                String programName = request.getString("name");
                String programText = request.getString("program");
                Long timestamp = request.getLong("timestamp");
                Timestamp programTimestamp = new Timestamp(timestamp);
                pp.updateProgram(programName, userId, robotId, programText, programTimestamp, true, true);
                if ( pp.isOk() ) {
                    Program program = pp.getProgram(programName, userId, robotId);
                    if ( program != null ) {
                        response.put("newProgramTimestamp", program.getLastChanged());
                    }
                }
                Util.addResultInfo(response, pp);

            } else if ( cmd.equals("loadP") && httpSessionState.isUserLoggedIn() ) {
                String programName = request.getString("name");
                String ownerName = request.getString("owner");
                User owner = up.getUser(ownerName);
                int ownerID = owner.getId();
                Program program = pp.getProgram(programName, ownerID, robotId);
                if ( program != null ) {
                    response.put("data", program.getProgramText());
                }
                Util.addResultInfo(response, pp);
            } else if ( cmd.equals("checkP") ) {
                String programText = request.optString("programText");
                String configurationText = request.optString("configurationText");

                Jaxb2BlocklyProgramTransformer<Void> programTransformer = null;
                try {
                    programTransformer = Ev3CompilerWorkflow.generateProgramTransformer(programText);
                } catch ( Exception e ) {
                    LOG.error("Transformer failed", e);
                    //return Key.COMPILERWORKFLOW_ERROR_PROGRAM_TRANSFORM_FAILED;
                }
                Ev3Configuration brickConfiguration = null;
                try {
                    brickConfiguration = Ev3CompilerWorkflow.generateConfiguration(configurationText);
                } catch ( Exception e ) {
                    LOG.error("Generation of the configuration failed", e);
                    //return Key.COMPILERWORKFLOW_ERROR_CONFIGURATION_TRANSFORM_FAILED;
                }

                UsedPortsCheckVisitor programChecker = new UsedPortsCheckVisitor(brickConfiguration);
                int errorCounter = programChecker.check(programTransformer.getTree());
                response.put("data", jaxbToXml(astToJaxb(programChecker.getCheckedProgram())));
                response.put("errorCounter", errorCounter);
                Util.addSuccessInfo(response, Key.ROBOT_PUSH_RUN);

            } else if ( cmd.equals("shareP") && httpSessionState.isUserLoggedIn() ) {
                String programName = request.getString("programName");
                String userToShareName = request.getString("userToShare");
                String right = request.getString("right");
                upp.shareToUser(userId, robotId, userToShareName, programName, right);
                Util.addResultInfo(response, upp);

            } else if ( cmd.equals("deleteP") && httpSessionState.isUserLoggedIn() ) {
                String programName = request.getString("name");
                pp.deleteByName(programName, userId, robotId);
                Util.addResultInfo(response, pp);

            } else if ( cmd.equals("loadPN") && httpSessionState.isUserLoggedIn() ) {
                JSONArray programInfo = pp.getProgramInfo(userId, robotId);
                response.put("programNames", programInfo);
                Util.addResultInfo(response, pp);

            } else if ( cmd.equals("loadPR") && httpSessionState.isUserLoggedIn() ) {
                String programName = request.getString("name");
                JSONArray relations = pp.getProgramRelations(programName, userId, robotId);
                response.put("relations", relations);
                Util.addResultInfo(response, pp);

            } else if ( cmd.equals("runP") ) {
                // TODO
                RobotDao robotDao = new RobotDao(dbSession);
                Robot robot = robotDao.get(robotId);
                if ( robot.getName().equals("ev3") ) {
                    String token = httpSessionState.getToken();
                    String programName = request.getString("name");
                    String programText = request.optString("programText");
                    String configurationText = request.optString("configurationText");
                    LOG.info("compiler workflow started for program {}", programName);
                    Key messageKey = this.compilerWorkflow.execute(dbSession, token, programName, programText, configurationText);
                    if ( messageKey == null ) {
                        // everything is fine
                        if ( token == null ) {
                            Util.addErrorInfo(response, Key.ROBOT_NOT_CONNECTED);
                        } else {
                            boolean wasRobotWaiting = this.brickCommunicator.theRunButtonWasPressed(token, programName);
                            if ( wasRobotWaiting ) {
                                Util.addSuccessInfo(response, Key.ROBOT_PUSH_RUN);
                            } else {
                                Util.addErrorInfo(response, Key.ROBOT_NOT_WAITING);
                            }
                        }
                    } else {
                        Util.addErrorInfo(response, messageKey);
                    }
                } else if ( robot.getName().equals("oraSim") ) {
                    String programName = request.getString("name");
                    String programText = request.optString("programText");
                    String configurationText = request.optString("configurationText");
                    LOG.info("JavaScript code generation started for program {}", programName);

                    if ( programText == null || programText.trim().equals("") ) {
                        //return Key.COMPILERWORKFLOW_ERROR_PROGRAM_NOT_FOUND;
                    } else if ( configurationText == null || configurationText.trim().equals("") ) {
                        //return Key.COMPILERWORKFLOW_ERROR_CONFIGURATION_NOT_FOUND;
                    }

                    Jaxb2BlocklyProgramTransformer<Void> programTransformer = null;
                    try {
                        programTransformer = Ev3CompilerWorkflow.generateProgramTransformer(programText);
                    } catch ( Exception e ) {
                        LOG.error("Transformer failed", e);
                        //return Key.COMPILERWORKFLOW_ERROR_PROGRAM_TRANSFORM_FAILED;
                    }

                    String javaScriptCode = Ast2Ev3JavaScriptVisitor.generate(programTransformer.getTree());

                    LOG.info("JavaScriptCode \n{}", javaScriptCode);
                    response.put("javaScriptProgram", javaScriptCode);
                    Util.addSuccessInfo(response, Key.ROBOT_PUSH_RUN);
                } else {
                    Util.addSuccessInfo(response, Key.ROBOT_PUSH_RUN);
                }
            } else {
                LOG.error("Invalid command: " + cmd);
                Util.addErrorInfo(response, Key.COMMAND_INVALID);
            }
            dbSession.commit();
        } catch ( Exception e ) {
            dbSession.rollback();
            String errorTicketId = Util.getErrorTicketId();
            LOG.error("Exception. Error ticket: " + errorTicketId, e);
            Util.addErrorInfo(response, Key.SERVER_ERROR).append("parameters", errorTicketId);
        } finally {
            if ( dbSession != null ) {
                dbSession.close();
            }
        }
        Util.addFrontendInfo(response, httpSessionState, this.brickCommunicator);
        return Response.ok(response).build();
    }

    public static String jaxbToXml(BlockSet blockSet) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(BlockSet.class);
        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter writer = new StringWriter();
        m.marshal(blockSet, writer);
        return writer.toString();
    }

    public static BlockSet astToJaxb(ArrayList<ArrayList<Phrase<Void>>> astProgram) {
        BlockSet blockSet = new BlockSet();

        Instance instance = null;
        for ( ArrayList<Phrase<Void>> tree : astProgram ) {
            for ( Phrase<Void> phrase : tree ) {
                if ( phrase.getKind() == BlockType.LOCATION ) {
                    blockSet.getInstance().add(instance);
                    instance = new Instance();
                    instance.setX(((Location<Void>) phrase).getX());
                    instance.setY(((Location<Void>) phrase).getY());
                }
                instance.getBlock().add(phrase.astToBlock());
            }
        }
        blockSet.getInstance().add(instance);
        return blockSet;
    }
}