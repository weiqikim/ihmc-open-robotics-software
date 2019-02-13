package us.ihmc.quadrupedRobotics.controller.toolbox;

import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameQuaternion;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.quadrupedBasics.gait.QuadrupedStep;
import us.ihmc.quadrupedBasics.supportPolygon.QuadrupedSupportPolygon;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedFallDetectionParameters;
import us.ihmc.robotics.math.filters.GlitchFilteredYoBoolean;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.yoVariables.parameters.DoubleParameter;
import us.ihmc.yoVariables.parameters.IntegerParameter;
import us.ihmc.yoVariables.providers.BooleanProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;

public class QuadrupedFallDetector
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   public enum FallDetectionType
   {
      NONE, ROLL_LIMIT, PITCH_LIMIT, PITCH_AND_ROLL_LIMIT, HEIGHT_LIMIT, DCM_OUTSIDE_SUPPORT_POLYGON_LIMIT, ALL
   }

   // Parameters
   private static final int DEFAULT_FALL_GLITCH_WINDOW = 1;
   private final DoubleParameter maxPitchInRad;
   private final DoubleParameter maxRollInRad;
   private final DoubleParameter dcmOutsideSupportThreshold;
   private final DoubleParameter maxHeightError;
   private final IntegerParameter fallDetectorGlitchFilterWindow = new IntegerParameter("fallDetectorGlitchFilterWindow", registry, DEFAULT_FALL_GLITCH_WINDOW);

   private final FrameQuaternion bodyOrientation = new FrameQuaternion();

   private final ReferenceFrame bodyFrame;
   private final QuadrantDependentList<MovingReferenceFrame> soleFrames;

   private final FramePoint3D dcmPositionEstimate = new FramePoint3D();
   private final FramePoint2D dcmPositionEstimate2D = new FramePoint2D();
   private final DivergentComponentOfMotionEstimator dcmPositionEstimator;
   private final QuadrupedSupportPolygon supportPolygon = new QuadrupedSupportPolygon();
   private final QuadrupedSupportPolygon upcomingSupportPolygon = new QuadrupedSupportPolygon();

   private final QuadrantDependentList<YoBoolean> isUsingNextFootsteps = new QuadrantDependentList<>();
   private final QuadrantDependentList<FramePoint3D> nextFootstepPositions = new QuadrantDependentList<>();

   private final YoDouble desiredHeightForFallDetection = new YoDouble("desiredHeightForFallDetection", registry);
   private final YoDouble currentHeightForFallDetection = new YoDouble("currentHeightForFallDetection", registry);
   private final YoDouble heightErrorForFallDetection = new YoDouble("heightErrorForFallDetection", registry);

   private final YoDouble yoDcmDistanceOutsideSupportPolygon = new YoDouble("dcmDistanceOutsideSupportPolygon", registry);
   private final YoDouble yoDcmDistanceOutsideUpcomingPolygon = new YoDouble("dcmDistanceOutsideUpcomingPolygon", registry);
   private final YoEnum<FallDetectionType> fallDetectionType = YoEnum.create("fallDetectionType", FallDetectionType.class, registry);
   private final GlitchFilteredYoBoolean isFallDetected;

   public QuadrupedFallDetector(ReferenceFrame bodyFrame, QuadrantDependentList<MovingReferenceFrame> soleFrames,
                                DivergentComponentOfMotionEstimator dcmPositionEstimator, QuadrupedFallDetectionParameters fallDetectionParameters,
                                YoVariableRegistry parentRegistry)
   {
      this.bodyFrame = bodyFrame;
      this.soleFrames = soleFrames;
      this.fallDetectionType.set(fallDetectionParameters.getFallDetectionType());
      this.dcmPositionEstimator = dcmPositionEstimator;

      maxPitchInRad = new DoubleParameter("maxPitchInRad", registry, fallDetectionParameters.getMaxPitch());
      maxRollInRad = new DoubleParameter("maxRollInRad", registry, fallDetectionParameters.getMaxRoll());
      maxHeightError = new DoubleParameter("maxHeightError", registry, fallDetectionParameters.getMaxHeightError());
      dcmOutsideSupportThreshold = new DoubleParameter("dcmDistanceOutsideSupportPolygonSupportThreshold", registry, fallDetectionParameters.getIcpDistanceOutsideSupportPolygon());

      isFallDetected = new GlitchFilteredYoBoolean("isFallDetected", registry, DEFAULT_FALL_GLITCH_WINDOW);
      isFallDetected.set(false);

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         YoBoolean isUsingNextFootstep = new YoBoolean(robotQuadrant.getShortName() + "_IsFallDetectionUsingNextFootstep", registry);
         isUsingNextFootstep.set(false);
         isUsingNextFootsteps.put(robotQuadrant, isUsingNextFootstep);
         nextFootstepPositions.put(robotQuadrant, new FramePoint3D());
      }

      parentRegistry.addChild(registry);
   }

   public void setNextFootstep(RobotQuadrant robotQuadrant, QuadrupedStep timedFootstep)
   {
      boolean notNull = timedFootstep != null;
      isUsingNextFootsteps.get(robotQuadrant).set(notNull);

      if (notNull)
         timedFootstep.getGoalPosition(nextFootstepPositions.get(robotQuadrant));
   }

   public void setHeightForFallDetection(double desiredHeight, double currentHeight)
   {
      desiredHeightForFallDetection.set(desiredHeight);
      currentHeightForFallDetection.set(currentHeight);
   }

   public boolean detect()
   {
      updateEstimates();

      boolean isFallDetectedUnfiltered;
      switch (fallDetectionType.getEnumValue())
      {
      case DCM_OUTSIDE_SUPPORT_POLYGON_LIMIT:
         isFallDetectedUnfiltered = detectDcmDistanceOutsideSupportPolygonLimitFailure();
         break;
      case ROLL_LIMIT:
         isFallDetectedUnfiltered = detectRollLimitFailure();
         break;
      case PITCH_LIMIT:
         isFallDetectedUnfiltered = detectPitchLimitFailure();
         break;
      case PITCH_AND_ROLL_LIMIT:
         isFallDetectedUnfiltered = detectPitchLimitFailure() || detectRollLimitFailure();
         break;
      case HEIGHT_LIMIT:
         isFallDetectedUnfiltered = detectHeightLimitFailure();
         break;
      case ALL:
         isFallDetectedUnfiltered = detectDcmDistanceOutsideSupportPolygonLimitFailure() || detectPitchLimitFailure() || detectRollLimitFailure() || detectHeightLimitFailure();
         break;
      default:
         isFallDetectedUnfiltered = false;
         break;
      }
      isFallDetected.setWindowSize(fallDetectorGlitchFilterWindow.getValue());
      isFallDetected.update(isFallDetectedUnfiltered);
      if (isFallDetected.getBooleanValue())
         return true;
      else
         return false;
   }

   private void updateEstimates()
   {
      bodyOrientation.setToZero(bodyFrame);
      bodyOrientation.changeFrame(worldFrame);

      dcmPositionEstimator.getDCMPositionEstimate(dcmPositionEstimate);
      dcmPositionEstimate2D.set(dcmPositionEstimate);

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         supportPolygon.setFootstep(robotQuadrant, soleFrames.get(robotQuadrant));

         // update the upcoming position
         if (isUsingNextFootsteps.get(robotQuadrant).getBooleanValue())
            upcomingSupportPolygon.setFootstep(robotQuadrant, nextFootstepPositions.get(robotQuadrant));
         else
            upcomingSupportPolygon.setFootstep(robotQuadrant, soleFrames.get(robotQuadrant));
      }
   }

   private boolean detectPitchLimitFailure()
   {
      return Math.abs(bodyOrientation.getPitch()) > maxPitchInRad.getValue();
   }

   private boolean detectHeightLimitFailure()
   {
      heightErrorForFallDetection.set(desiredHeightForFallDetection.getDoubleValue() - currentHeightForFallDetection.getDoubleValue());
      return Math.abs(heightErrorForFallDetection.getDoubleValue()) > maxHeightError.getValue();
   }


   private boolean detectRollLimitFailure()
   {
      return Math.abs(bodyOrientation.getRoll()) > maxRollInRad.getValue();
   }

   private boolean detectDcmDistanceOutsideSupportPolygonLimitFailure()
   {
      double supportDistance = supportPolygon.distance(dcmPositionEstimate2D);
      double upcomingSupportDistance = upcomingSupportPolygon.distance(dcmPositionEstimate2D);

      yoDcmDistanceOutsideSupportPolygon.set(supportDistance);
      yoDcmDistanceOutsideUpcomingPolygon.set(upcomingSupportDistance);
      return supportDistance > dcmOutsideSupportThreshold.getValue() && upcomingSupportDistance > yoDcmDistanceOutsideUpcomingPolygon.getDoubleValue();
   }

   public BooleanProvider getIsFallDetected()
   {
      return isFallDetected;
   }
}
