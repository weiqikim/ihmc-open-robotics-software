package us.ihmc.humanoidRobotics.communication.controllerAPI.command;

import controller_msgs.msg.dds.WholeBodyPoseTrajectoryMessage;
import gnu.trove.list.array.TDoubleArrayList;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import us.ihmc.euclid.tools.EuclidCoreTools;

public class WholeBodyPoseTrajectoryCommand implements Command<WholeBodyPoseTrajectoryCommand, WholeBodyPoseTrajectoryMessage>, EpsilonComparable<WholeBodyPoseTrajectoryCommand>
{
   private long sequenceId;
   private double trajectoryDuration;
   private final TDoubleArrayList jointAngles = new TDoubleArrayList(50);
   private final Pose3D pelvisPose = new Pose3D();

   @Override
   public void clear()
   {
      sequenceId = 0;
      trajectoryDuration = Double.NaN;
      jointAngles.reset();
      pelvisPose.setToNaN();
   }

   @Override
   public void setFromMessage(WholeBodyPoseTrajectoryMessage message)
   {
      sequenceId = message.getSequenceId();
      trajectoryDuration = message.getTrajectoryDuration();
      pelvisPose.set(message.getPelvisPose());

      for (int i = 0; i < message.getJointAngles().size(); i++)
      {
         jointAngles.add(message.getJointAngles().get(i));
      }
   }

   @Override
   public Class<WholeBodyPoseTrajectoryMessage> getMessageClass()
   {
      return WholeBodyPoseTrajectoryMessage.class;
   }

   @Override
   public boolean isCommandValid()
   {
      return !Double.isNaN(trajectoryDuration) && !jointAngles.isEmpty() && !pelvisPose.containsNaN();
   }

   @Override
   public long getSequenceId()
   {
      return sequenceId;
   }

   @Override
   public boolean epsilonEquals(WholeBodyPoseTrajectoryCommand other, double epsilon)
   {
      if (!EuclidCoreTools.epsilonEquals(trajectoryDuration, other.trajectoryDuration, epsilon))
      {
         return false;
      }
      if (!pelvisPose.epsilonEquals(other.pelvisPose, epsilon))
      {
         return false;
      }
      if (jointAngles.size() != other.jointAngles.size())
      {
         return false;
      }

      for (int i = 0; i < jointAngles.size(); i++)
      {
         if (!EuclidCoreTools.epsilonEquals(jointAngles.get(i), other.jointAngles.get(i), epsilon))
         {
            return false;
         }
      }

      return false;
   }

   @Override
   public void set(WholeBodyPoseTrajectoryCommand other)
   {
      this.sequenceId = other.sequenceId;
      this.trajectoryDuration = other.trajectoryDuration;
      this.pelvisPose.set(other.pelvisPose);

      this.jointAngles.reset();
      for (int i = 0; i < other.jointAngles.size(); i++)
      {
         this.jointAngles.add(other.jointAngles.get(i));
      }
   }
}
