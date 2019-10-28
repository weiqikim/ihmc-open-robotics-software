package us.ihmc.footstepPlanning.postProcessing;

import controller_msgs.msg.dds.FootstepDataMessage;
import controller_msgs.msg.dds.FootstepPlanningRequestPacket;
import controller_msgs.msg.dds.FootstepPlanningToolboxOutputStatus;
import controller_msgs.msg.dds.FootstepPostProcessingPacket;
import us.ihmc.commonWalkingControlModules.configurations.ICPPlannerParameters;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.footstepPlanning.postProcessing.parameters.FootstepPostProcessingParametersReadOnly;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;

import java.util.List;

public class AreaSplitFractionPostProcessingElement implements FootstepPlanPostProcessingElement
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final FootstepPostProcessingParametersReadOnly parameters;
   private final ICPPlannerParameters walkingControllerParameters;

   public AreaSplitFractionPostProcessingElement(FootstepPostProcessingParametersReadOnly parameters, ICPPlannerParameters walkingControllerParameters)
   {
      this.parameters = parameters;
      this.walkingControllerParameters = walkingControllerParameters;
   }

   /** {@inheritDoc} **/
   @Override
   public boolean isActive()
   {
      return parameters.areaSplitFractionProcessingEnabled();
   }

   /** {@inheritDoc} **/
   @Override
   public FootstepPostProcessingPacket postProcessFootstepPlan(FootstepPostProcessingPacket inputPlan)
   {
      FootstepPostProcessingPacket processedPlan = new FootstepPostProcessingPacket(inputPlan);

      ConvexPolygon2D previousPolygon = new ConvexPolygon2D();
      ConvexPolygon2D currentPolygon = new ConvexPolygon2D();

      PoseReferenceFrame previousFrame = new PoseReferenceFrame("previousFrame", worldFrame);
      PoseReferenceFrame currentFrame = new PoseReferenceFrame("nextFrame", worldFrame);

      double defaultTransferSplitFraction = walkingControllerParameters.getTransferSplitFraction();
      double defaultWeightDistribution = 0.5;

      List<FootstepDataMessage> footstepDataMessageList = processedPlan.getFootstepDataList().getFootstepDataList();
      for (int stepNumber = 1; stepNumber < footstepDataMessageList.size(); stepNumber++)
      {
         FootstepDataMessage previousStep = footstepDataMessageList.get(stepNumber - 1);
         FootstepDataMessage currentStep = footstepDataMessageList.get(stepNumber);

         previousFrame.setPositionAndUpdate(new FramePoint3D(worldFrame, previousStep.getLocation()));
         previousFrame.setOrientationAndUpdate(previousStep.getOrientation());

         currentFrame.setPositionAndUpdate(new FramePoint3D(worldFrame, currentStep.getLocation()));
         currentFrame.setOrientationAndUpdate(currentStep.getOrientation());

         for (Point3DReadOnly vertex : previousStep.getPredictedContactPoints2d())
         {
            FramePoint3D vertexInSoleFrame = new FramePoint3D(worldFrame, vertex);
            vertexInSoleFrame.changeFrame(previousFrame);

            previousPolygon.addVertex(vertexInSoleFrame);
         }
         previousPolygon.update();

         for (Point3DReadOnly vertex : currentStep.getPredictedContactPoints2d())
         {
            FramePoint3D vertexInSoleFrame = new FramePoint3D(worldFrame, vertex);
            vertexInSoleFrame.changeFrame(currentFrame);

            currentPolygon.addVertex(vertexInSoleFrame);
         }
         currentPolygon.update();

         double currentArea = currentPolygon.getArea();
         double previousArea = previousPolygon.getArea();

         double currentWidth = currentPolygon.getBoundingBoxRangeY();
         double previousWidth = previousPolygon.getBoundingBoxRangeY();

         double percentAreaOnCurrentFoot = currentArea / (previousArea + currentArea);
         double percentWidthOnCurrentFoot = currentWidth / (currentWidth + previousWidth);

         double transferWeightDistributionFromArea = percentAreaOnCurrentFoot * parameters.getFractionLoadIfFootHasFullSupport();
         double transferWeightDistributionFromWidth = percentWidthOnCurrentFoot * parameters.getFractionLoadIfOtherFootHasNoWidth();

         // lower means it spends more time shifting to the center, higher means it spends less time shifting to the center
         // e.g., if we set the fraction to 0 and the trailing foot has no area, the split fraction should be 1 because we spend no time on the first segment
         double transferSplitFractionFromArea =  percentAreaOnCurrentFoot * (1.0 - parameters.getFractionTimeOnFootIfFootHasFullSupport());
         double transferSplitFractionFromWidth =  percentWidthOnCurrentFoot * (1.0 - parameters.getFractionTimeOnFootIfOtherFootHasNoWidth());

         double transferWeightDistribution = SplitFractionTools.appendWeightDistribution(transferWeightDistributionFromWidth,
                                                                                         transferWeightDistributionFromArea, defaultWeightDistribution);
         double transferSplitFraction = SplitFractionTools.appendSplitFraction(transferSplitFractionFromWidth, transferSplitFractionFromArea,
                                                                               defaultTransferSplitFraction);

         transferWeightDistribution = MathTools.clamp(transferWeightDistribution, 0.01, 0.99);
         transferSplitFraction = MathTools.clamp(transferSplitFraction, 0.01, 0.99);

         if (stepNumber == footstepDataMessageList.size() - 1)
         { // this is the last step
            double currentSplitFraction = processedPlan.getFootstepDataList().getFinalTransferSplitFraction();
            double currentWeightDistribution = processedPlan.getFootstepDataList().getFinalTransferWeightDistribution();

            double splitFractionToSet = SplitFractionTools.appendSplitFraction(transferSplitFraction, currentSplitFraction, defaultTransferSplitFraction);
            double weightDistributionToSet = SplitFractionTools.appendWeightDistribution(transferWeightDistribution, currentWeightDistribution, defaultWeightDistribution);

            processedPlan.getFootstepDataList().setFinalTransferSplitFraction(splitFractionToSet);
            processedPlan.getFootstepDataList().setFinalTransferWeightDistribution(weightDistributionToSet);
         }
         else
         {
            double currentSplitFraction = footstepDataMessageList.get(stepNumber + 1).getTransferSplitFraction();
            double currentWeightDistribution = footstepDataMessageList.get(stepNumber + 1).getTransferWeightDistribution();

            double splitFractionToSet = SplitFractionTools.appendSplitFraction(transferSplitFraction, currentSplitFraction, defaultTransferSplitFraction);
            double weightDistributionToSet = SplitFractionTools.appendWeightDistribution(transferWeightDistribution, currentWeightDistribution, defaultWeightDistribution);

            footstepDataMessageList.get(stepNumber + 1).setTransferSplitFraction(splitFractionToSet);
            footstepDataMessageList.get(stepNumber + 1).setTransferWeightDistribution(weightDistributionToSet);
         }
      }

      return processedPlan;
   }

   /** {@inheritDoc} **/
   @Override
   public PostProcessingEnum getElementName()
   {
      return PostProcessingEnum.AREA_SPLIT_FRACTIONS;
   }
}
