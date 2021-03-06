package us.ihmc.tools.dataSampling;

import static us.ihmc.robotics.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
public class DataSamplerTest
{
   private static double epsilon = 1e-10;
   
   DataSampler<Double> dataSampler;
   
   String[] variableNames = {"var1", "var2", "var3"};
   double [] variableFirstValues = {1.5, 2.5, 3.5};
   double [] variableSecondValues = {1.6, 1.6, 3.6};
   double [] variableThirdValues = {1.7, 2.7, 3.7};
   int initialTime = 1;
   int numberOfSamples = 10;
   double timeInterval = 20.5;
   HashMap<String, Double> firstData = createFirstData();
   HashMap<String, Double> secondData = createSecondData();
   HashMap<String, Double> thirdData = createThirdData();
   
   @BeforeEach
   public void createDataSampler()
   {
      dataSampler = new DataSampler<Double>();
   }

	@Test
   public void testInitialization()
   {
      dataSampler.initialize(variableNames, initialTime, timeInterval, numberOfSamples);
      double detaTimeFromSampler = dataSampler.getDeltaTime();
      assertEquals(2.05, detaTimeFromSampler, epsilon);
      
      ArrayList<Double> array = dataSampler.getSingleVariableSamples(variableNames[0]);
      assertNotNull(array);
      
      ArrayList<Double> array2 = dataSampler.getSingleVariableSamples(variableNames[1]);
      assertNotNull(array2);
   }

	@Test
   public void testSampling()
   {
      dataSampler.initialize(variableNames, initialTime, timeInterval, numberOfSamples);
      dataSampler.acquire(firstData.get("time"), firstData);
      dataSampler.acquire(secondData.get("time"), secondData);
      dataSampler.acquire(thirdData.get("time"), thirdData);
      
      ArrayList<Double> array = dataSampler.getSingleVariableSamples(variableNames[0]);
      assertEquals(variableFirstValues[0], array.get(0), epsilon);
      assertEquals(variableThirdValues[0], array.get(1), epsilon);
   }
   
   // local methods
   
   private HashMap<String, Double> createFirstData()
   {
      HashMap<String, Double> data = new HashMap<String, Double>();
      for(int i = 0; i < variableNames.length; i++)
      {
         data.put(variableNames[i], variableFirstValues[i]);
      }
      
      data.put("time", 1.1);
      
      return data;
   }
   
   private HashMap<String, Double> createSecondData()
   {
      HashMap<String, Double> data = new HashMap<String, Double>();
      for(int i = 0; i < variableNames.length; i++)
      {
         data.put(variableNames[i], variableSecondValues[i]);
      }
      
      data.put("time", 1.2);
      
      return data;
   }
   
   private HashMap<String, Double> createThirdData()
   {
      HashMap<String, Double> data = new HashMap<String, Double>();
      for(int i = 0; i < variableNames.length; i++)
      {
         data.put(variableNames[i], variableThirdValues[i]);
      }
      
      data.put("time", 4.0);
      
      return data;
   }

}
