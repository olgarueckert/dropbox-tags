package de.rueckert.task.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class DbxTagsAudit {
  @AroundInvoke
  public Object logCall(InvocationContext ic) throws Exception {
    StringBuffer info = new StringBuffer();

    long start = System.currentTimeMillis();
    Object target = ic.getTarget();
    String methodName = ic.getMethod().getName();

    Object result = null;
    try {

      info.append( " method " ).append( methodName );
      info.append( " called with params: " );
      if (ic.getParameters() != null)
        for (Object object : ic.getParameters()) {

          info.append( object ).append( "#" );
        }

      result = ic.proceed();
      return result;

    }
    catch (Exception e) {
      throw e;
    }
    finally {

      info.append( " Result = " ).append( result );
      info.append( " Method call took " ).append( System.currentTimeMillis() - start ).append( "ms" );

      Logger.getLogger( target.getClass().getName() ).log( Level.INFO, info.toString() );

    }
  }
}
