package de.rueckert.task.business;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.interceptor.Interceptors;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import de.rueckert.task.entity.DbxFile;
import de.rueckert.task.utils.DbxTagsAudit;

@ApplicationScoped
@Interceptors(DbxTagsAudit.class)
public class SolrClientProvider {
  private static final int MAX_ROW_COUNT = 200;

  private static final String SOLR_URL = System.getenv( "SOLR_URL" );

  private HttpSolrClient solrClient;

  @PostConstruct
  public void init() {
    if (solrClient == null) {

      if (SOLR_URL != null) {
        solrClient = new HttpSolrClient.Builder( SOLR_URL ).build();
        solrClient.setParser( new XMLResponseParser() );
      }

    }

  }

  public DbxFile save(DbxFile file) throws IOException, SolrServerException {
    SolrInputDocument solrInputDocument = convertToSolrDoc( file );

    solrClient.add( solrInputDocument );
    solrClient.commit();

    return get( file.getPath() );
  }

  public DbxFile update(DbxFile file) throws IOException, SolrServerException {
    SolrInputDocument solrInputDocument = convertToSolrDoc( file );
    solrInputDocument.addField( "id", getId( file.getPath() ) );

    solrClient.add( solrInputDocument );
    solrClient.commit();

    return get( file.getPath() );
  }

  private SolrInputDocument convertToSolrDoc(DbxFile file) {
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    solrInputDocument.addField( "tags", file.getTags() );
    solrInputDocument.addField( "path", file.getPath() );
    return solrInputDocument;
  }

  public String getId(String path) throws SolrServerException, IOException {

    SolrQuery query = new SolrQuery();
    query.set( "q", "path:" + '"' + path + '"' );
    QueryResponse response = solrClient.query( query );
    SolrDocumentList results = response.getResults();
    if (results.size() == 0)
      return null;

    SolrDocument solrDocument = results.get( 0 );
    return solrDocument.get( "id" ).toString();
  }

  // Query helpers:
  public DbxFile get(String path) throws SolrServerException, IOException {
    final List<DbxFile> files = queryFile( "path:", '"' + path + '"' );
    return files.isEmpty() ? null : files.get( 0 );
  }

  public List<DbxFile> queryFile(String key, String name) throws SolrServerException, IOException {
    SolrQuery query = new SolrQuery();
    query.set( "q", key + name );
    query.setRows( MAX_ROW_COUNT );
    QueryResponse response = solrClient.query( query );
    return response.getBeans( DbxFile.class );

  }

  public List<DbxFile> queryFile(SolrQuery query) throws SolrServerException, IOException {
    QueryResponse response = solrClient.query( query );

    return response.getBeans( DbxFile.class );

  }

  public List<DbxFile> queryFile(List<String> tags) throws SolrServerException, IOException {
    SolrQuery query = new SolrQuery();
    query.set( "q", prepareConditionalQuery( tags, "OR" ) );
    query.setRows( MAX_ROW_COUNT );
    return queryFile( query );
  }

  public List<DbxFile> queryFile(List<String> tags, int offset, int range, String operator)
      throws SolrServerException, IOException {
    SolrQuery query = new SolrQuery();
    query.setStart( offset );
    query.setRows( range );
    query.set( "q", prepareConditionalQuery( tags, operator ) );

    return queryFile( query );
  }

  private String[] prepareConditionalQuery(List<String> tags, String operand) {
    ArrayList<String> queryArr = new ArrayList<String>();

    String query = "";
    if (operand != null) {
      for (String tag : tags) {
        if (!query.isEmpty())
          query += " " + operand.toUpperCase();
        query += " tags:" + tag;
      }
      return new String[] {query};
    }

    tags.forEach( (tag) -> {
      queryArr.add( "tags:" + tag );

    } );

    return queryArr.toArray( new String[queryArr.size()] );
  }

}
