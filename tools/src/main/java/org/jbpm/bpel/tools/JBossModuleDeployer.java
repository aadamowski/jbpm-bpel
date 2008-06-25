/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.bpel.tools;

import java.io.IOException;
import java.net.URL;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A JBossWS test helper that deals with test deployment/undeployment, etc.
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 14-Oct-2004
 */
public class JBossModuleDeployer implements ModuleDeployer {

  private static final String MAIN_DEPLOYER = "jboss.system:service=MainDeployer";

  public void deploy(URL url) throws DeploymentException {
    try {
      MBeanServerConnection server = getServer();
      server.invoke(new ObjectName(MAIN_DEPLOYER), "redeploy", new Object[] { url },
          new String[] { "java.net.URL" });
    }
    catch (NamingException e) {
      throw new DeploymentException("could not retrieve mbean server connection", e);
    }
    catch (JMException e) {
      throw new DeploymentException("redeploy operation failed on: " + MAIN_DEPLOYER, e);
    }
    catch (IOException e) {
      throw new DeploymentException("communication with mbean server failed", e);
    }
  }

  public void undeploy(URL url) throws DeploymentException {
    try {
      MBeanServerConnection server = getServer();
      server.invoke(new ObjectName(MAIN_DEPLOYER), "undeploy", new Object[] { url },
          new String[] { "java.net.URL" });
    }
    catch (NamingException e) {
      throw new DeploymentException("could not retrieve mbean server connection", e);
    }
    catch (JMException e) {
      throw new DeploymentException("redeploy operation failed on: " + MAIN_DEPLOYER, e);
    }
    catch (IOException e) {
      throw new DeploymentException("communication with mbean server failed", e);
    }
  }

  private MBeanServerConnection getServer() throws NamingException {
    InitialContext iniCtx = new InitialContext();
    try {
      return (MBeanServerConnection) iniCtx.lookup("jmx/invoker/RMIAdaptor");
    }
    finally {
      iniCtx.close();
    }
  }
}
