package de.rueckert.task.business;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

public class FilesResourceTest {

  private static final String URL = "http://localhost:3000/dropbox-tags/api/files";

  private Client client;

  @Before
  public void setUp() throws Exception {
    client = ClientBuilder.newClient();

  }

  @Test
  public void crudTest() throws UnsupportedEncodingException {

    String tag = String.valueOf( System.currentTimeMillis() );

    JsonArray dbxFiles = client.target( URL + "/dbx/*" ).request().get( JsonArray.class );
    assertFalse( dbxFiles.isEmpty() );

    // create
    String path = ((JsonObject) dbxFiles.get( 0 )).getString( "pathLower" );

    JsonObject create =
      Json.createObjectBuilder().add( "path", path ).add( "tags", Json.createArrayBuilder().add( tag ) ).build();

    Response postResponse = client.target( URL ).request().post( Entity.json( create ) );

    assertThat( postResponse.getStatus(), is( 200 ) );

    // get
    JsonArray files = client.target( URL + "?path=" + URLEncoder.encode( path, "UTF-8" ) ).request().get( JsonArray.class );
    assertFalse( files.isEmpty() );
    assertTrue( ((JsonObject) files.get( 0 )).getJsonArray( "tags" ).toString().contains( tag ) );

    // search
    final JsonArray filesForTag =
      client.target( URL + "/search?tags=" + tag ).request( MediaType.APPLICATION_JSON ).get( JsonArray.class );
    assertTrue( filesForTag.size() == 1 );

    // download
    Response downloadResponse =
      client.target( URL + "/download?tags=" + tag ).request( MediaType.APPLICATION_OCTET_STREAM ).get();

    assertThat( downloadResponse.getStatus(), is( 200 ) );

    // removing tags
    Response removeTags =
      client.target( URL + "/remove-tags" ).request( MediaType.APPLICATION_JSON ).put( Entity.json( create ) );

    assertThat( removeTags.getStatus(), is( 200 ) );

    // check that tag was removed
    JsonArray updatedFiles =
      client.target( URL + "?path=" + URLEncoder.encode( path, "UTF-8" ) ).request().get( JsonArray.class );
    assertFalse( ((JsonObject) updatedFiles.get( 0 )).getJsonArray( "tags" ).toString().contains( tag ) );

  }

}
