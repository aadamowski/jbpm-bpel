package org.jbpm.bpel.tutorial.purchase;

import java.rmi.RemoteException;

import javax.naming.InitialContext;

import junit.framework.Test;
import junit.framework.TestCase;

import org.jbpm.bpel.tools.ModuleDeployTestSetup;

/**
 * Test for common order purchasing scenarios.
 * @author Jeff DeLong
 * @author Alejandro Guizar
 * @version $Revision$
 */
public class PurchaseOrderTest extends TestCase {

  private PurchaseOrderPT purchaseOrderPT;

  protected void setUp() throws Exception {
    InitialContext ctx = new InitialContext();
    /*
     * "service/PurchaseOrder" is the JNDI name of the service interface instance relative to the
     * client environment context. This name matches the <service-ref-name> in
     * application-client.xml
     */
    PurchaseOrderService service = (PurchaseOrderService) ctx.lookup("java:comp/env/service/PurchaseOrder");
    purchaseOrderPT = service.getPurchaseServicePort();
  }

  public void testSendPurchaseOrderAvailable() throws RemoteException {
    CustomerInfo customerInfo = new CustomerInfo();
    customerInfo.setCustomerId("manager");
    customerInfo.setAddress("123 Main St");

    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setOrderId(10);
    purchaseOrder.setPartNumber(23);
    purchaseOrder.setQuantity(4);

    try {
      Invoice invoice = purchaseOrderPT.sendPurchaseOrder(customerInfo, purchaseOrder);
      /*
       * In our system, the part number is also the unit price! The shipper charges a flat fare of
       * $10.95.
       */
      assertEquals(purchaseOrder.getPartNumber() * purchaseOrder.getQuantity() + 10.95,
          invoice.getAmount(), 0.001);
      assertEquals(purchaseOrder.getOrderId(), invoice.getOrderId());
    }
    catch (ProblemInfo e) {
      fail("shipping to available address should complete");
    }
  }

  public void testSendPurchaseOrderNotAvailable() throws RemoteException {
    CustomerInfo customerInfo = new CustomerInfo();
    customerInfo.setCustomerId("freddy");
    customerInfo.setAddress("666 Elm St");

    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setOrderId(20);
    purchaseOrder.setPartNumber(13);
    purchaseOrder.setQuantity(7);

    try {
      purchaseOrderPT.sendPurchaseOrder(customerInfo, purchaseOrder);
      fail("shipping to unavailable address should not complete");
    }
    catch (ProblemInfo e) {
      assertTrue(e.getDetail().indexOf(customerInfo.getAddress()) != -1);
    }
  }

  public static Test suite() {
    return new ModuleDeployTestSetup(PurchaseOrderTest.class, new String[] { "purchase-ejb.jar",
        "purchase-client.jar" });
  }
}
