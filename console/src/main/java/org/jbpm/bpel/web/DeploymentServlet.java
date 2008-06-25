package org.jbpm.bpel.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.BpelGraphSession;
import org.jbpm.bpel.tools.WebModuleBuilder;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.par.ProcessArchive;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:14:01 $
 */
public class DeploymentServlet extends HttpServlet {

  private JbpmConfiguration jbpmConfiguration;
  private File deployDirectory;

  /** Servlet parameter: deploy directory. */
  public static final String PARAM_DEPLOY_DIRECTORY = "deployDirectory";

  /** Request parameter: process archive. */
  public static final String PARAM_PROCESS_ARCHIVE = "processArchive";

  private static final long serialVersionUID = 1L;

  private static final Pattern fileSeparatorPattern = compileFileSeparatorPattern();

  private static final Log log = LogFactory.getLog(DeploymentServlet.class);

  public void init() throws ServletException {
    // jbpm configuration
    String configResource = getServletContext().getInitParameter(WebConstants.PARAM_CONFIG_RESOURCE);
    jbpmConfiguration = JbpmConfiguration.getInstance(configResource);

    // deploy directory
    String deployDirectoryName = getInitParameter(PARAM_DEPLOY_DIRECTORY);
    if (deployDirectoryName == null) {
      // deduce the deploy directory from environment information
      String serverHomeDirectory;
      try {
        serverHomeDirectory = System.getProperty("jboss.server.home.dir");
      }
      catch (SecurityException e) {
        serverHomeDirectory = null;
      }
      if (serverHomeDirectory == null)
        throw new ServletException("servlet parameter not found: " + PARAM_DEPLOY_DIRECTORY);

      deployDirectoryName = serverHomeDirectory + File.separatorChar + "deploy";
      // TODO what about servers other than jboss?
    }

    deployDirectory = new File(deployDirectoryName);
    if (!deployDirectory.exists())
      throw new ServletException("deploy directory does not exist: " + deployDirectory);
  }

  private static Pattern compileFileSeparatorPattern() {
    String expression = "[/\\\\]";
    if (File.separatorChar != '/' && File.separatorChar != '\\') {
      expression = new StringBuffer(expression).insert(expression.length() - 2, File.separatorChar)
          .toString();
    }
    return Pattern.compile(expression);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // parse request
    parseRequest(request);
    // read and deploy process definition
    FileItem fileItem = (FileItem) request.getAttribute(PARAM_PROCESS_ARCHIVE);
    String fileName = extractFileName(fileItem.getName());
    ProcessDefinition processDefinition = readProcessDefinition(fileItem.getInputStream(), fileName);
    deployProcessDefinition(processDefinition);
    // build and deploy web module, if the language is BPEL
    if (processDefinition instanceof BpelProcessDefinition)
      deployWebModule((BpelProcessDefinition) processDefinition, fileName);
    // transfer web flow
    response.sendRedirect("processes.jsp");
  }

  private void parseRequest(HttpServletRequest request) throws ServletException, IOException {
    if (!ServletFileUpload.isMultipartContent(request))
      throw new ServletException("request does not have multipart content");

    try {
      ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
      List items = upload.parseRequest(request);
      if (items.size() != 1)
        throw new ServletException("deployment request must contain exactly one parameter");

      request.setAttribute(PARAM_PROCESS_ARCHIVE, parseProcessArchive((FileItem) items.get(0)));
    }
    catch (FileUploadException e) {
      throw new ServletException("could not parse upload request", e);
    }
  }

  private FileItem parseProcessArchive(FileItem processItem) throws ServletException {
    if (!PARAM_PROCESS_ARCHIVE.equals(processItem.getFieldName())) {
      throw new ServletException("expected parameter '"
          + PARAM_PROCESS_ARCHIVE
          + "', found: "
          + processItem.getFieldName());
    }

    if (processItem.isFormField()) {
      throw new ServletException("parameter '"
          + PARAM_PROCESS_ARCHIVE
          + "' is not an uploaded file");
    }

    String contentType = processItem.getContentType();
    if (!contentType.startsWith(WebConstants.CONTENT_TYPE_ZIP)
        && !contentType.startsWith(WebConstants.CONTENT_TYPE_X_ZIP_COMPRESSED)) {
      throw new ServletException("parameter '"
          + PARAM_PROCESS_ARCHIVE
          + "' is expected to have content type '"
          + WebConstants.CONTENT_TYPE_ZIP
          + "' or '"
          + WebConstants.CONTENT_TYPE_X_ZIP_COMPRESSED
          + "', found: "
          + contentType);
    }

    return processItem;
  }

  private static String extractFileName(String filePath) {
    /*
     * PORTABILITY INFO. Some browsers (e.g. internet explorer) send the file's absolute path. If
     * this servlet ran on the client side, it could leverage the File class to extract the file
     * name. However, since the separator char may differ between the client and the server, File is
     * not reliable. This code splits the path around matches of all known file separators and takes
     * the last fragment as the file name.
     */
    String[] fragments = fileSeparatorPattern.split(filePath);
    return fragments[fragments.length - 1];
  }

  private ProcessDefinition readProcessDefinition(InputStream fileSource, String fileName)
      throws IOException {
    try {
      ProcessArchive processArchive = new ProcessArchive(new ZipInputStream(fileSource));
      log.debug("loaded process archive: " + fileName);

      ProcessDefinition processDefinition = processArchive.parseProcessDefinition();
      log.debug("read process definition: " + processDefinition.getName());

      return processDefinition;
    }
    finally {
      fileSource.close();
    }
  }

  private void deployProcessDefinition(ProcessDefinition processDefinition) {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      if (processDefinition instanceof BpelProcessDefinition) {
        BpelGraphSession graphSession = BpelGraphSession.getContextInstance(jbpmContext);
        graphSession.deployProcessDefinition((BpelProcessDefinition) processDefinition);
      }
      else
        jbpmContext.deployProcessDefinition(processDefinition);
      log.info("deployed process definition: " + processDefinition.getName());
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      throw e;
    }
    finally {
      jbpmContext.close();
    }
  }

  private void deployWebModule(BpelProcessDefinition processDefinition, String fileName)
      throws ServletException {
    File moduleFile = new File(deployDirectory, extractFilePrefix(fileName) + ".war");

    WebModuleBuilder builder = new WebModuleBuilder();
    builder.setModuleFile(moduleFile);
    builder.buildModule(processDefinition);

    if (builder.getProblemHandler().getProblemCount() > 0)
      throw new ServletException("could not build web module for: " + processDefinition);

    log.info("deployed web module: " + moduleFile.getName());
  }

  private static String extractFilePrefix(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    return dotIndex != -1 ? fileName.substring(0, dotIndex) : fileName;
  }
}