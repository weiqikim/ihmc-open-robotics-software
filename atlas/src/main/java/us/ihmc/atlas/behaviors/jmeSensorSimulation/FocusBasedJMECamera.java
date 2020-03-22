package us.ihmc.atlas.behaviors.jmeSensorSimulation;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.Axis;
import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.euclid.yawPitchRoll.YawPitchRoll;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;

public class FocusBasedJMECamera extends Camera
{
   private final Point3D focusPoint;
   private double latitude = Math.PI / 2.0;
   private double longitude = 0.0;
//   private YawPitchRoll yawPitchRoll = new YawPitchRoll();

//   private final Quaternion orientation;
   private final Vector3D offsetFromFocusPoint;

   private final PoseReferenceFrame zUpFrame = new PoseReferenceFrame("ZUpFrame", ReferenceFrame.getWorldFrame());
   private final FramePose3D cameraPose = new FramePose3D();
//   private final RigidBodyTransform transform = new RigidBodyTransform();
//   private final Point3D translation;

   private final Vector3f translationJME = new Vector3f();
   private final com.jme3.math.Quaternion orientationJME = new com.jme3.math.Quaternion();

   private boolean leftMousePressed = false;
   private boolean isWPressed = false;
   private boolean isAPressed = false;
   private boolean isSPressed = false;
   private boolean isDPressed = false;
   private boolean isQPressed = false;
   private boolean isZPressed = false;

   public FocusBasedJMECamera(int width, int height, InputManager inputManager)
   {
      super(width, height);

      RotationMatrix zUpToYUp = new RotationMatrix();
      zUpToYUp.set(0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0);
      zUpFrame.setOrientationAndUpdate(zUpToYUp);

      focusPoint = new Point3D(0.0, 0.0, -5.0);
//      translation = new Point3D(-2.0, 0.7, 1.0);
      offsetFromFocusPoint = new Vector3D(-10.0, 0.0, 0.0);
//      orientation = new Quaternion(0.0, -Math.PI / 2.0, 0.0);

//      focusPoint = new Vector3f(0.0f, 0.0f, 0.0f);
//      translation = new Vector3f(-2.0f, 0.7f, 1.0f);

      setFrustumPerspective(45.0f, (float) width / height, 1.0f, 1000.0f);

      updateCameraPose();

//      setLocation(translation);
//      lookAt(focusPoint, Vector3f.UNIT_Y);

      JMEInputMapperHelper inputMapper = new JMEInputMapperHelper(inputManager);
      inputMapper.addAnalogMapping("onMouseYUp", new MouseAxisTrigger(MouseInput.AXIS_Y, false), this::onMouseYUp);
      inputMapper.addAnalogMapping("onMouseYDown", new MouseAxisTrigger(MouseInput.AXIS_Y, true), this::onMouseYDown);
      inputMapper.addAnalogMapping("onMouseXLeft", new MouseAxisTrigger(MouseInput.AXIS_X, true), this::onMouseXLeft);
      inputMapper.addAnalogMapping("onMouseXRight", new MouseAxisTrigger(MouseInput.AXIS_X, false), this::onMouseXRight);
      inputMapper.addAnalogMapping("onMouseScrollUp", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false), this::onMouseScrollUp);
      inputMapper.addAnalogMapping("onMouseScrollDown", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true), this::onMouseScrollDown);
      inputMapper.addActionMapping("onMouseButtonLeft", new MouseButtonTrigger(MouseInput.BUTTON_LEFT), this::onMouseButtonLeft);
      inputMapper.addActionMapping("onMouseButtonRight", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT), this::onMouseButtonRight);
      inputMapper.addActionMapping("onKeyW", new KeyTrigger(KeyInput.KEY_W), this::onKeyW);
      inputMapper.addActionMapping("onKeyA", new KeyTrigger(KeyInput.KEY_A), this::onKeyA);
      inputMapper.addActionMapping("onKeyS", new KeyTrigger(KeyInput.KEY_S), this::onKeyS);
      inputMapper.addActionMapping("onKeyD", new KeyTrigger(KeyInput.KEY_D), this::onKeyD);
      inputMapper.addActionMapping("onKeyQ", new KeyTrigger(KeyInput.KEY_Q), this::onKeyQ);
      inputMapper.addActionMapping("onKeyZ", new KeyTrigger(KeyInput.KEY_Z), this::onKeyZ);
      inputMapper.build();
   }

   private void updateCameraPose()
   {
      Vector3D up = new Vector3D(0.0, 0.0, 1.0);
      Vector3D forward = new Vector3D(1.0, 0.0, 0.0);
      Vector3D left = new Vector3D();
      left.cross(up, forward);
      Vector3D down = new Vector3D();
      down.setAndNegate(up);
      Vector3D cameraZAxis = new Vector3D(forward);
      Vector3D cameraYAxis = new Vector3D(up);
      Vector3D cameraXAxis = new Vector3D();
      cameraXAxis.cross(cameraYAxis, cameraZAxis);
      RotationMatrix cameraOrientationOffset = new RotationMatrix();
      cameraOrientationOffset.setColumns(cameraXAxis, cameraYAxis, cameraZAxis);

//      up = new Vector3D(0.0, -1.0, 0.0);
//      forward = new Vector3D(1.0, 0.0, 0.0);
//      left = new Vector3D();
//      left.cross(up, forward);
//      rotationOffset.setColumns(forward, left, up);

      latitude = MathTools.clamp(latitude, Math.PI);
      longitude = EuclidCoreTools.trimAngleMinusPiToPi(longitude);
      double roll = 0.0;

//      yawPitchRoll.set(-latitude, -longitude, roll);

//      AxisAngle latitudeRotate = new AxisAngle(Axis.X, -latitude);
//      AxisAngle longitudeRotate = new AxisAngle(Axis.Y, -longitude);
//      AxisAngle rollRotate = new AxisAngle(Axis.Z, roll);
//      AxisAngle latitudeRotate = new AxisAngle(Axis.Y, -latitude);
//      AxisAngle longitudeRotate = new AxisAngle(Axis.X, -longitude);
//      AxisAngle rollRotate = new AxisAngle(Axis.Z, roll);
      AxisAngle latitudeRotate = new AxisAngle(Axis.Y, -latitude);
//      AxisAngle longitudeRotate = new AxisAngle(Axis.Z, -longitude);
      AxisAngle longitudeRotate = new AxisAngle(Axis.Z, 0.0);
      AxisAngle rollRotate = new AxisAngle(Axis.X, roll);

      RotationMatrix latLonOffset = new RotationMatrix();
//      rotationMatrix.append(rotationOffset);
      latLonOffset.append(latitudeRotate);
      latLonOffset.append(longitudeRotate);
      latLonOffset.append(rollRotate);

//      RigidBodyTransform transform = new RigidBodyTransform();
//      transform.

      cameraPose.setToZero(zUpFrame);
//      cameraPose.setPosition(focusPoint);
      cameraPose.appendTranslation(focusPoint);
      cameraPose.appendRotation(cameraOrientationOffset);
//      cameraPose.appendRotation(latLonOffset);
//      cameraPose.appendRotation(new YawPitchRoll(-longitude, -latitude, 0.0));
//      cameraPose.setOrientation(orientation);
      cameraPose.appendTranslation(offsetFromFocusPoint);

      cameraPose.changeFrame(ReferenceFrame.getWorldFrame());

//      System.out.println(focusPoint.toString());

      translationJME.set(cameraPose.getPosition().getX32(), cameraPose.getPosition().getY32(), cameraPose.getPosition().getZ32());
      orientationJME.set(cameraPose.getOrientation().getX32(),
                         cameraPose.getOrientation().getY32(),
                         cameraPose.getOrientation().getZ32(),
                         cameraPose.getOrientation().getS32());

//      System.out.println(translationJME.toString() + "   " + orientationJME.toString());

      setLocation(translationJME);
      setRotation(orientationJME);
   }

   public void simpleUpdate(float tpf)
   {
      if (isWPressed)
      {
         focusPoint.addX(tpf);
      }
      else if (isAPressed)
      {
         focusPoint.addY(tpf);
      }
      else if (isSPressed)
      {
         focusPoint.subX(tpf);
      }
      else if (isDPressed)
      {
         focusPoint.subY(tpf);
      }
      else if (isQPressed)
      {
         focusPoint.addZ(tpf);
      }
      else if (isZPressed)
      {
         focusPoint.subZ(tpf);
      }

      updateCameraPose();
   }

   private void onMouseYUp(float value, float tpf)
   {
      if (leftMousePressed)
      {
         latitude += tpf;
      }

      updateCameraPose();
   }

   private void onMouseYDown(float value, float tpf)
   {
      if (leftMousePressed)
      {
         latitude -= tpf;
      }

      updateCameraPose();
   }

   private void onMouseXLeft(float value, float tpf)
   {
      if (leftMousePressed)
      {
         longitude += tpf;
      }

      updateCameraPose();
   }

   private void onMouseXRight(float value, float tpf)
   {
      if (leftMousePressed)
      {
         longitude += tpf;
      }

      updateCameraPose();
   }

   private void onMouseScrollUp(float value, float tpf)
   {

   }

   private void onMouseScrollDown(float value, float tpf)
   {

   }

   private void onMouseButtonLeft(boolean isPressed, float tpf)
   {
      leftMousePressed = isPressed;
   }

   private void onMouseButtonRight(boolean isPressed, float tpf)
   {

   }

   private void onKeyW(boolean isPressed, float tpf)
   {
      isWPressed = isPressed;
   }

   private void onKeyA(boolean isPressed, float tpf)
   {
      isAPressed = isPressed;
   }

   private void onKeyS(boolean isPressed, float tpf)
   {
      isSPressed = isPressed;
   }

   private void onKeyD(boolean isPressed, float tpf)
   {
      isDPressed = isPressed;
   }

   private void onKeyQ(boolean isPressed, float tpf)
   {
      isQPressed = isPressed;
   }

   private void onKeyZ(boolean isPressed, float tpf)
   {
      isZPressed = isPressed;
   }
}
