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
package org.jbpm.bpel.persistence.db;

import java.util.Timer;
import java.util.TimerTask;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmContextTestHelper;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.db.ContextSession;
import org.jbpm.db.GraphSession;
import org.jbpm.db.JobSession;
import org.jbpm.db.LoggingSession;
import org.jbpm.db.TaskMgmtSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Job;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class AbstractDbTestCase extends TestCase {

  private static final long serialVersionUID = 1L;

  protected static JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance();

  protected JbpmContext jbpmContext;
  protected SchemaExport schemaExport;

  protected Session session;
  protected GraphSession graphSession;
  protected TaskMgmtSession taskMgmtSession;
  protected ContextSession contextSession;
  protected JobSession jobSession;
  protected LoggingSession loggingSession;

  protected BpelGraphSession bpelGraphSession;

  protected JobExecutor jobExecutor;

  protected void setUp() throws Exception {
    /*
     * resetting JbpmConfiguration causes the instance retrieved above to be discarded from the
     * JbpmConfiguration.instances map and MUST NOT be called
     */
    // JbpmConfigurationTestHelper.reset();
    // contexts stack is empty at this point, no need to create it again
    // JbpmContextTestHelper.reset();
    // hibernate is configured to create schema automatically
    // createSchema();
    beginSessionTransaction();

    log.debug("### starting " + getName() + " ####################################################");
  }

  protected void tearDown() throws Exception {
    log.debug("### " + getName() + " done ####################################################");

    commitAndCloseSession();
    // hibernate creates schema automatically, no need to drop it between cases
    // dropSchema();
    /*
     * resetting JbpmConfiguration causes the instance retrieved above to be discarded from the
     * JbpmConfiguration.instances map and MUST NOT be called
     */
    // JbpmConfigurationTestHelper.reset();
    JbpmContextTestHelper.reset();
  }

  public void beginSessionTransaction() {
    createJbpmContext();
    initializeMembers();
  }

  public void commitAndCloseSession() {
    closeJbpmContext();
    resetMembers();
  }

  protected void newTransaction() {
    DbPersistenceService persistenceService = (DbPersistenceService) jbpmContext.getServices()
        .getPersistenceService();
    persistenceService.endTransaction();
    persistenceService.beginTransaction();
  }

  public ProcessInstance saveAndReload(ProcessInstance pi) {
    jbpmContext.save(pi);
    newTransaction();
    return graphSession.loadProcessInstance(pi.getId());
  }

  public TaskInstance saveAndReload(TaskInstance taskInstance) {
    jbpmContext.save(taskInstance);
    newTransaction();
    return (TaskInstance) session.load(TaskInstance.class, new Long(taskInstance.getId()));
  }

  public ProcessDefinition saveAndReload(ProcessDefinition pd) {
    graphSession.saveProcessDefinition(pd);
    newTransaction();
    return graphSession.loadProcessDefinition(pd.getId());
  }

  public BpelProcessDefinition saveAndReload(BpelProcessDefinition pd) {
    graphSession.saveProcessDefinition(pd);
    newTransaction();
    return (BpelProcessDefinition) session.load(BpelProcessDefinition.class, new Long(pd.getId()));
  }

  public ProcessLog saveAndReload(ProcessLog processLog) {
    loggingSession.saveProcessLog(processLog);
    newTransaction();
    return loggingSession.loadProcessLog(processLog.getId());
  }

  protected void createSchema() {
    getJbpmConfiguration().createSchema();
  }

  protected JbpmConfiguration getJbpmConfiguration() {
    return jbpmConfiguration;
  }

  protected void dropSchema() {
    getJbpmConfiguration().dropSchema();
  }

  protected void createJbpmContext() {
    jbpmContext = getJbpmConfiguration().createJbpmContext();
  }

  protected void closeJbpmContext() {
    jbpmContext.close();
  }

  protected void startJobExecutor() {
    jobExecutor = jbpmConfiguration.getJobExecutor();
    jobExecutor.start();
  }

  private void processAllJobs(final long maxWait) {
    boolean jobsAvailable = true;

    // install a timer that will interrupt if it takes too long
    // if that happens, it will lead to an interrupted exception and the test
    // will fail
    TimerTask interruptTask = new TimerTask() {

      Thread testThread = Thread.currentThread();

      public void run() {
        log.debug("test " + getName() + " took too long. going to interrupt...");
        testThread.interrupt();
      }
    };
    Timer timer = new Timer();
    timer.schedule(interruptTask, maxWait);

    try {
      while (jobsAvailable) {
        log.debug("going to sleep for 200 millis, waiting for the job executor to process more jobs");
        Thread.sleep(200);
        jobsAvailable = areJobsAvailable();
      }
      jobExecutor.stopAndJoin();
    }
    catch (InterruptedException e) {
      fail("test execution exceeded treshold of " + maxWait + " milliseconds");
    }
    finally {
      timer.cancel();
    }
  }

  protected int getNbrOfJobsAvailable() {
    if (session != null)
      return getNbrOfJobsAvailable(session);

    beginSessionTransaction();
    try {
      return getNbrOfJobsAvailable(session);
    }
    finally {
      commitAndCloseSession();
    }
  }

  private int getNbrOfJobsAvailable(Session session) {
    int nbrOfJobsAvailable = 0;
    Number jobs = (Number) session.createQuery("select count(*) from org.jbpm.job.Job")
        .uniqueResult();
    log.debug("there are '" + jobs + "' jobs currently in the job table");
    if (jobs != null) {
      nbrOfJobsAvailable = jobs.intValue();
    }
    return nbrOfJobsAvailable;
  }

  protected boolean areJobsAvailable() {
    return (getNbrOfJobsAvailable() > 0);
  }

  protected Job getJob() {
    return (Job) session.createQuery("from org.jbpm.job.Job").uniqueResult();
  }

  protected void processJobs(long maxWait) {
    commitAndCloseSession();
    try {
      Thread.sleep(300);
    }
    catch (InterruptedException e) {
    }
    startJobExecutor();
    try {
      processAllJobs(maxWait);
    }
    finally {
      stopJobExecutor();
      beginSessionTransaction();
    }
  }

  protected void stopJobExecutor() {
    if (jobExecutor != null) {
      try {
        jobExecutor.stopAndJoin();
      }
      catch (InterruptedException e) {
      }
      jobExecutor = null;
    }
  }

  protected void initializeMembers() {
    session = jbpmContext.getSession();
    graphSession = jbpmContext.getGraphSession();
    taskMgmtSession = jbpmContext.getTaskMgmtSession();
    loggingSession = jbpmContext.getLoggingSession();
    jobSession = jbpmContext.getJobSession();
    contextSession = jbpmContext.getContextSession();

    bpelGraphSession = BpelGraphSession.getContextInstance(jbpmContext);
  }

  protected void resetMembers() {
    session = null;
    graphSession = null;
    taskMgmtSession = null;
    loggingSession = null;
    jobSession = null;
    contextSession = null;

    bpelGraphSession = null;
  }

  private static Log log = LogFactory.getLog(AbstractDbTestCase.class);
}
