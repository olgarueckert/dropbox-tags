package de.rueckert.task.utils;

import javax.ejb.EJBException;

public class DbxTagsException extends EJBException {

  /**
   * 
   */
  private static final long serialVersionUID = 4992815623097869749L;

  private int code;

  public DbxTagsException(String message, int code) {

    super( message );
    this.code = code;
  }

  public int getCode() {
    return code;
  }

}
