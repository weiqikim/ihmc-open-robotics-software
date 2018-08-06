package us.ihmc.footstepPlanning.roughTerrainPlanning;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.footstepPlanning.FootstepPlanner;
import us.ihmc.footstepPlanning.tools.PlannerTools;
import us.ihmc.footstepPlanning.graphSearch.aStar.FootstepNodeVisualization;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.ParameterBasedNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.planners.AStarFootstepPlanner;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

@ContinuousIntegrationPlan(categories = IntegrationCategory.FAST)
public class AStarOnRoughTerrainTest extends FootstepPlannerOnRoughTerrainTest
{
   private static final boolean visualizePlanner = false;
   private AStarFootstepPlanner planner;
   private FootstepNodeVisualization visualization = null;

   private static boolean keepUp = false;



   @Override
   public boolean assertPlannerReturnedResult()
   {
      return true;
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 2.5)
   @Test(timeout = 100000)
   public void testDownCorridor()
   {
      super.testDownCorridor();
   }

   @Override
   @Ignore
   @ContinuousIntegrationTest(estimatedDuration = 10.2, categoriesOverride = {IntegrationCategory.EXCLUDE})
   @Test(timeout = 51000)
   public void testPartialGaps()
   {
      super.testPartialGaps();
   }

   @Override
   @Ignore
   @ContinuousIntegrationTest(estimatedDuration = 10.2, categoriesOverride = {IntegrationCategory.EXCLUDE})
   @Test(timeout = 51000)
   public void testSpiralStaircase()
   {
      super.testSpiralStaircase();
   }

   @After
   public void destroyPlanner()
   {
      planner = null;

      if (visualizePlanner)
      {
         for (int i = 0; i < 1000; i++)
            visualization.tickAndUpdate();
         visualization.showAndSleep(true);
      }
   }

   @Override
   public FootstepPlanner getPlanner()
   {
      return planner;
   }

   @Override
   public boolean visualize()
   {
      if (visualizePlanner)
         return false;
      return visualize;
   }

   @Override
   public boolean keepUp()
   {
      return keepUp;
   }

   @Override
   public void setupInternal()
   {
      if (visualizePlanner)
         visualization = new FootstepNodeVisualization(1000, 1.0, null);
      SideDependentList<ConvexPolygon2D> footPolygons = PlannerTools.createDefaultFootPolygons();
      ParameterBasedNodeExpansion expansion = new ParameterBasedNodeExpansion(getPlannerParameters());
      planner = AStarFootstepPlanner
            .createRoughTerrainPlanner(getPlannerParameters(), visualization, footPolygons, expansion, new YoVariableRegistry("TestRegistry"));
   }

   @Override
   public void destroyInternal()
   {
      planner = null;
   }
}
