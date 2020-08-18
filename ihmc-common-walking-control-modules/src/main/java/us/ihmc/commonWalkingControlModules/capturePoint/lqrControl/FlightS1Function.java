package us.ihmc.commonWalkingControlModules.capturePoint.lqrControl;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import us.ihmc.matrixlib.MatrixTools;
import us.ihmc.matrixlib.NativeCommonOps;

public class FlightS1Function implements S1Function
{
   private final DMatrixRMaj Afl = new DMatrixRMaj(6, 6);

   private final DMatrixRMaj finalS1 = new DMatrixRMaj(6, 6);
   private static final DMatrixRMaj identity = CommonOps_DDRM.identity(3);

   public FlightS1Function()
   {
      MatrixTools.setMatrixBlock(Afl, 0, 0, identity, 0, 0, 3, 3, 1.0);
      MatrixTools.setMatrixBlock(Afl, 3, 3, identity, 0, 0, 3, 3, 1.0);
   }


   public void set( DMatrixRMaj finalS1)
   {
      this.finalS1.set(finalS1);
   }

   public void compute(double timeInState, DMatrixRMaj S1ToPack)
   {
      MatrixTools.setMatrixBlock(Afl, 3, 3, identity, 0, 0, 3, 3, timeInState);
      NativeCommonOps.multQuad(Afl, finalS1, S1ToPack);
   }
}
