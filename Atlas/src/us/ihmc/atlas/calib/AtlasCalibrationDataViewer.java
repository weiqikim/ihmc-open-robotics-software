package us.ihmc.atlas.calib;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.zip.ZipFile;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearanceRGBColor;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.humanoidRobot.partNames.LimbName;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFramePose;

import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicCoordinateSystem;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;

public class AtlasCalibrationDataViewer extends AtlasKinematicCalibrator
{
   //YoVariables for Display
   private final YoFramePoint ypLeftEE, ypRightEE;
   private final YoFramePose yposeLeftEE, yposeRightEE;
   Map<String, DoubleYoVariable> yoQout = new HashMap<>();
   Map<String, DoubleYoVariable> yoQdiff = new HashMap<>();

   public AtlasCalibrationDataViewer(DRCRobotModel robotModel)
   {
      super(robotModel);
      ypLeftEE = new YoFramePoint("leftEE", ReferenceFrame.getWorldFrame(), registry);
      ypRightEE = new YoFramePoint("rightEE", ReferenceFrame.getWorldFrame(), registry);
      yposeLeftEE = new YoFramePose("leftPoseEE", "", ReferenceFrame.getWorldFrame(), registry);
      yposeRightEE = new YoFramePose("rightPoseEE", "", ReferenceFrame.getWorldFrame(), registry);
   }

   @Override
   protected void setupDynamicGraphicObjects()
   {
      double transparency = 0.5;
      double scale = 0.02;
      DynamicGraphicPosition dgpLeftEE = new DynamicGraphicPosition("dgpLeftEE", ypLeftEE, scale, new YoAppearanceRGBColor(Color.BLUE, transparency));
      DynamicGraphicPosition dgpRightEE = new DynamicGraphicPosition("dgpRightEE", ypRightEE, scale, new YoAppearanceRGBColor(Color.RED, transparency));

      scs.addDynamicGraphicObject(dgpLeftEE);
      scs.addDynamicGraphicObject(dgpRightEE);

      DynamicGraphicCoordinateSystem dgPoseLeftEE = new DynamicGraphicCoordinateSystem("dgposeLeftEE", yposeLeftEE, 5 * scale);
      DynamicGraphicCoordinateSystem dgPoseRightEE = new DynamicGraphicCoordinateSystem("dgposeRightEE", yposeRightEE, 5 * scale);
      scs.addDynamicGraphicObject(dgPoseLeftEE);
      scs.addDynamicGraphicObject(dgPoseRightEE);
   }

   @Override
   protected void updateDynamicGraphicsObjects(int index)
   {
      FramePoint leftEE = new FramePoint(fullRobotModel.getEndEffectorFrame(RobotSide.LEFT, LimbName.ARM), 0, 0.13, 0);
      FramePoint rightEE = new FramePoint(fullRobotModel.getEndEffectorFrame(RobotSide.RIGHT, LimbName.ARM), 0, -0.13, 0);
      
      leftEE.changeFrame(CalibUtil.world);
      rightEE.changeFrame(CalibUtil.world);

      ypLeftEE.set(leftEE);
      ypRightEE.set(rightEE);

      yposeLeftEE.set(leftEE, new FrameOrientation(CalibUtil.world));
      yposeRightEE.set(rightEE, new FrameOrientation(CalibUtil.world));
   }

   public void createQoutYoVariables()
   {
      Map<String, Double> qout0 = (Map) qout.get(0);
      for (String jointName : qout0.keySet())
      {
         yoQout.put(jointName, new DoubleYoVariable("qout_" + jointName, registry));
         yoQdiff.put(jointName, new DoubleYoVariable("qdiff_" + jointName, registry));
      }

   }

   public void updateQoutYoVariables(int index)
   {
      for (String jointName : qout.get(0).keySet())
      {
         yoQout.get(jointName).set((double) qout.get(index).get(jointName));
         yoQdiff.get(jointName).set((double) qout.get(index).get(jointName) - (double) q.get(index).get(jointName));
      }

   }

   public void loadData(String calib_file)
   {

      BufferedReader reader = null;
      try
      {
         if (calib_file.contains("zip"))
         {
            ZipFile zip = new ZipFile(calib_file);
            reader = new BufferedReader(new InputStreamReader(zip.getInputStream(zip.getEntries().nextElement())));
         } else
            reader = new BufferedReader(new FileReader(calib_file));
      } catch (IOException e1)
      {
         System.out.println("Cannot load calibration file " + calib_file);
         e1.printStackTrace();
      }

      String line;
      final int numJoints = 28;
      System.out.println("total joints should be " + numJoints);
      try
      {
         while ((line = reader.readLine()) != null)
         {
            if (line.matches("^entry.*"))
            {
               Map<String, Double> q_ = new HashMap<>();
               Map<String, Double> qout_ = new HashMap<>();

               for (int i = 0; i < numJoints; i++)
               {
                  line = reader.readLine();
                  if (line != null)
                  {
                     String[] items = line.split("\\s");

                     if (items[0].equals("neck_ay"))
                        items[0] = new String("neck_ry");
                     q_.put(items[0], new Double(items[1]));
                     qout_.put(items[0], new Double(items[2]));
                  } else
                  {
                     System.out.println("One ill-formed data entry");
                     break;
                  }

               }

               if (q_.size() == numJoints)
                  q.add((Map) q_);
               if (qout_.size() == numJoints)
                  qout.add((Map) qout_);
            }
         }
      } catch (IOException e1)
      {
         System.err.println("File reading error");
         e1.printStackTrace();
      }
      System.out.println("total entry loaded q/qout " + q.size() + "/" + qout.size());
   }

   /**
    * @param args
    */
   public static void main(String[] args)
   {
	  final AtlasRobotVersion ATLAS_ROBOT_VERSION = AtlasRobotVersion.DRC_NO_HANDS;
	  final boolean RUNNING_ON_REAL_ROBOT = false;
	  
	  DRCRobotModel robotModel = new AtlasRobotModel(ATLAS_ROBOT_VERSION, RUNNING_ON_REAL_ROBOT, RUNNING_ON_REAL_ROBOT);
	  
      AtlasWristLoopKinematicCalibrator calib = new AtlasWristLoopKinematicCalibrator(robotModel);
      calib.loadData("data/manip_motions/log4.zip");
      calib.createQoutYoVariables();

      calib.createDisplay(calib.q.size());
      

      Map<String, Double> q0 = new HashMap<String, Double>();
      
      for(String key: calib.q.get(0).keySet())
         q0.put(key,new Double(0));

      
      CalibUtil.setRobotModelFromData(calib.fullRobotModel,q0);
      return;
//    
//      for(int i=0;i<calib.q.size();i++)
//      {
//         CalibUtil.setRobotModelFromData(calib.fullRobotModel, (Map)calib.q.get(i));
//         calib.updateQoutYoVariables(i);
//         calib.displayUpdate(i);
//      }

   }

}
