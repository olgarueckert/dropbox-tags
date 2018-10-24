package de.rueckert.task.business;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import org.apache.solr.client.solrj.SolrServerException;
import de.rueckert.task.entity.DbxFile;
import de.rueckert.task.utils.DbxTagsAudit;
import de.rueckert.task.utils.DbxTagsException;

@Stateless
@Interceptors(DbxTagsAudit.class)
public class SolrHandler {

  @Inject
  SolrClientProvider solrClient;

  public DbxFile save(DbxFile fileToUpdate) throws SolrServerException, IOException {

    DbxFile file = solrClient.get( fileToUpdate.getPath() );

    if (file == null)
      return solrClient.save( filterInvalidTags( fileToUpdate ) );

    if (file.getTags() == null)
      file.setTags( new ArrayList<String>() );

    for (String tagToAdd : fileToUpdate.getTags()) {
      if (!file.getTags().contains( tagToAdd ) && isValid( tagToAdd ))
        file.getTags().add( tagToAdd );
    }

    return solrClient.update( file );
  }

  private DbxFile filterInvalidTags(DbxFile fileToUpdate) {
    List<String> tags = fileToUpdate.getTags();
    ArrayList<String> validTags = new ArrayList<String>();
    for (String tag : tags) {
      if (isValid( tag ))
        validTags.add( tag );

    }
    fileToUpdate.setTags( validTags );
    return fileToUpdate;
  }

  public DbxFile removeTags(DbxFile fileWithTagsToRemove) throws SolrServerException, IOException {
    DbxFile file = solrClient.get( fileWithTagsToRemove.getPath() );
    if (file != null) {

      file.setTags( removeTags( file.getTags(), fileWithTagsToRemove.getTags() ) );

      return solrClient.update( file );
    }
    throw new DbxTagsException( "File not found", 404 );
  }

  private List<String> removeTags(List<String> tags, List<String> tagsToRemove) {
    for (String tag : tagsToRemove) {
      if (tags.contains( tag ))
        tags.remove( tag );
    }
    return tags;
  }

  private boolean isValid(String tagToAdd) {
    return tagToAdd.matches( "[a-z0-9]*" );
  }

  public List<DbxFile> get(List<String> tags) throws SolrServerException, IOException {
    return solrClient.queryFile( tags );
  }

  public List<DbxFile> getAll() throws SolrServerException, IOException {
    return solrClient.queryFile( "*:", "*" );
  }

  public List<DbxFile> queryFile(List<String> tags, int offset, int range, String operator)
      throws SolrServerException, IOException {
    return solrClient.queryFile( tags, offset, range == 0 ? 10 : range, operator == null || operator.isEmpty() ? "OR" : operator );
  }

  public DbxFile queryFile(String path) throws SolrServerException, IOException {
    return solrClient.get( path );
  }

}
