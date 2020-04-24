package us.ihmc.robotics.physics;

import java.util.*;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.mecano.algorithms.interfaces.RigidBodyTwistProvider;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyReadOnly;
import us.ihmc.mecano.spatial.Twist;
import us.ihmc.mecano.spatial.interfaces.TwistReadOnly;
import us.ihmc.mecano.tools.MultiBodySystemTools;

public class ImpulseBasedRigidBodyTwistProvider implements RigidBodyTwistProvider
{
   private final ReferenceFrame inertialFrame;
   private final RigidBodyBasics rootBody;
   private int impulseDimension;

   private final Twist twist = new Twist();
   private final FrameVector3D linearVelocity = new FrameVector3D();
   private boolean isImpulseZero = true;
   private final DenseMatrix64F impulse = new DenseMatrix64F(6, 1);
   private final List<RigidBodyBasics> rigidBodies = new ArrayList<>();
   private final Map<RigidBodyBasics, DenseMatrix64F> apparentInertiaMatrixInverseMap = new HashMap<>();

   public ImpulseBasedRigidBodyTwistProvider(ReferenceFrame inertialFrame, RigidBodyBasics rootBody)
   {
      this.inertialFrame = inertialFrame;
      this.rootBody = rootBody;
   }

   public void clear(int impulseDimension)
   {
      isImpulseZero = true;
      this.impulseDimension = impulseDimension;
      impulse.reshape(impulseDimension, 1);
      impulse.zero();
      rigidBodies.clear();
      apparentInertiaMatrixInverseMap.clear();
   }

   public void addAll(Collection<? extends RigidBodyBasics> bodies)
   {
      for (RigidBodyBasics body : bodies)
      {
         add(body);
      }
   }

   public void add(RigidBodyBasics rigidBody)
   {
      if (MultiBodySystemTools.getRootBody(rigidBody) != rootBody)
         return;

      rigidBodies.add(rigidBody);
      apparentInertiaMatrixInverseMap.put(rigidBody, new DenseMatrix64F(Twist.SIZE, impulseDimension));
   }

   public List<RigidBodyBasics> getRigidBodies()
   {
      return rigidBodies;
   }

   public DenseMatrix64F getApparentInertiaMatrixInverse(RigidBodyBasics rigidBody)
   {
      return apparentInertiaMatrixInverseMap.get(rigidBody);
   }

   public void setImpulseToZero()
   {
      isImpulseZero = true;
      impulse.zero();
   }

   public void setImpulse(double impulse)
   {
      isImpulseZero = false;
      this.impulse.set(0, impulse);
   }

   public void setImpulse(DenseMatrix64F impulse)
   {
      isImpulseZero = false;
      this.impulse.set(impulse);
   }

   public void setImpulse(Vector3DReadOnly impulse)
   {
      isImpulseZero = false;
      impulse.get(this.impulse);
   }

   private final DenseMatrix64F twistMatrix = new DenseMatrix64F(Twist.SIZE, 1);

   @Override
   public TwistReadOnly getTwistOfBody(RigidBodyReadOnly body)
   {
      if (isImpulseZero)
         return null;
      DenseMatrix64F apparentInertiaMatrixInverse = apparentInertiaMatrixInverseMap.get(body);
      if (apparentInertiaMatrixInverse == null)
         return null;

      CommonOps.mult(apparentInertiaMatrixInverse, impulse, twistMatrix);
      twist.setIncludingFrame(body.getBodyFixedFrame(), inertialFrame, body.getBodyFixedFrame(), twistMatrix);

      return twist;
   }

   @Override
   public TwistReadOnly getRelativeTwist(RigidBodyReadOnly base, RigidBodyReadOnly body)
   {
      throw new UnsupportedOperationException("Relative twist is not supported.");
   }

   @Override
   public FrameVector3DReadOnly getLinearVelocityOfBodyFixedPoint(RigidBodyReadOnly base, RigidBodyReadOnly body, FramePoint3DReadOnly bodyFixedPoint)
   {
      if (isImpulseZero)
         return null;
      if (base != null)
         getRelativeTwist(base, body).getLinearVelocityAt(bodyFixedPoint, linearVelocity);
      else
         getTwistOfBody(body).getLinearVelocityAt(bodyFixedPoint, linearVelocity);
      return linearVelocity;
   }

   @Override
   public ReferenceFrame getInertialFrame()
   {
      return inertialFrame;
   }
}