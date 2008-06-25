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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A JBossWS test helper that deals with test deployment/undeployment, etc.
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 14-Oct-2004
 */
public class ModuleDeployHelper {

  private static final String DEFAULT_TARGET_SERVER = "jboss";

  /**
   * Deploys the given archive.
   * @throws DeploymentException if deployment fails
   */
  public void deploy(String archive) throws DeploymentException {
    URL url = getArchiveURL(archive);
    getDeployer().deploy(url);
  }

  /**
   * Undeploys the given archive.
   * @throws DeploymentException if undeployment fails
   */
  public void undeploy(String archive) throws DeploymentException {
    URL url = getArchiveURL(archive);
    getDeployer().undeploy(url);
  }

  /** True, if -Djbossws.target.server=jboss */
  public boolean isTargetServerJBoss() {
    String targetServer = System.getProperty("jbossws.target.server", DEFAULT_TARGET_SERVER);
    return "jboss".equals(targetServer);
  }

  /** True, if -Djbossws.target.server=tomcat */
  public boolean isTargetServerTomcat() {
    String targetServer = System.getProperty("jbossws.target.server", DEFAULT_TARGET_SERVER);
    return "tomcat".equals(targetServer);
  }

  private ModuleDeployer getDeployer() {
    if (isTargetServerJBoss())
      return new JBossModuleDeployer();

    throw new IllegalStateException("Unsupported target server");
  }

  /** Try to discover the URL for the deployment archive */
  private URL getArchiveURL(String archive) {
    URL url;
    try {
      url = new URL(archive);
    }
    catch (MalformedURLException ignore) {
      File file = new File(archive);
      if (file.exists()) {
        try {
          url = file.toURL();
        }
        catch (MalformedURLException e) {
          throw new IllegalArgumentException("could not parse as url: " + archive);
        }
      }
      else {
        url = Thread.currentThread().getContextClassLoader().getResource(archive);
        if (url == null)
          throw new IllegalArgumentException("could not obtain url for: " + archive);
      }
    }
    return url;
  }
}
