package org.bonitasoft.cmd;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.command.BonitaCommand;
import org.bonitasoft.command.BonitaCommand.ExecuteAnswer;
import org.bonitasoft.engine.service.TenantServiceAccessor;


public class CmdPing extends BonitaCommand {
  static Logger logger = Logger.getLogger(CmdPing.class.getName());

  static String logHeader = "CmdPing ~~~";


  @Override
  public String getHelp(Map<String, Serializable> parameters, long tenantId, TenantServiceAccessor serviceAccessor) {
    return "Give parameters, the ping command will return it";
  }

@Override
public ExecuteAnswer executeCommand(ExecuteParameters parameters, TenantServiceAccessor arg1) {
    logger.info(logHeader+"Execute cmdPing");
    ExecuteAnswer executeAnswer = new ExecuteAnswer();
    executeAnswer.result.put("Status", "pong");
    executeAnswer.result.put("VerbeAcknowledge", parameters.verb);
    
    String traceInput="Receive:";
    if (parameters!=null && parameters.parameters !=null)
      for (String key:parameters.parameters.keySet())
      {
        traceInput+=key+",";
      }
    executeAnswer.result.put("traceInput", traceInput);
    return executeAnswer;
}
  




}
