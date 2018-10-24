package de.rueckert.task.entity;

import java.util.List;
import org.apache.solr.client.solrj.beans.Field;

public class DbxFile {

  @Field
  private String path;

  @Field
  private List<String> tags;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

}
