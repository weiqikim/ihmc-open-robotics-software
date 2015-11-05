package us.ihmc.darpaRoboticsChallenge.networkProcessor.modules.uiConnector;

import java.util.HashMap;

import us.ihmc.communication.packets.ControllerCrashNotificationPacket;
import us.ihmc.humanoidRobotics.communication.packets.DetectedObjectPacket;
import us.ihmc.communication.packets.InvalidPacketNotificationPacket;
import us.ihmc.humanoidRobotics.communication.packets.bdi.BDIBehaviorStatusPacket;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.BehaviorControlModeResponsePacket;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.script.ScriptBehaviorStatusPacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ControlStatusPacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandJointAnglePacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandPosePacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandPoseStatus;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandRotateAboutAxisPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.BlackFlyParameterPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.DepthDataClearCommand;
import us.ihmc.humanoidRobotics.communication.packets.sensing.DrillDetectionPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.FisheyePacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.HeadPosePacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.LocalizationPointMapPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.MultisenseMocapExperimentPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.PelvisPoseErrorPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.PointCloudWorldPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.RawIMUPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.VideoPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.CapturabilityBasedStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.ChestOrientationPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.ComHeightPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataList;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.HeadOrientationPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.ManipulationAbortedStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisPosePacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.SnapFootstepPacket;
import us.ihmc.humanoidRobotics.communication.packets.wholebody.JointAnglesPacket;
import us.ihmc.humanoidRobotics.communication.packets.wholebody.WholeBodyTrajectoryPacket;
import us.ihmc.sensorProcessing.communication.packets.dataobjects.RobotConfigurationData;

public class PacketsForwardedToTheUi
{
   public static final long UI_JOINT_CONFIGURATION_UPDATE_MILLIS = 100l;
   public static final long UI_WRIST_FEET_SENSORS_UPDATE_MILLIS = 500l;
   public static final long UI_MULTISENSE_IMU_CHECK_MILLIS=5000l;
   
   public static Class<?>[] PACKETS_ALLOWED_TO_BE_SENT_TO_THE_USER_INTERFACE = {
      FootstepStatus.class,                          
      ScriptBehaviorStatusPacket.class,
      PelvisPoseErrorPacket.class,                   
      BehaviorControlModeResponsePacket.class,
      BDIBehaviorStatusPacket.class,                 
      ControlStatusPacket.class,                     
      FootstepDataList.class,                        
      ComHeightPacket.class,                         
      HeadOrientationPacket.class,                   
      PelvisPosePacket.class,                        
      ChestOrientationPacket.class,                  
      SnapFootstepPacket.class,
      VideoPacket.class,
      HandPosePacket.class,
      HandRotateAboutAxisPacket.class,
      DepthDataClearCommand.class,
      PointCloudWorldPacket.class,
      HandJointAnglePacket.class,
      WholeBodyTrajectoryPacket.class,
      JointAnglesPacket.class,
      ControllerCrashNotificationPacket.class,
      InvalidPacketNotificationPacket.class,
      DetectedObjectPacket.class,
      MultisenseMocapExperimentPacket.class,
      FisheyePacket.class,
      HandPoseStatus.class,
      LocalizationPointMapPacket.class,
      BlackFlyParameterPacket.class,
      DrillDetectionPacket.class,
      ManipulationAbortedStatus.class
   };
   
   public static final HashMap<Class<?>, Long> PACKETS_ALLOWED_TO_BE_SENT_TO_THE_USER_INTERFACE_WITH_MINIMAL_INTERVALS = new HashMap<Class<?>, Long>();
   static {
//      PACKETS_ALLOWED_TO_BE_SENT_TO_THE_USER_INTERFACE_WITH_MINIMAL_INTERVALS.put(RobotPoseData.class, UI_JOINT_CONFIGURATION_UPDATE_MILLIS);
      PACKETS_ALLOWED_TO_BE_SENT_TO_THE_USER_INTERFACE_WITH_MINIMAL_INTERVALS.put(CapturabilityBasedStatus.class, UI_JOINT_CONFIGURATION_UPDATE_MILLIS);
      PACKETS_ALLOWED_TO_BE_SENT_TO_THE_USER_INTERFACE_WITH_MINIMAL_INTERVALS.put(RobotConfigurationData.class, UI_JOINT_CONFIGURATION_UPDATE_MILLIS);
      PACKETS_ALLOWED_TO_BE_SENT_TO_THE_USER_INTERFACE_WITH_MINIMAL_INTERVALS.put(HeadPosePacket.class, UI_MULTISENSE_IMU_CHECK_MILLIS);
      PACKETS_ALLOWED_TO_BE_SENT_TO_THE_USER_INTERFACE_WITH_MINIMAL_INTERVALS.put(RawIMUPacket.class, UI_MULTISENSE_IMU_CHECK_MILLIS);
   }
   
   
}
