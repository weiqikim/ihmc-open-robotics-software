package us.ihmc.robotics.screwTheory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import gnu.trove.list.array.TIntArrayList;
import us.ihmc.euclid.matrix.Matrix3D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.mecano.multiBodySystem.PrismaticJoint;
import us.ihmc.mecano.multiBodySystem.RevoluteJoint;
import us.ihmc.mecano.multiBodySystem.RigidBody;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.JointReadOnly;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.multiBodySystem.iterators.SubtreeStreams;
import us.ihmc.mecano.spatial.SpatialAcceleration;
import us.ihmc.mecano.tools.MultiBodySystemFactories;
import us.ihmc.mecano.tools.MultiBodySystemTools;
import us.ihmc.robotics.geometry.TransformTools;

public class ScrewTools
{
   public static PassiveRevoluteJoint addPassiveRevoluteJoint(String jointName, RigidBodyBasics parentBody, Vector3D jointOffset, Vector3D jointAxis,
                                                              boolean isPartOfClosedKinematicLoop)
   {
      return addPassiveRevoluteJoint(jointName, parentBody, TransformTools.createTranslationTransform(jointOffset), jointAxis, isPartOfClosedKinematicLoop);
   }

   public static PassiveRevoluteJoint addPassiveRevoluteJoint(String jointName, RigidBodyBasics parentBody, RigidBodyTransform transformToParent,
                                                              Vector3D jointAxis, boolean isPartOfClosedKinematicLoop)
   {
      return new PassiveRevoluteJoint(jointName, parentBody, transformToParent, jointAxis, isPartOfClosedKinematicLoop);
   }

   public static RigidBodyBasics[] computeSubtreeSuccessors(JointBasics... joints)
   {
      return Stream.of(joints).map(JointBasics::getSuccessor).flatMap(RigidBodyBasics::subtreeStream).distinct().toArray(RigidBodyBasics[]::new);
   }

   public static RigidBodyBasics[] computeSubtreeSuccessors(RigidBodyBasics... bodies)
   {
      return MultiBodySystemTools.collectSuccessors(computeSubtreeJoints(bodies));
   }

   public static RigidBodyBasics[] computeSupportAndSubtreeSuccessors(RigidBodyBasics... bodies)
   {
      return MultiBodySystemTools.collectSuccessors(computeSupportAndSubtreeJoints(bodies));
   }

   public static JointBasics[] computeSupportAndSubtreeJoints(RigidBodyBasics... bodies)
   {
      Set<JointBasics> ret = new LinkedHashSet<JointBasics>();
      for (RigidBodyBasics body : bodies)
      {
         ret.addAll(Arrays.asList(computeSupportJoints(body)));
         ret.addAll(Arrays.asList(computeSubtreeJoints(body)));
      }
      return ret.toArray(new JointBasics[ret.size()]);
   }

   public static JointBasics[] computeSupportJoints(RigidBodyBasics... bodies)
   {
      Set<JointBasics> supportSet = new LinkedHashSet<JointBasics>();
      for (RigidBodyBasics rigidBody : bodies)
      {
         RigidBodyBasics rootBody = MultiBodySystemTools.getRootBody(rigidBody);
         JointBasics[] jointPath = MultiBodySystemTools.createJointPath(rootBody, rigidBody);
         supportSet.addAll(Arrays.asList(jointPath));
      }

      return supportSet.toArray(new JointBasics[supportSet.size()]);
   }

   public static JointBasics[] computeSubtreeJoints(RigidBodyBasics... rootBodies)
   {
      return computeSubtreeJoints(Arrays.asList(rootBodies));
   }

   public static JointBasics[] computeSubtreeJoints(List<RigidBodyBasics> rootBodies)
   {
      return rootBodies.stream().flatMap(root -> SubtreeStreams.fromChildren(root)).toArray(JointBasics[]::new);
   }

   /**
    * Compute and pack the joint path between two RigidBody in the jointPathToPack. Use the method
    * {@link #computeDistanceToAncestor(RigidBodyBasics, RigidBodyBasics)} to get the size of the Array
    * to provide.
    * 
    * @param jointPathToPack
    * @param start
    * @param end
    * @return the length of the joint path, returns -1 if the the given jointPathToPack is too small.
    */
   public static int createJointPath(JointBasics[] jointPathToPack, RigidBodyBasics start, RigidBodyBasics end)
   {
      boolean flip = false;
      RigidBodyBasics descendant = start;
      RigidBodyBasics ancestor = end;
      int pathLength = MultiBodySystemTools.computeDistanceToAncestor(descendant, ancestor);
      if (pathLength < 0)
      {
         flip = true;
         descendant = end;
         ancestor = start;
         pathLength = MultiBodySystemTools.computeDistanceToAncestor(end, start);
      }

      if (jointPathToPack == null || jointPathToPack.length < pathLength)
         return -1;

      RigidBodyBasics currentBody = descendant;
      int i = 0;
      while (currentBody != ancestor)
      {
         int j = flip ? pathLength - 1 - i : i;
         JointBasics parentJoint = currentBody.getParentJoint();
         jointPathToPack[j] = parentJoint;
         currentBody = parentJoint.getPredecessor();
         i++;
      }

      for (int k = pathLength; k < jointPathToPack.length; k++)
         jointPathToPack[k] = null;

      return pathLength;
   }

   public static OneDoFJointBasics[] cloneOneDoFJointPath(RigidBodyBasics start, RigidBodyBasics end)
   {
      return MultiBodySystemFactories.cloneOneDoFJointKinematicChain(start, end);
   }

   public static OneDoFJointBasics[] cloneOneDoFJointPath(OneDoFJointBasics[] oneDoFJoints)
   {
      return cloneJointPathAndFilter(oneDoFJoints, OneDoFJointBasics.class);
   }

   public static <T extends JointBasics> T[] cloneJointPathAndFilter(T[] joints, Class<T> clazz)
   {
      return MultiBodySystemTools.filterJoints(cloneJointPath(joints), clazz);
   }

   public static <T extends JointBasics> T[] cloneJointPathAndFilter(T[] joints, Class<T> clazz, String suffix)
   {
      return MultiBodySystemTools.filterJoints(cloneJointPath(joints, suffix), clazz);
   }

   public static JointBasics[] cloneJointPath(JointBasics[] inverseDynamicsJoints)
   {
      return MultiBodySystemFactories.cloneKinematicChain(inverseDynamicsJoints);
   }

   public static JointBasics[] cloneJointPath(JointBasics[] inverseDynamicsJoints, String suffix)
   {
      return MultiBodySystemFactories.cloneKinematicChain(inverseDynamicsJoints, suffix);
   }

   public static <T extends JointBasics> T[] cloneJointPathDisconnectedFromOriginalRobot(T[] joints, Class<T> clazz, String suffix,
                                                                                         ReferenceFrame rootBodyFrame)
   {
      return MultiBodySystemTools.filterJoints(cloneJointPathDisconnectedFromOriginalRobot(joints, suffix, rootBodyFrame), clazz);
   }

   public static JointBasics[] cloneJointPathDisconnectedFromOriginalRobot(JointBasics[] inverseDynamicsJoints, String suffix, ReferenceFrame rootBodyFrame)
   {
      JointBasics[] cloned = new JointBasics[inverseDynamicsJoints.length];

      for (int i = 0; i < inverseDynamicsJoints.length; i++)
      {
         if (inverseDynamicsJoints[i] instanceof RevoluteJoint)
         {
            RevoluteJoint jointOriginal = (RevoluteJoint) inverseDynamicsJoints[i];

            RigidBodyBasics predecessorOriginal = jointOriginal.getPredecessor();
            RigidBodyBasics predecessorCopy;

            if (i > 0)
            {
               predecessorCopy = cloned[i - 1].getSuccessor();
            }
            else
            {
               String predecessorNameOriginal = predecessorOriginal.getName();
               predecessorCopy = new RigidBody(predecessorNameOriginal + suffix, rootBodyFrame);
            }

            cloned[i] = cloneOneDoFJoint(jointOriginal, suffix, predecessorCopy);
         }
         else
         {
            throw new RuntimeException("Not implemented for joints of the type: " + inverseDynamicsJoints[i].getClass().getSimpleName());
         }

         cloneRigidBody(inverseDynamicsJoints[i].getSuccessor(), suffix, cloned[i]);
      }
      return cloned;
   }

   private static OneDoFJointBasics cloneOneDoFJoint(OneDoFJointBasics original, String cloneSuffix, RigidBodyBasics clonePredecessor)
   {
      String jointNameOriginal = original.getName();
      RigidBodyTransform jointTransform = new RigidBodyTransform();
      original.getJointOffset(jointTransform);
      Vector3D jointAxisCopy = new Vector3D(original.getJointAxis());
      OneDoFJointBasics clone;

      if (original instanceof RevoluteJoint)
         clone = new RevoluteJoint(jointNameOriginal + cloneSuffix, clonePredecessor, jointTransform, jointAxisCopy);
      else if (original instanceof PrismaticJoint)
         clone = new PrismaticJoint(jointNameOriginal + cloneSuffix, clonePredecessor, jointTransform, jointAxisCopy);
      else
         throw new RuntimeException("Unhandled type of " + OneDoFJointBasics.class.getSimpleName() + ": " + original.getClass().getSimpleName());

      clone.setJointLimitLower(original.getJointLimitLower());
      clone.setJointLimitUpper(original.getJointLimitUpper());
      clone.setVelocityLimits(original.getVelocityLimitLower(), original.getVelocityLimitUpper());
      clone.setEffortLimits(original.getEffortLimitLower(), original.getEffortLimitUpper());
      return clone;
   }

   private static RigidBodyBasics cloneRigidBody(RigidBodyBasics original, String cloneSuffix, JointBasics parentJointOfClone)
   {
      FramePoint3D comOffset = new FramePoint3D();
      original.getCenterOfMass(comOffset);
      comOffset.changeFrame(original.getParentJoint().getFrameAfterJoint());
      String nameOriginal = original.getName();
      Matrix3D massMomentOfInertiaPartCopy = new Matrix3D(original.getInertia().getMomentOfInertia());
      double mass = original.getInertia().getMass();
      Vector3D comOffsetCopy = new Vector3D(comOffset);
      RigidBodyBasics clone = new RigidBody(nameOriginal + cloneSuffix, parentJointOfClone, massMomentOfInertiaPartCopy, mass, comOffsetCopy);
      return clone;
   }

   public static SpatialAcceleration createGravitationalSpatialAcceleration(RigidBodyBasics rootBody, double gravity)
   {
      Vector3D gravitationalAcceleration = new Vector3D(0.0, 0.0, gravity);
      Vector3D zero = new Vector3D();
      SpatialAcceleration rootAcceleration = new SpatialAcceleration(rootBody.getBodyFixedFrame(), ReferenceFrame.getWorldFrame(), rootBody.getBodyFixedFrame(),
                                                                     zero, gravitationalAcceleration);

      return rootAcceleration;
   }

   public static void computeIndicesForJoint(JointBasics[] jointsInOrder, TIntArrayList listToPackIndices, JointBasics... jointsToComputeIndicesFor)
   {
      int startIndex = 0;
      for (int i = 0; i < jointsInOrder.length; i++)
      {
         int nDegreesOfFreedom = jointsInOrder[i].getDegreesOfFreedom();

         for (int j = 0; j < jointsToComputeIndicesFor.length; j++)
         {
            if (jointsInOrder[i] == jointsToComputeIndicesFor[j])
            {
               for (int k = startIndex; k < startIndex + nDegreesOfFreedom; k++)
               {
                  listToPackIndices.add(k);
               }
            }
         }

         startIndex += nDegreesOfFreedom;
      }
   }

   public static void computeIndexForJoint(List<? extends JointReadOnly> jointsInOrder, TIntArrayList listToPackIndices, JointReadOnly jointToComputeIndicesFor)
   {
      int startIndex = 0;
      for (int i = 0; i < jointsInOrder.size(); i++)
      {
         JointReadOnly joint = jointsInOrder.get(i);
         int nDegreesOfFreedom = joint.getDegreesOfFreedom();

         if (joint == jointToComputeIndicesFor)
         {
            for (int k = startIndex; k < startIndex + nDegreesOfFreedom; k++)
            {
               listToPackIndices.add(k);
            }
         }

         startIndex += nDegreesOfFreedom;
      }
   }

   public static void computeIndexForJoint(JointReadOnly[] jointsInOrder, TIntArrayList listToPackIndices, JointReadOnly jointToComputeIndicesFor)
   {
      int startIndex = 0;
      for (int i = 0; i < jointsInOrder.length; i++)
      {
         int nDegreesOfFreedom = jointsInOrder[i].getDegreesOfFreedom();

         if (jointsInOrder[i] == jointToComputeIndicesFor)
         {
            for (int k = startIndex; k < startIndex + nDegreesOfFreedom; k++)
            {
               listToPackIndices.add(k);
            }
         }

         startIndex += nDegreesOfFreedom;
      }
   }

   public static JointBasics[] findJointsWithNames(JointBasics[] allJoints, String... jointNames)
   {
      Set<String> jointNameSet = new HashSet<>(Arrays.asList(jointNames));
      JointBasics[] result = Stream.of(allJoints).distinct().filter(joint -> jointNameSet.contains(joint.getName())).toArray(JointBasics[]::new);

      if (result.length != jointNames.length)
         throw new RuntimeException("Not all joints could be found");

      return result;
   }

   public static RigidBodyBasics[] findRigidBodiesWithNames(RigidBodyBasics[] allBodies, String... bodyNames)
   {
      Set<String> bodyNameSet = new HashSet<>(Arrays.asList(bodyNames));
      RigidBodyBasics[] result = Stream.of(allBodies).distinct().filter(body -> bodyNameSet.contains(body.getName())).toArray(RigidBodyBasics[]::new);

      if (result.length != bodyNames.length)
         throw new RuntimeException("Not all bodies could be found");

      return result;
   }

   public static int computeGeometricJacobianHashCode(JointBasics joints[], ReferenceFrame jacobianFrame, boolean allowChangeFrame)
   {
      int jointsHashCode = 1;
      for (JointBasics joint : joints)
      {
         jointsHashCode = 31 * jointsHashCode + joint.hashCode();
      }
      if (!allowChangeFrame)
         return 31 * jointsHashCode + jacobianFrame.hashCode();
      else
         return jointsHashCode;
   }

   public static int computeGeometricJacobianHashCode(JointBasics joints[], int firstIndex, int lastIndex, ReferenceFrame jacobianFrame,
                                                      boolean allowChangeFrame)
   {
      int jointsHashCode = 1;
      for (int i = firstIndex; i <= lastIndex; i++)
      {
         jointsHashCode = 31 * jointsHashCode + joints[i].hashCode();
      }
      if (!allowChangeFrame)
         return 31 * jointsHashCode + jacobianFrame.hashCode();
      else
         return jointsHashCode;
   }

   /**
    * Will return the {@code numberOfBodies}'eth parent of the provided {@code startBody}. E.g. if
    * {@code numberOfBodies == 1} this will return the parent of the {@code startBody} and so on.
    * 
    * @throws RuntimeException if the body chain is not long enough to reach the desired parent.
    * @param startBody the body to start at.
    * @param numberOfBodies the amount of steps to go up the body chain.
    * @return the {@link RigidBodyBasics} that is {@code numberOfBodies} higher up the rigid body chain
    *         then the {@code startBody}.
    */
   public static RigidBodyBasics goUpBodyChain(RigidBodyBasics startBody, int numberOfBodies)
   {
      if (numberOfBodies == 0)
      {
         return startBody;
      }
      JointBasics parentJoint = startBody.getParentJoint();
      if (parentJoint == null)
      {
         throw new RuntimeException("Reached root body. Can not move up the chain any further.");
      }
      return goUpBodyChain(parentJoint.getPredecessor(), numberOfBodies - 1);
   }
}