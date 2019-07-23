package us.ihmc.pathPlanning.visibilityGraphs.ui.graphics;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import us.ihmc.commons.FormattingTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.javaFXToolkit.shapes.JavaFXMeshBuilder;
import us.ihmc.javaFXVisualizers.JavaFXGraphicTools;
import us.ihmc.javafx.graphics.LabelGraphic;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PlanarRegionTools;
import us.ihmc.pathPlanning.visibilityGraphs.ui.VisualizationParameters;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlanarRegionsGraphic extends Group
{
   private static final PlanarRegionColorPicker colorPicker = new PlanarRegionColorPicker();

   private volatile List<Node> regionNodes;
   private List<Node> lastNodes = null; // optimization

   private Object regionMeshAddSync = new Object(); // for parallel mesh builder
   private volatile List<Node> updateRegionMeshViews; // for parallel mesh builder

   // visualization options
   private boolean drawAreaText = false;
   private boolean drawBoundingBox = false;
   private boolean drawNormal;

   public PlanarRegionsGraphic()
   {
      this(true);
   }

   public PlanarRegionsGraphic(boolean initializeToFlatGround)
   {
      PlanarRegionsList planarRegionsList;
      if (initializeToFlatGround)
      {
         ConvexPolygon2D convexPolygon = new ConvexPolygon2D();  // start with a flat ground region
         convexPolygon.addVertex(10.0, 10.0);
         convexPolygon.addVertex(-10.0, 10.0);
         convexPolygon.addVertex(-10.0, -10.0);
         convexPolygon.addVertex(10.0, -10.0);
         convexPolygon.update();
         PlanarRegion groundPlane = new PlanarRegion(new RigidBodyTransform(), convexPolygon);
         planarRegionsList = new PlanarRegionsList(groundPlane);
      }
      else
      {
         planarRegionsList = new PlanarRegionsList();
      }

      generateMeshes(planarRegionsList);
   }

   public void generateMeshesAsync(PlanarRegionsList planarRegionsList)
   {
      ThreadTools.startAThread(() -> generateMeshes(planarRegionsList), "MeshGeneration");
   }

   public synchronized void generateMeshes(PlanarRegionsList planarRegionsList)
   {
      updateRegionMeshViews = new ArrayList<>();

      planarRegionsList.getPlanarRegionsAsList().parallelStream().forEach(this::parallelMeshBuilder);

      regionNodes = updateRegionMeshViews; // volatile set
   }

   private void parallelMeshBuilder(PlanarRegion planarRegion)
   {
      JavaFXMeshBuilder meshBuilder = new JavaFXMeshBuilder();

      RigidBodyTransform transformToWorld = PlanarRegionTools.getTransformToWorld(planarRegion);

      meshBuilder.addMultiLine(transformToWorld, Arrays.asList(planarRegion.getConcaveHull()), VisualizationParameters.CONCAVEHULL_LINE_THICKNESS, true);

      double totalArea = 0.0;
      for (ConvexPolygon2D convexPolygon : planarRegion.getConvexPolygons())
      {
         meshBuilder.addPolygon(transformToWorld, convexPolygon);

         totalArea += convexPolygon.getArea();
      }

      LabelGraphic sizeLabel = null;
      if (drawAreaText)
      {
         sizeLabel = new LabelGraphic(FormattingTools.getFormattedToSignificantFigures(totalArea, 3));
         sizeLabel.getPose().appendTransform(transformToWorld);
         sizeLabel.update();
      }

      if (drawBoundingBox)
      {
         JavaFXGraphicTools.drawBoxEdges(meshBuilder, PlanarRegionTools.getLocalBoundingBox3DInWorld(planarRegion, 0.1), 0.005);
      }

      if (drawNormal)
      {
         Vector3D normal = planarRegion.getNormal();
         normal.normalize();

         Point3D centroid = PlanarRegionTools.getAverageCentroid3DInWorld(planarRegion);

         double length = 0.07;
         double radius = 0.004;
         double cylinderToConeLengthRatio = 0.8;
         double coneDiameterMultiplier = 1.8;
         JavaFXGraphicTools.drawArrow(meshBuilder,
                                      centroid,
                                      transformToWorld.getRotation(),
                                      length,
                                      radius,
                                      cylinderToConeLengthRatio,
                                      coneDiameterMultiplier);
      }

      MeshView regionMeshView = new MeshView(meshBuilder.generateMesh());
      regionMeshView.setMaterial(new PhongMaterial(getRegionColor(planarRegion.getRegionId())));

      synchronized (regionMeshAddSync)
      {
         if (drawAreaText) updateRegionMeshViews.add(sizeLabel.getNode());
         updateRegionMeshViews.add(regionMeshView);
      }
   }

   public void update()
   {
      List<Node> meshViews = regionNodes;  // volatile get
      if (lastNodes != meshViews) // optimization
      {
         getChildren().clear();
         getChildren().addAll(meshViews);
         lastNodes = meshViews;
      }
   }

   public void setDrawAreaText(boolean drawAreaText)
   {
      this.drawAreaText = drawAreaText;
   }

   public void setDrawBoundingBox(boolean drawBoundingBox)
   {
      this.drawBoundingBox = drawBoundingBox;
   }

   public void setDrawNormal(boolean drawNormal)
   {
      this.drawNormal = drawNormal;
   }

   public static Color getRegionColor(int regionId)
   {
      return getRegionColor(regionId, 1.0);
   }

   public static Color getRegionColor(int regionId, double opacity)
   {
      java.awt.Color awtColor = colorPicker.getColor(regionId);
      return Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), opacity);
   }

   /**
    * Keeps a list N of good colors to render planar regions. Region i is given color i mod N
    */
   private static class PlanarRegionColorPicker
   {
      private final ArrayList<java.awt.Color> colors = new ArrayList<>();

      PlanarRegionColorPicker()
      {
         colors.add(new java.awt.Color(104, 130, 219));
         colors.add(new java.awt.Color(113, 168, 133));
         colors.add(new java.awt.Color(196, 182, 90));
         colors.add(new java.awt.Color(190, 89, 110));
         colors.add(new java.awt.Color(155, 80, 190));
      }

      java.awt.Color getColor(int regionId)
      {
         return colors.get(Math.abs(regionId % colors.size()));
      }
   }
}