package us.ihmc.footstepPlanning.graphSearch.nodeChecking;

import us.ihmc.commons.InterpolationTools;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.Pose2D;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapAndWiggler;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.BipedalFootstepPlannerNodeRejectionReason;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.referenceFrames.TransformReferenceFrame;
import us.ihmc.robotics.referenceFrames.ZUpFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFramePose3D;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFramePoseUsingYawPitchRoll;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

import java.util.function.UnaryOperator;

public class FootstepPoseChecker
{
   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private final FootstepPlannerParametersReadOnly parameters;
   private final FootstepNodeSnapAndWiggler snapper;

   private final TransformReferenceFrame startOfSwingFrame = new TransformReferenceFrame("startOfSwingFrame", ReferenceFrame.getWorldFrame());
   private final TransformReferenceFrame stanceFootFrame = new TransformReferenceFrame("stanceFootFrame", ReferenceFrame.getWorldFrame());
   private final TransformReferenceFrame candidateFootFrame = new TransformReferenceFrame("candidateFootFrame", ReferenceFrame.getWorldFrame());
   private final ZUpFrame startOfSwingZUpFrame = new ZUpFrame(ReferenceFrame.getWorldFrame(), startOfSwingFrame, "startOfSwingZUpFrame");
   private final ZUpFrame stanceFootZUpFrame = new ZUpFrame(ReferenceFrame.getWorldFrame(), stanceFootFrame, "stanceFootZUpFrame");
   private final FramePose3D stanceFootPose = new FramePose3D();
   private final FramePose3D candidateFootPose = new FramePose3D();

   private final YoFramePoseUsingYawPitchRoll yoStanceFootPose = new YoFramePoseUsingYawPitchRoll("stance", ReferenceFrame.getWorldFrame(), registry);
   private final YoFramePoseUsingYawPitchRoll yoCandidateFootPose = new YoFramePoseUsingYawPitchRoll("candidate", stanceFootZUpFrame, registry);

   private UnaryOperator<FootstepNode> parentNodeSupplier;

   private final YoDouble stepWidth = new YoDouble("stepWidth", registry);
   private final YoDouble stepLength = new YoDouble("stepLength", registry);
   private final YoDouble stepHeight = new YoDouble("stepHeight", registry);
   private final YoDouble stepReachXY = new YoDouble("stepReachXY", registry);
   private final YoDouble stepYaw = new YoDouble("stepYaw", registry);

   private final YoBoolean stepIsPitchedBack = new YoBoolean("stepIsPitchedBack", registry);
   private final YoBoolean stepTooLow = new YoBoolean("stepTooLow", registry);
   private final YoBoolean stepTooForward = new YoBoolean("stepTooForward", registry);
   private final YoDouble minZFromPitchContraint = new YoDouble("minZFromPitchContraint", registry);
   private final YoDouble maxXFromPitchContraint = new YoDouble("maxXFromPitchContraint", registry);

   public FootstepPoseChecker(FootstepPlannerParametersReadOnly parameters, FootstepNodeSnapAndWiggler snapper, YoRegistry parentRegistry)
   {
      this.parameters = parameters;
      this.snapper = snapper;
      parentRegistry.addChild(registry);
   }

   public void setParentNodeSupplier(UnaryOperator<FootstepNode> parentNodeSupplier)
   {
      this.parentNodeSupplier = parentNodeSupplier;
   }

   public BipedalFootstepPlannerNodeRejectionReason checkStepValidity(FootstepNode candidateNode, FootstepNode stanceNode)
   {
      RobotSide stepSide = candidateNode.getRobotSide();

      FootstepNodeSnapData candidateNodeSnapData = snapper.snapFootstepNode(candidateNode);
      FootstepNodeSnapData stanceNodeSnapData = snapper.snapFootstepNode(stanceNode);

      candidateFootFrame.setTransformAndUpdate(candidateNodeSnapData.getSnappedNodeTransform(candidateNode));
      stanceFootFrame.setTransformAndUpdate(stanceNodeSnapData.getSnappedNodeTransform(stanceNode));
      stanceFootZUpFrame.update();

      candidateFootPose.setToZero(candidateFootFrame);
      candidateFootPose.changeFrame(stanceFootZUpFrame);
      yoCandidateFootPose.set(candidateFootPose);

      stanceFootPose.setToZero(stanceFootFrame);
      stanceFootPose.changeFrame(ReferenceFrame.getWorldFrame());
      yoStanceFootPose.set(stanceFootPose);

      stepLength.set(candidateFootPose.getX());
      stepWidth.set(stepSide.negateIfRightSide(candidateFootPose.getY()));
      stepReachXY.set(EuclidGeometryTools.pythagorasGetHypotenuse(Math.abs(candidateFootPose.getX()), Math.abs(stepWidth.getValue() - parameters.getIdealFootstepWidth())));
      stepHeight.set(candidateFootPose.getZ());
      double maximumStepZ = stepSide == RobotSide.LEFT ? parameters.getMaximumLeftStepZ() : parameters.getMaximumRightStepZ();

      if (stepWidth.getValue() < parameters.getMinimumStepWidth())
      {
         return BipedalFootstepPlannerNodeRejectionReason.STEP_NOT_WIDE_ENOUGH;
      }
      else if (stepWidth.getValue() > parameters.getMaximumStepWidth())
      {
         return BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_WIDE;
      }
      else if (stepLength.getValue() < parameters.getMinimumStepLength())
      {
         return BipedalFootstepPlannerNodeRejectionReason.STEP_NOT_LONG_ENOUGH;
      }
      else if (Math.abs(stepHeight.getValue()) > maximumStepZ)
      {
         return BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_HIGH_OR_LOW;
      }

      double alphaPitchedBack = Math.max(0.0, - stanceFootPose.getPitch() / parameters.getMinimumSurfaceInclineRadians());
      minZFromPitchContraint.set(InterpolationTools.linearInterpolate(Math.abs(maximumStepZ), Math.abs(parameters.getMinimumStepZWhenFullyPitched()), alphaPitchedBack));
      maxXFromPitchContraint.set(InterpolationTools.linearInterpolate(Math.abs(parameters.getMaximumStepReach()), parameters.getMaximumStepXWhenFullyPitched(), alphaPitchedBack));
      double stepDownFraction = - stepHeight.getValue() / minZFromPitchContraint.getValue();
      double stepForwardFraction = stepLength.getValue() / maxXFromPitchContraint.getValue();

      stepIsPitchedBack.set(alphaPitchedBack > 0.0);
      stepTooLow.set(stepLength.getValue() > 0.0 && stepDownFraction > 1.0);
      stepTooForward.set(stepHeight.getValue() < 0.0 && stepForwardFraction > 1.0);

      if (stepIsPitchedBack.getBooleanValue() && (stepTooLow.getBooleanValue() || stepTooForward.getBooleanValue()))
      {
         return BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_LOW_AND_FORWARD_WHEN_PITCHED;
      }

      double maxReach = parameters.getMaximumStepReach();
      if (stepHeight.getValue() < -Math.abs(parameters.getMaximumStepZWhenForwardAndDown()))
      {
         if (stepLength.getValue() > parameters.getMaximumStepXWhenForwardAndDown())
         {
            return BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FORWARD_AND_DOWN;
         }

         if (stepWidth.getValue() > parameters.getMaximumStepYWhenForwardAndDown())
         {
            return BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_WIDE_AND_DOWN;
         }

         maxReach = EuclidCoreTools.norm(parameters.getMaximumStepXWhenForwardAndDown(), parameters.getMaximumStepYWhenForwardAndDown() - parameters.getIdealFootstepWidth());
      }

      if (stepReachXY.getValue() > parameters.getMaximumStepReach())
      {
         return BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FAR;
      }

      if (stepHeight.getValue() > parameters.getMaximumStepZWhenSteppingUp())
      {
         if (stepReachXY.getValue() > parameters.getMaximumStepReachWhenSteppingUp())
         {
            return BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_AND_HIGH;
         }
         if (stepWidth.getValue() > parameters.getMaximumStepWidthWhenSteppingUp())
         {
            return BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_WIDE_AND_HIGH;
         }

         maxReach = parameters.getMaximumStepReachWhenSteppingUp();
      }

      double stepReach3D = EuclidCoreTools.norm(stepReachXY.getValue(), stepHeight.getValue());
      double maxInterpolationFactor = Math.max(stepReach3D / maxReach, Math.abs(stepHeight.getValue() / maximumStepZ));
      maxInterpolationFactor = Math.min(maxInterpolationFactor, 1.0);

      double maxYaw = InterpolationTools.linearInterpolate(parameters.getMaximumStepYaw(), (1.0 - parameters.getStepYawReductionFactorAtMaxReach()) * parameters.getMaximumStepYaw(),
                                                           maxInterpolationFactor);
      double minYaw = InterpolationTools.linearInterpolate(parameters.getMinimumStepYaw(), (1.0 - parameters.getStepYawReductionFactorAtMaxReach()) * parameters.getMinimumStepYaw(),
                                                           maxInterpolationFactor);
      double yawDelta = AngleTools.computeAngleDifferenceMinusPiToPi(candidateNode.getYaw(), stanceNode.getYaw());
      if (!MathTools.intervalContains(stepSide.negateIfRightSide(yawDelta), minYaw, maxYaw))
      {
         return BipedalFootstepPlannerNodeRejectionReason.STEP_YAWS_TOO_MUCH;
      }

      // Check reach from start of swing
      FootstepNode grandParentNode;
      FootstepNodeSnapData grandparentNodeSnapData;
      double alphaSoS = parameters.getTranslationScaleFromGrandparentNode();
      if (alphaSoS > 0.0 && parentNodeSupplier != null && (grandParentNode = parentNodeSupplier.apply(stanceNode)) != null
          && (grandparentNodeSnapData = snapper.snapFootstepNode(grandParentNode)) != null)
      {
         startOfSwingFrame.setTransformAndUpdate(grandparentNodeSnapData.getSnappedNodeTransform(grandParentNode));
         startOfSwingZUpFrame.update();
         candidateFootPose.changeFrame(startOfSwingZUpFrame);
         double swingHeight = candidateFootPose.getZ();
         double swingReach = EuclidGeometryTools.pythagorasGetHypotenuse(Math.abs(candidateFootPose.getX()), Math.abs(candidateFootPose.getY()));

         if (swingHeight > parameters.getMaximumStepZWhenSteppingUp())
         {
            if (swingReach > alphaSoS * parameters.getMaximumStepReachWhenSteppingUp())
            {
               return BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_AND_HIGH;
            }
         }
      }

      return null;
   }

   void setApproximateStepDimensions(FootstepNode candidateNode, FootstepNode stanceNode)
   {
      if (stanceNode == null)
      {
         return;
      }

      double dx = candidateNode.getX() - stanceNode.getX();
      double dy = candidateNode.getY() - stanceNode.getY();

      double stepLength = dx * Math.cos(stanceNode.getYaw()) + dy * Math.sin(stanceNode.getYaw());
      double stepWidth = - dx * Math.sin(stanceNode.getYaw()) + dy * Math.cos(stanceNode.getYaw());
      stepWidth = stanceNode.getRobotSide().negateIfLeftSide(stepWidth);

      double stepYaw = AngleTools.computeAngleDifferenceMinusPiToPi(candidateNode.getYaw(), stanceNode.getYaw());
      stepYaw = stanceNode.getRobotSide().negateIfLeftSide(stepYaw);

      this.stepLength.set(stepLength);
      this.stepWidth.set(stepWidth);
      this.stepYaw.set(stepYaw);
   }

   void clearLoggedVariables()
   {
      stepWidth.setToNaN();
      stepLength.setToNaN();
      stepHeight.setToNaN();
      stepReachXY.setToNaN();
      yoStanceFootPose.setToNaN();
      yoCandidateFootPose.setToNaN();

      stepIsPitchedBack.set(false);
      stepTooLow.set(false);
      stepTooForward.set(false);
      minZFromPitchContraint.setToNaN();
      maxXFromPitchContraint.setToNaN();
   }
}
