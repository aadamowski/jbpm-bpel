/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the JBPM BPEL PUBLIC LICENSE AGREEMENT as
 * published by JBoss Inc.; either version 1.0 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.jbpm.bpel.tutorial.loan;

import java.rmi.RemoteException;

import javax.naming.InitialContext;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;


import junit.framework.Test;
import junit.framework.TestCase;

import org.jbpm.bpel.tools.ModuleDeployTestSetup;
import org.jbpm.bpel.tutorial.task.TaskInfo;
import org.jbpm.bpel.tutorial.task.TaskList;
import org.jbpm.bpel.tutorial.task.TaskManagementService;
import org.jbpm.bpel.tutorial.task.TaskManager;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/06 22:06:29 $
 */
public class LoanerTest extends TestCase {

  private Loaner loaner;
  private TaskManager taskManager;

  protected void setUp() throws Exception {
    InitialContext ctx = new InitialContext();
    /*
     * "service/Loan" and "service/Task" are the JNDI names of the service interface instances
     * relative to the client environment context. These names match the <service-ref-name>s in
     * application-client.xml
     */
    LoanApprovalService loanService = (LoanApprovalService) ctx.lookup("java:comp/env/service/Loan");
    loaner = loanService.getLoanerPort();

    TaskManagementService taskService = (TaskManagementService) ctx.lookup("java:comp/env/service/TaskManagement");
    taskManager = taskService.getTaskManagerPort();
  }

  public void testRequestLoan_small() throws RemoteException {
    // request a small loan
    String borrower = "ernie";
    loaner.requestLoan(borrower, 900);

    // small loan is immediately approved
    assertEquals(LoanStatus.approved, loaner.getLoanStatus(borrower));
  }

  public void testRequestLoan_large() throws RemoteException, SOAPException {
    // request a large loan
    String borrower = "bert";
    loaner.requestLoan(borrower, 1100);

    // large loans are evaluated by agents
    assertEquals(LoanStatus.evaluating, loaner.getLoanStatus(borrower));

    // get the task list for agents
    TaskList taskList = taskManager.getTaskList("agent");
    TaskInfo[] taskInfos = taskList.getTaskInfo();
    assertEquals(1, taskInfos.length);

    // the agent rejects the loan
    SOAPElement statusElem = SOAPFactory.newInstance().createElement("loanStatus");
    statusElem.setValue(LoanStatus._rejected);

    // end the evaluation task
    TaskInfo taskInfo = taskInfos[0];
    taskInfo.set_any(new SOAPElement[] { statusElem });

    taskManager.endTask(taskInfo);

    // large loan was rejected :-(
    assertEquals(LoanStatus.rejected, loaner.getLoanStatus(borrower));
  }

  public static Test suite() {
    return new ModuleDeployTestSetup(LoanerTest.class, "loan-client.jar");
  }
}
