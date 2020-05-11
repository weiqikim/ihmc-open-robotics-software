package us.ihmc.humanoidBehaviors.tools.perception;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.robotEnvironmentAwareness.planarRegion.slam.PlanarRegionSLAM;
import us.ihmc.robotEnvironmentAwareness.planarRegion.slam.PlanarRegionSLAMParameters;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.Ros2NodeInterface;
import us.ihmc.tools.thread.PausablePeriodicThread;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Assembles and publishes currently visible planar regions on the REA output topic.
 */
public class CompositePlanarRegionService
{
   private Supplier<PlanarRegionsList>[] planarRegionSuppliers;
   private final List<IHMCROS2Publisher<PlanarRegionsListMessage>> planarRegionPublishers = new ArrayList<>();
   private final IHMCROS2Publisher<PlanarRegionsListMessage> combinedPlanarRegionPublisher;
   private final PausablePeriodicThread thread;

   private PlanarRegionSLAMParameters planarRegionSLAMParameters = new PlanarRegionSLAMParameters();

   public CompositePlanarRegionService(Ros2NodeInterface ros2Node, List<String> names, Supplier<PlanarRegionsList>... planarRegionSuppliers)
   {
      this.planarRegionSuppliers = planarRegionSuppliers;

      for (int i = 0; i < planarRegionSuppliers.length; i++)
      {
         MessageTopicNameGenerator topicGenerator = ROS2Tools.getTopicNameGenerator(null,
                                                                                    ROS2Tools.REA_MODULE + names.get(i),
                                                                                    ROS2Tools.ROS2TopicQualifier.OUTPUT);
         planarRegionPublishers.add(ROS2Tools.createPublisher(ros2Node, PlanarRegionsListMessage.class, topicGenerator));
      }

      MessageTopicNameGenerator topicGenerator = ROS2Tools.getTopicNameGenerator(null,
                                                                                 ROS2Tools.REA_MODULE,
                                                                                 ROS2Tools.ROS2TopicQualifier.OUTPUT);
      combinedPlanarRegionPublisher = ROS2Tools.createPublisher(ros2Node, PlanarRegionsListMessage.class, topicGenerator);
      thread = new PausablePeriodicThread(getClass().getSimpleName(), 0.5, this::process);
   }

   public void start()
   {
      thread.start();
   }

   public void stop()
   {
      thread.stop();
   }

   private void process()
   {
      if (planarRegionSuppliers.length <= 0)
      {
         return;
      }

      PlanarRegionsListMessage message;

      PlanarRegionsList planarRegionsListToPublish = planarRegionSuppliers[0].get();
      message = PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(planarRegionsListToPublish);
      planarRegionPublishers.get(0).publish(message);

      for (int i = 1; i < planarRegionSuppliers.length; i++)
      {
         PlanarRegionsList planarRegions = planarRegionSuppliers[i].get();
         message = PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(planarRegions);
         planarRegionPublishers.get(i).publish(message);

         planarRegionsListToPublish = PlanarRegionSLAM.generateMergedMapByMergingAllPlanarRegionsMatches(planarRegionsListToPublish,
                                                                                                         planarRegions,
                                                                                                         planarRegionSLAMParameters,
                                                                                                         null);
      }

      message = PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(planarRegionsListToPublish);
      combinedPlanarRegionPublisher.publish(message);
   }
}
