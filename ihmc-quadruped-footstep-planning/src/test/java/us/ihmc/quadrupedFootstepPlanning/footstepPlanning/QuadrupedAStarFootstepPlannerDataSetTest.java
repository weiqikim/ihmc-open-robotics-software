package us.ihmc.quadrupedFootstepPlanning.footstepPlanning;

import org.junit.jupiter.api.Test;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.QuadrupedAStarFootstepPlanner;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.nodeExpansion.FootstepNodeExpansion;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.nodeExpansion.ParameterBasedNodeExpansion;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.DefaultFootstepPlannerParameters;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.quadrupedPlanning.QuadrupedGait;
import us.ihmc.quadrupedPlanning.QuadrupedSpeed;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettings;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettingsReadOnly;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class QuadrupedAStarFootstepPlannerDataSetTest extends FootstepPlannerDataSetTest
{
   public FootstepPlannerType getPlannerType()
   {
      return FootstepPlannerType.A_STAR;
   }

   public QuadrupedXGaitSettingsReadOnly getXGaitSettings()
   {
      QuadrupedXGaitSettings settings = new QuadrupedXGaitSettings();
      settings.setStanceLength(1.0);
      settings.setStanceWidth(0.5);
      settings.setQuadrupedSpeed(QuadrupedSpeed.MEDIUM);
      settings.setEndPhaseShift(QuadrupedGait.AMBLE.getEndPhaseShift());
      settings.getAmbleMediumTimings().setEndDoubleSupportDuration(0.25);
      settings.getAmbleMediumTimings().setStepDuration(0.5);
      return settings;
   }

   @Override
   public QuadrupedBodyPathAndFootstepPlanner createPlanner()
   {
      YoVariableRegistry registry = new YoVariableRegistry("test");
      QuadrupedXGaitSettingsReadOnly xGaitSettings = getXGaitSettings();
      FootstepPlannerParameters parameters = new DefaultFootstepPlannerParameters()
      {
         @Override
         public double getMaximumStepReach()
         {
            return 0.7;
         }

         public double getMaximumStepLength()
         {
            return 0.6;
         }

         @Override
         public double getMinimumStepLength()
         {
            return -0.3;
         }

         @Override
         public double getMinimumStepWidth()
         {
            return -0.3;
         }

         @Override
         public double getMaximumStepWidth()
         {
            return 0.35;
         }
      };
      FootstepNodeExpansion expansion = new ParameterBasedNodeExpansion(parameters, xGaitSettings);
      AStarMessagerListener listener = new AStarMessagerListener(messager);

      return QuadrupedAStarFootstepPlanner.createPlanner(parameters, xGaitSettings, listener, expansion, registry);
   }

   @Override
   @Test
   public void testDatasetsWithoutOcclusion()
   {
      super.testDatasetsWithoutOcclusion();
   }

   public static void main(String[] args)
   {
      QuadrupedAStarFootstepPlannerDataSetTest test = new QuadrupedAStarFootstepPlannerDataSetTest();
      VISUALIZE = true;
      test.setup();
//      test.runAssertionsOnDataset(dataset -> test.runAssertions(dataset), "20171216_111326_CrossoverPlatforms");
//      test.runAssertionsOnDataset(dataset -> test.runAssertions(dataset), "20171218_204917_FlatGround");
      test.runAssertionsOnDataset(dataset -> test.runAssertions(dataset), "20171218_204953_FlatGroundWithWall");
//      if (activelyVisualize)
//         test.visualizer.showAndSleep(true);
      ThreadTools.sleepForever();
   }
}
