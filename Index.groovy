import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import org.apache.commons.lang3.StringEscapeUtils
import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageController
import org.bonitasoft.console.common.server.page.PageResourceProvider
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandNotFoundException;

import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandCriterion;
import org.codehaus.groovy.tools.shell.CommandAlias;

import java.util.logging.Logger;
import org.json.simple.JSONObject;
	
public class Index implements PageController {

		@Override
		public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
	
		Logger logger= Logger.getLogger("org.bonitasoft");
		
		
		try {
			def String indexContent;
			pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s-> indexContent = s.getText() };
			response.setCharacterEncoding("UTF-8");
			PrintWriter out = response.getWriter()

			String action=request.getParameter("action");
			logger.severe("###################################### YES action is["+action+"] page=["+request.getParameter("page")+"] !");
			
			logger.severe("pt0");
		
		
			if (action==null || action.length()==0 )
			{
				logger.severe("###################################### RUN Default !");
				
				runTheBonitaIndexDoGet( request, response,pageResourceProvider,pageContext);
				return;
			}
			
			APISession session = pageContext.getApiSession()
			CommandAPI commandAPI = TenantAPIAccessor.getCommandAPI(session);
	

			// ------------------------ undeploy
			if ("undeploy".equals(action))
			{
				HashMap<String,Object> resultOperation = new HashMap<String,Object>();
				long cmdId = getLongParameter(request,"cmdid",-1);
				if (cmdId==-1)
				{
					logger.severe("No command given");
					resultOperation.put("errormessage","No command given");
					resultOperation.put("shortmessage","No command given");
				}
				else
				{
					try
					{
						CommandDescriptor commandDescriptor = commandAPI.get(cmdId);
					
						String message= undeploy( commandDescriptor.getName(), commandDescriptor.getImplementation(),commandAPI);
						resultOperation.put("message",message);
						resultOperation.put("shortmessage","Command "+commandDescriptor.getName()+" undeployed");
					} catch(CommandNotFoundException ce)
					{
						logger.severe("Error undeploy a command["+ce.toString()+"]");
				
						resultOperation.put("message","Command not found");
						resultOperation.put("shortmessage","Command not found");
					}
				}
				// logger.severe("Get list command");
				String withSystemSt = request.getParameter("withsystem");
				boolean withSystem = "true".equals(withSystemSt)
						
				long startIndex = getLongParameter(request,"startIndex",0L);
				long maxResults = getLongParameter(request,"maxResult",100L);
				
				ArrayList<HashMap<String,Object>> listCmdJson = getListCommand( withSystem,startIndex, maxResults, commandAPI);
				resultOperation.put("listcmd",listCmdJson);

				
				String jsonResultOperationSt = JSONValue.toJSONString( resultOperation );
				// logger.severe("Result operation "+jsonResultOperationSt);

				out.write( jsonResultOperationSt );
				out.flush();
				out.close();
				return;
			}
			
			// ---------------------- list command
			if ("listcmd".equals(action))
			{
				String withSystemSt = request.getParameter("withsystem");
				boolean withSystem = "true".equals(withSystemSt)
						
				long startIndex = getLongParameter(request,"startIndex",0L);
				long maxResults = getLongParameter(request,"maxResult",100L);

				ArrayList<HashMap<String,Object>> listCmdJson  = getListCommand( withSystem, startIndex, maxResults, commandAPI );
				String jsonSt = JSONArray.toJSONString( listCmdJson);
				out.write( jsonSt );
				out.flush();
				out.close();
				return;				
			}
					
			// ------------------------------ deploy
			if ("deploy".equals(action))
			{
				logger.severe("###Deploy command");
				HashMap<String,Object> resultOperation = new HashMap<String,Object>();
				String name 		= request.getParameter("name");
				String className 	= request.getParameter("classname");
				String description 	= request.getParameter("description");
				String serverJarDependenciesFileName 	= request.getParameter("serverjardependenciesfilename");
				String message="Deploy...";
				String errorMessage ="";
				resultOperation.put("shortmessage", "Error while deployed command ["+name+"]");
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
				
						File commandFile = new File( jarFileServer );
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
					resultOperation.put("shortmessage", "Command ["+name+"] deployed");
				} catch (BonitaHomeNotSetException e) {
						errorMessage+="BonitaHomeNotSet";
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
				resultOperation.put("message",message);
				resultOperation.put("errormessage",errorMessage);
				String jsonResultOperationSt = JSONValue.toJSONString( resultOperation );
				out.write( jsonResultOperationSt );
				out.flush();
				out.close();
				return;
				
			}
			
			out.write( "Unknow command" );
			out.flush();
			out.close();
			return;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			logger.severe("Exception ["+e.toString()+"] at "+exceptionDetails);
		}
	}

	private String undeploy(String cmdName, String cmdDependencyName, CommandAPI commandAPI)
	{
		String message="Undeploy "+cmdName+"...";
		try
		{
			commandAPI.unregister( cmdName );
			message+=",Removing dependency...";
			commandAPI.removeDependency(cmdDependencyName);
			message+="Done.";
		}
		catch(Exception e)
		{
			message+= e.toString();
		}
	}
	
	private ArrayList<HashMap<String,Object>> getListCommand( boolean withSystem, long startIndex, long maxResults, CommandAPI commandAPI )
	{
		List<CommandDescriptor> listCmd = commandAPI.getAllCommands( (int) startIndex, (int) maxResults,  CommandCriterion.NAME_ASC);
		ArrayList<HashMap<String,Object>> listCmdJson = new ArrayList<HashMap<String,Object>>();
		for (CommandDescriptor commandDescriptor : listCmd)
		{
				HashMap<String,Object> mapCmd = new HashMap<String,Object>();
				mapCmd.put("id", commandDescriptor.getId());
				mapCmd.put("description", commandDescriptor.getDescription());
				mapCmd.put("implementation", commandDescriptor.getImplementation());
				mapCmd.put("name", commandDescriptor.getName());
				mapCmd.put("issystemcommandlabel", commandDescriptor.isSystemCommand() ? "(system)":"");
				mapCmd.put("allowundeploy", ! commandDescriptor.isSystemCommand() );
				if (withSystem)
					listCmdJson.add( mapCmd );
				else if ( ! commandDescriptor.isSystemCommand())
					listCmdJson.add( mapCmd );
		}
		
		Collections.sort(listCmdJson, new Comparator<HashMap<String,Object>>() {
									public int compare(HashMap<String,Object> s1, HashMap<String,Object> s2) {
											String d1 = s1.get("name");
											String d2 = s2.get("name");
											
											return d1.compareTo( d2 );
									}
							});
		
		return listCmdJson;
	}
	
	
	
	
	private long getLongParameter(HttpServletRequest request, String paramName, long defaultValue)
	{
		Logger logger= Logger.getLogger("org.bonitasoft");
		String valueParamSt = request.getParameter(paramName);
		logger.severe("paramName=["+paramName+"] value["+valueParamSt+"]");
		
		if (valueParamSt==null  || valueParamSt.length()==0)
			return defaultValue;
		long valueParam=defaultValue;
		try
		{
			valueParam = Long.valueOf( valueParamSt );
			logger.severe("paramName=["+paramName+"] valueLong["+valueParam+"]");
		}
		catch( Exception e)
		{
			valueParam= defaultValue;
		}
		return valueParam;
	}
	
	
	
	private void runTheBonitaIndexDoGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
				try {
						def String indexContent;
						pageResourceProvider.getResourceAsStream("index.html").withStream { InputStream s->
								indexContent = s.getText()
						}
						
						def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";
						
						// 7.0 Living application : don't do that
						// indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
						// indexContent= indexContent.replace("@_PAGE_RESOURCE_@", pageResource);
						
						response.setCharacterEncoding("UTF-8");
						PrintWriter out = response.getWriter();
						out.print(indexContent);
						out.flush();
						out.close();
				} catch (Exception e) {
						e.printStackTrace();
				}
		}

}
