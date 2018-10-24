package de.rueckert.task;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import de.rueckert.task.utils.DbxTagsException;

@Provider
public class ExceptionsMapper implements ExceptionMapper<Exception> {

  @Override
  public Response toResponse(Exception exception) {

    if (exception instanceof DbxTagsException) {
      return Response.status( ((DbxTagsException) exception).getCode() ).header( "cause", exception.getMessage() ).build();
    }
    final Throwable cause = exception.getCause();
    exception.printStackTrace();
    Response unknownError = Response.serverError().header( "cause", exception.toString() ).build();
    if (cause == null) {
      return unknownError;
    }
    return unknownError;
  }
}
