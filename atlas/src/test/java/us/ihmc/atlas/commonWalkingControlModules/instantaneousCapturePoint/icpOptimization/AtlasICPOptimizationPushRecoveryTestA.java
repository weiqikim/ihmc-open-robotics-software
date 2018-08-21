package us.ihmc.atlas.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization;

import org.junit.Test;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.atlas.parameters.AtlasICPOptimizationParameters;
import us.ihmc.atlas.parameters.AtlasWalkingControllerParameters;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.commonWalkingControlModules.AvatarICPOptimizationPushRecoveryTestA;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.capturePoint.optimization.ICPOptimizationParameters;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

@ContinuousIntegrationPlan(categories = {IntegrationCategory.FAST})
public class AtlasICPOptimizationPushRecoveryTestA extends AvatarICPOptimizationPushRecoveryTestA
{
   @Override
   protected DRCRobotModel getRobotModel()
   {
      AtlasRobotModel atlasRobotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, RobotTarget.SCS, false)
      {
         @Override
         public WalkingControllerParameters getWalkingControllerParameters()
         {
            return new AtlasWalkingControllerParameters(RobotTarget.SCS, getJointMap(), getContactPointParameters())
            {
               @Override
               public boolean useOptimizationBasedICPController()
               {
                  return true;
               }

               @Override
               public ICPOptimizationParameters getICPOptimizationParameters()
               {
                  return new AtlasICPOptimizationParameters(false)
                  {
                     @Override
                     public boolean useAngularMomentum()
                     {
                        return true;
                     }

                     @Override
                     public boolean allowStepAdjustment()
                     {
                        return true;
                     }
                  };
               }
            };
         }
      };

      return atlasRobotModel;
   }

   @Override
   protected double getSizeScale()
   {
      return 1.0;
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 60.0)
   @Test(timeout = 150000)
   public void testPushICPOptimizationBackwardPushInSwing() throws SimulationExceededMaximumTimeException
   {
      percentWeight = 0.2;
      super.testPushICPOptimizationBackwardPushInSwing();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 60.0)
   @Test(timeout = 150000)
   public void testPushICPOptimizationForwardPushInSlowSwing() throws SimulationExceededMaximumTimeException
   {
      percentWeight = 0.2;
      super.testPushICPOptimizationForwardPushInSlowSwing();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 60.0)
   @Test(timeout = 150000)
   public void testPushICPOptimizationForwardPushInSwing() throws SimulationExceededMaximumTimeException
   {
      percentWeight = 0.29;
      super.testPushICPOptimizationForwardPushInSwing();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 60.0)
   @Test(timeout = 150000)
   public void testPushICPOptimizationInwardPushInSwing() throws SimulationExceededMaximumTimeException
   {
      percentWeight = 0.17;
      super.testPushICPOptimizationInwardPushInSwing();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 60.0)
   @Test(timeout = 150000)
   public void testPushICPOptimizationLongInwardPushInSwing() throws SimulationExceededMaximumTimeException
   {
      percentWeight = 0.05;
      super.testPushICPOptimizationLongInwardPushInSwing();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 60.0)
   @Test(timeout = 150000)
   public void testPushICPOptimizationOutwardPushInTransfer() throws SimulationExceededMaximumTimeException
   {
      percentWeight = 0.12;
      super.testPushICPOptimizationOutwardPushInTransfer();
   }

   @ContinuousIntegrationTest(estimatedDuration = 60.0)
   @Test(timeout = 150000)
   public void testPushICPOptimizationOutwardPushOnEachStep() throws SimulationExceededMaximumTimeException
   {
      percentWeight = 0.12;
      super.testPushICPOptimizationOutwardPushOnEachStep();
   }

   public static void main(String[] args)
   {
      AtlasICPOptimizationPushRecoveryTestA test = new AtlasICPOptimizationPushRecoveryTestA();
      try
      {
         test.testPushICPOptimizationOutwardPushOnEachStep();
      }
      catch (SimulationExceededMaximumTimeException e)
      {

      }
   }
}
