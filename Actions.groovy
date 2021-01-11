import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.logging.Logger;
import java.io.File
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;


import org.bonitasoft.web.extension.page.PageContext;
import org.bonitasoft.web.extension.page.PageController;
import org.bonitasoft.web.extension.page.PageResourceProvider;


import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.CreationException;

import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandNotFoundException;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;





import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;




public class Actions {

    private static Logger logger= Logger.getLogger("org.bonitasoft.custompage.cmdmanage.groovy");
    
    
        
    
      // 2018-03-08T00:19:15.04Z
    public final static SimpleDateFormat sdfJson = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public final static SimpleDateFormat sdfHuman = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* doAction */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public static Index.ActionAnswer doAction(HttpServletRequest request, String paramJsonSt, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
                
        // logger.info("#### PingActions:Actions start");
        Index.ActionAnswer actionAnswer = new Index.ActionAnswer(); 
        List<BEvent> listEvents=new ArrayList<BEvent>();
        Object jsonParam = (paramJsonSt==null ? null : JSONValue.parse(paramJsonSt));
          
        try {
            String action=request.getParameter("action");
            logger.info("#### log:Actions  action is["+action+"] !");
            if (action==null || action.length()==0 )
            {
                actionAnswer.isManaged=false;
                logger.info("#### log:Actions END No Actions");
                return actionAnswer;
            }
            actionAnswer.isManaged=true;
            
            if (! TokenValidator.checkCSRFToken(request, response)) {
                actionAnswer.isResponseMap=false;
                return actionAnswer;
            }
            
            APISession apiSession = pageContext.getApiSession();
            HttpSession httpSession = request.getSession();            
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI( apiSession );
            IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI( apiSession );
            CommandAPI commandAPI = TenantAPIAccessor.getCommandAPI( apiSession );

            long tenantId = apiSession.getTenantId();          
            TenantServiceAccessor tenantServiceAccessor = TenantServiceSingleton.getInstance(tenantId);             

             
            // actionAnswer.responseMap.put("listevents",BEventFactory.getHtml( listEvents));
         // ------------------------ undeploy
            if ("undeploy".equals(action))
            {
              long cmdId = Index.getLongParameter(request,"cmdid",-1);
              if (cmdId==-1)
              {
                logger.severe("No command given");
                actionAnswer.responseMap.put("errormessage","No command given");
                actionAnswer.responseMap.put("shortmessage","No command given");
              }
              else
              {
                try
                {
                  CommandDescriptor commandDescriptor = commandAPI.get(cmdId);
                  
                  String message= undeploy( commandDescriptor.getName(), commandDescriptor.getImplementation(),commandAPI);
                  actionAnswer.responseMap.put("message",message);
                  actionAnswer.responseMap.put("shortmessage","Command "+commandDescriptor.getName()+" undeployed");
              
                
                } catch(Exception ce)
                {
                  logger.severe("Error undeploy a command["+ce.toString()+"]");
              
                  actionAnswer.responseMap.put("message","Command not found");
                  actionAnswer.responseMap.put("shortmessage","Command not found");
                }
              }             
            }
            
            // ---------------------- list command
            else if ("listcmd".equals(action))
            {
              String withSystemSt = request.getParameter("withsystem");
                  
              long startIndex = Index.getLongParameter(request,"startIndex",0L);
              long maxResults = Index.getLongParameter(request,"maxResult",100L);

              actionAnswer.responseMap.putAll( getListCommands( startIndex, maxResults, commandAPI ));
            }
            
            
            // ------------------------------ deploy
            else if ("deploy".equals(action))
            {

              String name         = jsonParam.get("name");
              String className    = jsonParam.get("classname");
              String description  = jsonParam.get("description");
              String serverJarDependenciesFileName  = jsonParam.get("serverjardependenciesfilename");
              logger.info("###Deploy command Name["+name+"] className["+className+"] Description["+description+"] jarDependencies["+serverJarDependenciesFileName+"]");

              String message="Deploy...";
              String errorMessage ="";
              actionAnswer.responseMap.put("shortmessage", "Error while deployed command ["+name+"]");
              try {
                undeploy( name, "",commandAPI);


                StringTokenizer st = new StringTokenizer( serverJarDependenciesFileName, ";");
                while (st.hasMoreTokens())
                {
                  String jarFileServer=st.nextToken();
                  if (jarFileServer.length()==0)
                    continue;
                  int posLastDot = className.lastIndexOf(".");
                  String jarname="";
                  if (posLastDot==-1)
                    jarname= className+".jar";
                  else
                    jarname= className.substring(posLastDot+1) +".jar";
                  logger.severe("Load file["+jarFileServer+"] jarName["+jarname+"]");
              
                  
                  String absoluteFile= getAbsoluteFile( jarFileServer, pageResourceProvider );
                  if (absoluteFile == null)
                  {
                    errorMessage+="can't find uploaded file ["+jarFileServer+"]";
                    continue;
                  }
                  File commandFile = new File( absoluteFile );
                  FileInputStream fis = new FileInputStream( commandFile );
                  byte[] fileContent = new byte[(int) commandFile.length()];
                  fis.read(fileContent);
                  fis.close();
                  
                
                  try { commandAPI.removeDependency(jarname); } catch(Exception e) {};

                  message+="Adding dependency ["+jarname+"] size["+fileContent.length+"]...";
                  commandAPI.addDependency(jarname, fileContent);
                  message+="Done.";
                }
                message+="Registering...";
                commandAPI.register(name, description, className);
                message+="Done.";
                actionAnswer.responseMap.put("shortmessage", "Command ["+name+"] deployed");
              } catch (ServerAPIException e) {
                  errorMessage+="ServerAPIException";
              } catch (UnknownAPITypeException e) {
                  errorMessage+="UnknownAPITypeException";
              } catch (FileNotFoundException e) {
                  errorMessage+="FileNotFoundException";
              } catch (IOException e) {
                  errorMessage+="IOException";
              } catch (AlreadyExistsException e) {
                  errorMessage+="AlreadyExistsException";
              } catch (CreationException e) {
                  errorMessage+="CreationException:"+e.toString();
                  logger.severe("###Error creation "+e.toString());
              }
              logger.severe("###Deploy command :"+message+"] :["+errorMessage+"]");
              actionAnswer.responseMap.put("message",message);
              actionAnswer.responseMap.put("errormessage",errorMessage);
   
              // refresh the list of command
              long startIndex = Index.getLongParameter(request,"startIndex",0L);
              long maxResults = Index.getLongParameter(request,"maxResult",100L);

              actionAnswer.responseMap.putAll( getListCommands( startIndex, maxResults, commandAPI ));
   
            }
            
            // ------------------------------ call
            else if ("callcmd".equals(action))
            {
              
              long commandId       = Long.valueOf( jsonParam.get("id"));
              String parametersSt    = jsonParam.get("parameters");
              logger.info("###Call command Id["+commandId+"] parameters["+parametersSt+"]");
              
              CommandDescriptor commandDescriptor = getCommandById(commandId, commandAPI);
              
              if (commandDescriptor==null)
              {
                actionAnswer.responseMap.put("errormessage","CommandId ["+commandId+"] not found");
              }
              else
              {
                
                Map<String, Serializable> parameters = new HashMap<String, Serializable>();
                Object parametersJson = (parametersSt==null ? null : JSONValue.parse(parametersSt));
                if (parametersJson instanceof Map)
                {
                  parameters.putAll((Map)parametersJson);
                }                
                else
                {
                  actionAnswer.responseMap.put("errormessage","Parameters must be a map");
                  logger.info("###Camm parametersJson=["+(parametersJson==null ? "null" : parametersJson.getClass().getName())+"] parametersJson="+parametersJson);
                }
                parameters.put("tenantId", tenantId);
                

                // see the command in CmdMeteor
                final Serializable resultCommand = commandAPI.execute(commandId, parameters);
                actionAnswer.responseMap.put("message", "Call, result=[" + (resultCommand != null ? resultCommand.toString() : "null") + "]");
              
               }

            } // end of callcmd
            
              
              
            
            logger.info("#### log:Actions END responseMap ="+actionAnswer.responseMap.size());
            return actionAnswer;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("#### log:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            actionAnswer.isResponseMap=true;
            actionAnswer.responseMap.put("errormessage", "log:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            

            
            return actionAnswer;
        }
    }

    

    private static String undeploy(String cmdName, String cmdDependencyName, CommandAPI commandAPI) {
      String message = "Undeploy " + cmdName + "...";
      try {
        commandAPI.unregister(cmdName);
        message += ",Removing dependency...";
        commandAPI.removeDependency(cmdDependencyName);
        message += "Done.";
      } catch (Exception e) {
        message += e.toString();
      }
    }
    
    /**
     * return 2 list of command, one system and one custom
     * @param withSystem
     * @param startIndex
     * @param maxResults
     * @param commandAPI
     * @return
     */
    private static Map<String, Object> getListCommands( long startIndex, long maxResults, CommandAPI commandAPI) {
      List<CommandDescriptor> listCmd = commandAPI.getAllCommands((int) startIndex, (int) maxResults, CommandCriterion.NAME_ASC);
      
      
      List<Map<String, Object>> listCmdSystem = new ArrayList<Map<String, Object>>();
      List<Map<String, Object>> listCmdCustom = new ArrayList<Map<String, Object>>();
      
      for (CommandDescriptor commandDescriptor : listCmd) {
        HashMap<String, Object> mapCmd = new HashMap<String, Object>();
        mapCmd.put("id", commandDescriptor.getId());
        mapCmd.put("description", commandDescriptor.getDescription());
        mapCmd.put("implementation", commandDescriptor.getImplementation());
        mapCmd.put("name", commandDescriptor.getName());
        mapCmd.put("issystemcommandlabel", commandDescriptor.isSystemCommand() ? "(system)" : "");
        mapCmd.put("allowundeploy", !commandDescriptor.isSystemCommand());
        if (commandDescriptor.isSystemCommand())
          listCmdSystem.add(mapCmd);
        else
          listCmdCustom.add(mapCmd);
      }
      // sort the two lists
      Collections.sort(listCmdSystem, new Comparator<HashMap<String, Object>>() {

        public int compare(HashMap<String, Object> s1, HashMap<String, Object> s2) {
          String d1 = s1.get("name");
          String d2 = s2.get("name");

          return d1.compareTo(d2);
        }
      });
      Collections.sort(listCmdCustom, new Comparator<HashMap<String, Object>>() {

        public int compare(HashMap<String, Object> s1, HashMap<String, Object> s2) {
          String d1 = s1.get("name");
          String d2 = s2.get("name");

          return d1.compareTo(d2);
        }
      });

      // add it in the result
      Map<String,Object> result = new HashMap<String,Object>();
      result.put("listcmdsystem",listCmdSystem);
      result.put("listcmdcustom",listCmdCustom);
      
      return result;
    }
    
    
    /**
     * get the absolute file path form the file uploaded
     * @param fileName
     * @param pageResourceProvider
     * @return
     */
    private static String getAbsoluteFile(String fileName, PageResourceProvider pageResourceProvider )
    {
      
      File pageDirectory = pageResourceProvider.getPageDirectory();
      List<String> listParentTmpFile = new ArrayList<String>();
      try {
        listParentTmpFile.add(pageDirectory.getCanonicalPath() + "/../../../tmp/");
        listParentTmpFile.add(pageDirectory.getCanonicalPath() + "/../../");
      } catch (Exception e) {
        logger.info("cmdManage : error get CanonicalPath of pageDirectory[" + e.toString() + "]");
        return;
      }
      for (String pathTemp : listParentTmpFile) {
        logger.info("cmdManage : CompleteuploadFile  TEST [" + pathTemp + fileName + "]");
        if (fileName.length() > 0 && (new File(pathTemp + fileName)).exists()) {
          logger.info("cmdManage : CompleteuploadFile  FOUND [" + pathTemp + fileName + "]");
          return (new File(pathTemp + fileName)).getAbsoluteFile();
        }
      }
    }
    
    /**
     * get a command from its ID
     * @param commandId
     * @param commandAPI
     * @return
     */
    private static CommandDescriptor getCommandById(long commandId, CommandAPI commandAPI) {
      List<CommandDescriptor> listCommands = commandAPI.getAllCommands(0, 1000, CommandCriterion.NAME_ASC);
      for (CommandDescriptor command : listCommands) {
        logger.info("Command["+command.getId()+"] <>"+commandId+"? "+(commandId.equals(command.getId())))
        if (commandId.equals(command.getId()))
          return command;
      }
      return null;

    }
}
