package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

/**
       * This message is part of the IHMC footstep planning module.
       * This message is sent from the planner in order to visualize and debug planner progress
       */
public class FootstepPlannerLatticeMapMessage extends Packet<FootstepPlannerLatticeMapMessage> implements Settable<FootstepPlannerLatticeMapMessage>, EpsilonComparable<FootstepPlannerLatticeMapMessage>
{
   /**
            * This is the ID of the planning request which this message corresponds to
            */
   public long sequence_id_;
   /**
            * List of cells that the planner has explored
            */
   public us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.FootstepPlannerLatticeNodeMessage>  lattice_nodes_;

   public FootstepPlannerLatticeMapMessage()
   {
      lattice_nodes_ = new us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.FootstepPlannerLatticeNodeMessage> (100, new controller_msgs.msg.dds.FootstepPlannerLatticeNodeMessagePubSubType());

   }

   public FootstepPlannerLatticeMapMessage(FootstepPlannerLatticeMapMessage other)
   {
      this();
      set(other);
   }

   public void set(FootstepPlannerLatticeMapMessage other)
   {
      sequence_id_ = other.sequence_id_;

      lattice_nodes_.set(other.lattice_nodes_);
   }

   /**
            * This is the ID of the planning request which this message corresponds to
            */
   public void setSequenceId(long sequence_id)
   {
      sequence_id_ = sequence_id;
   }
   /**
            * This is the ID of the planning request which this message corresponds to
            */
   public long getSequenceId()
   {
      return sequence_id_;
   }


   /**
            * List of cells that the planner has explored
            */
   public us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.FootstepPlannerLatticeNodeMessage>  getLatticeNodes()
   {
      return lattice_nodes_;
   }


   public static Supplier<FootstepPlannerLatticeMapMessagePubSubType> getPubSubType()
   {
      return FootstepPlannerLatticeMapMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return FootstepPlannerLatticeMapMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(FootstepPlannerLatticeMapMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.sequence_id_, other.sequence_id_, epsilon)) return false;

      if (this.lattice_nodes_.size() != other.lattice_nodes_.size()) { return false; }
      else
      {
         for (int i = 0; i < this.lattice_nodes_.size(); i++)
         {  if (!this.lattice_nodes_.get(i).epsilonEquals(other.lattice_nodes_.get(i), epsilon)) return false; }
      }


      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof FootstepPlannerLatticeMapMessage)) return false;

      FootstepPlannerLatticeMapMessage otherMyClass = (FootstepPlannerLatticeMapMessage) other;

      if(this.sequence_id_ != otherMyClass.sequence_id_) return false;

      if (!this.lattice_nodes_.equals(otherMyClass.lattice_nodes_)) return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("FootstepPlannerLatticeMapMessage {");
      builder.append("sequence_id=");
      builder.append(this.sequence_id_);      builder.append(", ");
      builder.append("lattice_nodes=");
      builder.append(this.lattice_nodes_);
      builder.append("}");
      return builder.toString();
   }
}
