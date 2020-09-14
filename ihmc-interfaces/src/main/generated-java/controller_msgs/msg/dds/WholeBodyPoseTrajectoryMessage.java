package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

/**
       * This message is part of the IHMC whole-body controller API.
       * Experimental mode of specifying a desired configuration in terms of root pose and joint angles.
       */
public class WholeBodyPoseTrajectoryMessage extends Packet<WholeBodyPoseTrajectoryMessage> implements Settable<WholeBodyPoseTrajectoryMessage>, EpsilonComparable<WholeBodyPoseTrajectoryMessage>
{
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long sequence_id_;
   public double trajectory_duration_;
   public us.ihmc.idl.IDLSequence.Double  joint_angles_;
   public us.ihmc.euclid.geometry.Pose3D pelvis_pose_;

   public WholeBodyPoseTrajectoryMessage()
   {
      joint_angles_ = new us.ihmc.idl.IDLSequence.Double (50, "type_6");

      pelvis_pose_ = new us.ihmc.euclid.geometry.Pose3D();
   }

   public WholeBodyPoseTrajectoryMessage(WholeBodyPoseTrajectoryMessage other)
   {
      this();
      set(other);
   }

   public void set(WholeBodyPoseTrajectoryMessage other)
   {
      sequence_id_ = other.sequence_id_;

      trajectory_duration_ = other.trajectory_duration_;

      joint_angles_.set(other.joint_angles_);
      geometry_msgs.msg.dds.PosePubSubType.staticCopy(other.pelvis_pose_, pelvis_pose_);
   }

   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public void setSequenceId(long sequence_id)
   {
      sequence_id_ = sequence_id;
   }
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long getSequenceId()
   {
      return sequence_id_;
   }

   public void setTrajectoryDuration(double trajectory_duration)
   {
      trajectory_duration_ = trajectory_duration;
   }
   public double getTrajectoryDuration()
   {
      return trajectory_duration_;
   }


   public us.ihmc.idl.IDLSequence.Double  getJointAngles()
   {
      return joint_angles_;
   }


   public us.ihmc.euclid.geometry.Pose3D getPelvisPose()
   {
      return pelvis_pose_;
   }


   public static Supplier<WholeBodyPoseTrajectoryMessagePubSubType> getPubSubType()
   {
      return WholeBodyPoseTrajectoryMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return WholeBodyPoseTrajectoryMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(WholeBodyPoseTrajectoryMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.sequence_id_, other.sequence_id_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.trajectory_duration_, other.trajectory_duration_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsDoubleSequence(this.joint_angles_, other.joint_angles_, epsilon)) return false;

      if (!this.pelvis_pose_.epsilonEquals(other.pelvis_pose_, epsilon)) return false;

      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof WholeBodyPoseTrajectoryMessage)) return false;

      WholeBodyPoseTrajectoryMessage otherMyClass = (WholeBodyPoseTrajectoryMessage) other;

      if(this.sequence_id_ != otherMyClass.sequence_id_) return false;

      if(this.trajectory_duration_ != otherMyClass.trajectory_duration_) return false;

      if (!this.joint_angles_.equals(otherMyClass.joint_angles_)) return false;
      if (!this.pelvis_pose_.equals(otherMyClass.pelvis_pose_)) return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("WholeBodyPoseTrajectoryMessage {");
      builder.append("sequence_id=");
      builder.append(this.sequence_id_);      builder.append(", ");
      builder.append("trajectory_duration=");
      builder.append(this.trajectory_duration_);      builder.append(", ");
      builder.append("joint_angles=");
      builder.append(this.joint_angles_);      builder.append(", ");
      builder.append("pelvis_pose=");
      builder.append(this.pelvis_pose_);
      builder.append("}");
      return builder.toString();
   }
}
