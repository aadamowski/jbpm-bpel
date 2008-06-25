package org.jbpm.bpel.graph.scope;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.graph.exe.FaultInstance;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Delivers the {@linkplain FaultInstance fault} carried by a
 * {@link BpelFaultException} to the nearest enclosing scope.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/02/19 22:25:44 $
 */
public class FaultActionHandler implements ActionHandler {

  private static final Log log = LogFactory.getLog(FaultActionHandler.class);
  private static final long serialVersionUID = 1L;

  public void execute(ExecutionContext exeContext) throws Exception {
    ScopeInstance scopeInstance = Scope.getInstance(exeContext.getToken());
    BpelFaultException faultException = (BpelFaultException) exeContext.getException();

    log.debug("handling fault exception: " + scopeInstance, faultException);
    scopeInstance.faulted(faultException.getFaultInstance());
  }
}