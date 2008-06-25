package org.jbpm.bpel.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.xml.ProblemCounter;
import org.jbpm.bpel.xml.ProblemHandler;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.jpdl.xml.Problem;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/06/12 08:51:51 $
 */
public class WebModuleBuilder {

  private File moduleFile = DEFAULT_MODULE_FILE;
  private String packagePrefix = DEFAULT_PACKAGE_PREFIX;
  private ProblemHandler problemHandler = new ProblemCounter();

  private String modulePrefix;
  private File workDirectory;

  private static final String DEFAULT_PACKAGE_PREFIX = WscompileTool.DEFAULT_PACKAGE_NAME;
  private static final File DEFAULT_MODULE_FILE = new File("module.war");

  static final String DIR_WSDL = "wsdl";
  static final String DIR_WEB_INF = "WEB-INF";
  static final String DIR_CLASSES = "classes";
  static final char SEPARATOR = '/'; // use File.separatorChar instead?

  private static final Log log = LogFactory.getLog(WebModuleBuilder.class);

  public File getModuleFile() {
    return moduleFile;
  }

  public void setModuleFile(File moduleFile) {
    if (moduleFile == null)
      throw new IllegalArgumentException("module file cannot be null");

    this.moduleFile = moduleFile;
  }

  public String getPackagePrefix() {
    return packagePrefix;
  }

  public void setPackagePrefix(String packagePrefix) {
    if (packagePrefix == null)
      throw new IllegalArgumentException("package prefix cannot be null");

    this.packagePrefix = packagePrefix;
  }

  public ProblemHandler getProblemHandler() {
    return problemHandler;
  }

  public void setProblemHandler(ProblemHandler problemHandler) {
    if (problemHandler == null)
      throw new IllegalArgumentException("problem handler cannot be null");

    this.problemHandler = problemHandler;
  }

  public void buildModule(BpelProcessDefinition processDefinition) {
    modulePrefix = moduleFile.getName();
    int dotIndex = modulePrefix.indexOf('.');
    if (dotIndex != -1)
      modulePrefix = modulePrefix.substring(0, dotIndex);

    try {
      buildModuleImpl(processDefinition);
      log.info("packaged web module: " + moduleFile.getName());
    }
    catch (IOException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "could not build web module", e));
    }
  }

  private void buildModuleImpl(BpelProcessDefinition processDefinition) throws IOException {
    workDirectory = makeWorkDirectory();
    try {
      FileDefinition fileDefinition = processDefinition.getFileDefinition();
      if (!hasDirectory(fileDefinition, DIR_WEB_INF + SEPARATOR + DIR_WSDL)) {
        callWsdlServiceTool(processDefinition);
        if (problemHandler.getProblemCount() > 0)
          return;
      }
      if (!hasDirectory(fileDefinition, DIR_WEB_INF + SEPARATOR + DIR_CLASSES)) {
        callJavaMappingTool();
        if (problemHandler.getProblemCount() > 0)
          return;
      }
      if (!fileDefinition.hasFile(DIR_WEB_INF
          + SEPARATOR
          + WebServicesDescriptorTool.DEFAULT_WEB_SERVICES_FILE_NAME)) {
        callWebServicesDescriptorTool();
        if (problemHandler.getProblemCount() > 0)
          return;
      }
      if (!fileDefinition.hasFile(DIR_WEB_INF
          + SEPARATOR
          + WebAppDescriptorTool.DEFAULT_WEB_APP_FILE_NAME)) {
        callWebAppDescriptorTool();
        if (problemHandler.getProblemCount() > 0)
          return;
      }
      writeUserProvidedFiles(fileDefinition);
      packFiles();
    }
    finally {
      FileUtil.clean(workDirectory);
    }
  }

  private static boolean hasDirectory(FileDefinition fileDefinition, String directory) {
    for (Iterator i = fileDefinition.getBytesMap().keySet().iterator(); i.hasNext();) {
      String fileName = (String) i.next();
      if (fileName.startsWith(directory))
        return true;
    }
    return false;
  }

  protected String generateModuleName(BpelProcessDefinition processDefinition) {
    return toLowerCaseJavaIdentifier(processDefinition.getName());
  }

  private static String toLowerCaseJavaIdentifier(String name) {
    final int length = name.length();
    char[] identifierBuffer = new char[length];

    // first character
    int i;
    for (i = 0; i < length; i++) {
      char ch = name.charAt(i);
      if (Character.isJavaIdentifierStart(ch)) {
        identifierBuffer[0] = Character.toLowerCase(ch);
        break;
      }
    }

    // next characters
    int k = 1;
    for (i++; i < length; i++) {
      char ch = name.charAt(i);
      if (Character.isJavaIdentifierPart(ch))
        identifierBuffer[k++] = Character.toLowerCase(ch);
    }

    return new String(identifierBuffer, 0, k);
  }

  protected File makeWorkDirectory() throws IOException {
    return FileUtil.makeTempDirectory(modulePrefix, ".war");
  }

  protected File getWsdlDirectory() {
    return new File(workDirectory, DIR_WSDL);
  }

  protected String getWsdlBindingFilesPrefix() {
    return modulePrefix + '-' + WsdlServiceTool.DEFAULT_BINDING_FILES_PREFIX + '-';
  }

  protected String getWsdlServiceFileName() {
    return modulePrefix + '-' + WsdlServiceTool.DEFAULT_SERVICE_FILE_NAME;
  }

  protected File getDeploymentDescriptorFile() {
    return new File(getClassesDirectory(), DeploymentDescriptor.FILE_NAME);
  }

  protected File getJaxrpcMappingFile() {
    return new File(workDirectory, modulePrefix + "-mapping.xml");
  }

  protected File getClassesDirectory() {
    return new File(workDirectory, DIR_CLASSES);
  }

  protected File getWebServicesDescriptorFile() {
    return new File(workDirectory, WebServicesDescriptorTool.DEFAULT_WEB_SERVICES_FILE_NAME);
  }

  protected File getWebAppDescriptorFile() {
    return new File(workDirectory, WebAppDescriptorTool.DEFAULT_WEB_APP_FILE_NAME);
  }

  protected void callWsdlServiceTool(BpelProcessDefinition processDefinition) throws IOException {
    // make output directories
    File wsdlDirectory = getWsdlDirectory();
    wsdlDirectory.mkdirs();

    File deploymentDescriptorFile = getDeploymentDescriptorFile();
    deploymentDescriptorFile.getParentFile().mkdirs();

    // configure tool
    WsdlServiceTool tool = new WsdlServiceTool();
    tool.setWsdlDirectory(wsdlDirectory);
    tool.setBindingFilesPrefix(getWsdlBindingFilesPrefix());
    tool.setServiceFileName(getWsdlServiceFileName());
    tool.setDeploymentDescriptorFile(deploymentDescriptorFile);
    tool.setProblemHandler(problemHandler);

    // run tool
    tool.generateWsdlService(processDefinition);

    // copy xml schema documents
    FileDefinition fileDefinition = processDefinition.getFileDefinition();
    for (Iterator i = fileDefinition.getBytesMap().entrySet().iterator(); i.hasNext();) {
      Map.Entry fileEntry = (Map.Entry) i.next();

      String fileName = (String) fileEntry.getKey();
      if (!fileName.endsWith(".xsd"))
        continue; // not an xml schema document, skip it

      byte[] fileData = (byte[]) fileEntry.getValue();
      File file = new File(wsdlDirectory, fileName);
      writeFile(file, fileData);
      log.debug("wrote xml schema: " + file.getName());
    }
  }

  private static void writeFile(File file, byte[] data) throws IOException {
   // create parent directory if it does not exist
    File parentDir = file.getParentFile();
    if (parentDir != null)
      parentDir.mkdirs(); 

    // write data to file
    OutputStream fileSink = new FileOutputStream(file);
    try {
      fileSink.write(data);
    }
    finally {
      fileSink.close();
    }
  }

  protected void callJavaMappingTool() {
    // make output directories
    File jaxrpcMappingFile = getJaxrpcMappingFile();
    jaxrpcMappingFile.getParentFile().mkdirs();

    File classesDirectory = getClassesDirectory();
    classesDirectory.mkdirs();

    // configure tool
    JavaMappingTool tool = new WscompileTool();
    tool.setWsdlFile(new File(getWsdlDirectory(), getWsdlServiceFileName()));
    tool.setPackageName(generateJavaMappingPackage());
    tool.setJaxrpcMappingFile(jaxrpcMappingFile);
    tool.setClassesDirectory(classesDirectory);
    tool.setProblemHandler(problemHandler);

    // run tool
    tool.generateJavaMapping();
  }

  protected String generateJavaMappingPackage() {
    return packagePrefix + '.' + modulePrefix;
  }

  protected void callWebServicesDescriptorTool() {
    // make output directory
    File webServicesDescriptorFile = getWebServicesDescriptorFile();
    webServicesDescriptorFile.getParentFile().mkdirs();

    // configure tool
    WebServicesDescriptorTool tool = new WebServicesDescriptorTool();
    tool.setWsdlFile(new File(getWsdlDirectory(), getWsdlServiceFileName()));
    tool.setJaxrpcMappingFile(getJaxrpcMappingFile());
    tool.setWebServicesDescriptorFile(webServicesDescriptorFile);
    tool.setProblemHandler(problemHandler);

    // run tool
    tool.generateWebServicesDescriptor();
  }

  protected void callWebAppDescriptorTool() {
    // make output directory
    File webAppDescriptorFile = getWebAppDescriptorFile();
    webAppDescriptorFile.getParentFile().mkdirs();

    // configure tool
    WebAppDescriptorTool tool = new WebAppDescriptorTool();
    tool.setWebServicesDescriptorFile(getWebServicesDescriptorFile());
    tool.setWebAppDescriptorFile(getWebAppDescriptorFile());

    // run tool
    tool.generateWebAppDescriptor();
  }

  protected void writeUserProvidedFiles(FileDefinition fileDefinition) throws IOException {
    for (Iterator i = fileDefinition.getBytesMap().entrySet().iterator(); i.hasNext();) {
      Map.Entry fileEntry = (Map.Entry) i.next();

      String fileName = (String) fileEntry.getKey();
      if (!fileName.startsWith(DIR_WEB_INF))
        continue; // not a web module artifact, skip it

      byte[] fileData = (byte[]) fileEntry.getValue();
      if (fileData == null)
        continue; // directory, skip it

      File file = new File(workDirectory, fileName.substring(DIR_WEB_INF.length() + 1));
      writeFile(file, fileData);
      log.info("wrote user-provided artifact: " + file.getName());
    }
  }

  private void packFiles() throws IOException {
    ZipOutputStream moduleSink = new ZipOutputStream(new FileOutputStream(moduleFile));
    try {
      FileUtil.zip(workDirectory, moduleSink, DIR_WEB_INF);
    }
    finally {
      moduleSink.close();
    }
  }
}
