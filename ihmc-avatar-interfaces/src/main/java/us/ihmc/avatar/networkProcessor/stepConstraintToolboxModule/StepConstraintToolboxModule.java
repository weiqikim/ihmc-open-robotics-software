package us.ihmc.avatar.networkProcessor.stepConstraintToolboxModule;

import controller_msgs.msg.dds.*;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.networkProcessor.modules.ToolboxController;
import us.ihmc.avatar.networkProcessor.modules.ToolboxModule;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.communication.IHMCRealtimeROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.communication.ROS2Tools.ROS2TopicQualifier;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotDataLogger.util.JVMStatisticsGenerator;
import us.ihmc.robotEnvironmentAwareness.communication.REACommunicationProperties;
import us.ihmc.ros2.RealtimeRos2Node;

import java.util.ArrayList;
import java.util.List;

public class StepConstraintToolboxModule extends ToolboxModule
{
   private static final int DEFAULT_UPDATE_PERIOD_MILLISECONDS = 5;

   protected final StepConstraintToolboxController controller;
   private IHMCRealtimeROS2Publisher<StepConstraintMessage> constraintRegionPublisher;

   public StepConstraintToolboxModule(DRCRobotModel robotModel, boolean startYoVariableServer, PubSubImplementation pubSubImplementation, double gravityZ)
   {
      super(robotModel.getSimpleRobotName(),
            robotModel.createFullRobotModel(),
            robotModel.getLogModelProvider(),
            startYoVariableServer,
            DEFAULT_UPDATE_PERIOD_MILLISECONDS,
            pubSubImplementation);

      setTimeWithoutInputsBeforeGoingToSleep(3.0);
      controller = new StepConstraintToolboxController(statusOutputManager,
                                                       constraintRegionPublisher, robotModel.getWalkingControllerParameters(), fullRobotModel, gravityZ, registry);

      setTimeWithoutInputsBeforeGoingToSleep(Double.POSITIVE_INFINITY);
      startYoVariableServer();
      if (yoVariableServer != null)
      {
         JVMStatisticsGenerator jvmStatisticsGenerator = new JVMStatisticsGenerator(yoVariableServer);
         jvmStatisticsGenerator.start();
      }
   }

   @Override
   public void registerExtraPuSubs(RealtimeRos2Node realtimeRos2Node)
   {
      MessageTopicNameGenerator controllerPubGenerator = ControllerAPIDefinition.getPublisherTopicNameGenerator(robotName);

      ROS2Tools.createCallbackSubscription(realtimeRos2Node, RobotConfigurationData.class, controllerPubGenerator, s ->
      {
         if (controller != null)
         {
            controller.updateRobotConfigurationData(s.takeNextData());
         }
      });

      ROS2Tools.createCallbackSubscription(realtimeRos2Node, CapturabilityBasedStatus.class, controllerPubGenerator, s ->
      {
         if (controller != null)
         {
            controller.updateCapturabilityBasedStatus(s.takeNextData());
         }
      });

      ROS2Tools.createCallbackSubscription(realtimeRos2Node, FootstepStatusMessage.class, controllerPubGenerator, s ->
      {
         if (controller != null)
         {
            controller.updateFootstepStatus(s.takeNextData());
         }
      });

      ROS2Tools.createCallbackSubscription(realtimeRos2Node,
                                           PlanarRegionsListMessage.class,
                                           REACommunicationProperties.publisherTopicNameGenerator,
                                           s -> updatePlanarRegion(s.takeNextData()));

      constraintRegionPublisher = ROS2Tools.createPublisher(realtimeRos2Node, StepConstraintMessage.class, ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotName));
   }

   public void setSwitchPlanarRegionConstraintsAutomatically(boolean switchAutomatically)
   {
      controller.setSwitchPlanarRegionConstraintsAutomatically(switchAutomatically);
   }

   public void updatePlanarRegion(PlanarRegionsListMessage planarRegionsListMessage)
   {
      if (controller != null)
      {
         controller.updatePlanarRegions(planarRegionsListMessage);
      }
   }

   @Override
   public ToolboxController getToolboxController()
   {
      return controller;
   }

   @Override
   public List<Class<? extends Command<?, ?>>> createListOfSupportedCommands()
   {
      return new ArrayList<>();
   }

   @Override
   public List<Class<? extends Settable<?>>> createListOfSupportedStatus()
   {
      return supportedStatus();
   }

   public static List<Class<? extends Settable<?>>> supportedStatus()
   {
      List<Class<? extends Settable<?>>> status = new ArrayList<>();
      status.add(PlanarRegionsListMessage.class);
      status.add(PlanarRegionMessage.class);
      return status;
   }

   @Override
   public MessageTopicNameGenerator getPublisherTopicNameGenerator()
   {
      return getPublisherTopicNameGenerator(robotName);
   }

   public static MessageTopicNameGenerator getPublisherTopicNameGenerator(String robotName)
   {
      return ROS2Tools.getTopicNameGenerator(robotName, ROS2Tools.STEP_CONSTRAINT_TOOLBOX, ROS2TopicQualifier.OUTPUT);
   }

   @Override
   public MessageTopicNameGenerator getSubscriberTopicNameGenerator()
   {
      return getSubscriberTopicNameGenerator(robotName);
   }

   public static MessageTopicNameGenerator getSubscriberTopicNameGenerator(String robotName)
   {
      return ROS2Tools.getTopicNameGenerator(robotName, ROS2Tools.STEP_CONSTRAINT_TOOLBOX, ROS2TopicQualifier.INPUT);
   }
}
