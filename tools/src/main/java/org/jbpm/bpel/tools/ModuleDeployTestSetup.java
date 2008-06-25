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

import junit.extensions.TestSetup;
import junit.framework.TestSuite;

import org.jbpm.bpel.tools.ModuleDeployHelper;

/**
 * A test setup that deploys/undeploys archives
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 14-Oct-2004
 */
public class ModuleDeployTestSetup extends TestSetup {

  private ModuleDeployHelper deployHelper = new ModuleDeployHelper();
  private String[] archives;

  public ModuleDeployTestSetup(Class testClass, String archive) {
    this(testClass, new String[] { archive });
  }

  public ModuleDeployTestSetup(Class testClass, String[] archives) {
    super(new TestSuite(testClass));
    this.archives = archives;
  }

  protected void setUp() throws Exception {
    for (int i = 0; i < archives.length; i++) {
      String archive = archives[i];
      deployHelper.deploy(archive);
    }
  }

  protected void tearDown() throws Exception {
    for (int i = archives.length - 1; i >= 0; i--) {
      String archive = archives[i];
      deployHelper.undeploy(archive);
    }
  }
}
