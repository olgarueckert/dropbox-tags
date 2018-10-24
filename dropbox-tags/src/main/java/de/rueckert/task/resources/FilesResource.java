package de.rueckert.task.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.solr.client.solrj.SolrServerException;
import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.Metadata;
import de.rueckert.task.business.DbxFileHandler;
import de.rueckert.task.business.SolrHandler;
import de.rueckert.task.entity.DbxFile;

@RequestScoped
@Path("files")
@Produces({MediaType.APPLICATION_JSON})
public class FilesResource {

  @Inject
  SolrHandler fileHandler;

  @Inject
  DbxFileHandler dbxFilesHandler;

  /**
   * Get files saved in solr instance with corresponding tags<br>
   * <br>
   * @param path Optional parameter to search DbxFile for spesific path, if not specified all DbxFiles will be retrieved
   * @return List of tagged dropbox files solr representation
   * @throws SolrServerException
   * @throws IOException
   */
  @GET
  public List<DbxFile> get(@QueryParam("path") String path) throws SolrServerException, IOException {
    if (path == null || path.isEmpty())
      return fileHandler.getAll();
    else
      return Arrays.asList( fileHandler.queryFile( path ) );
  }

  /**
   * Add any number of tags to a DbxFile <br>
   * Use <b>pathLower</b> attribute of a dropbox file to specify which dropbox file you want to tag as a <b>path</b> in json
   * payload<br>
   * Enter list of tags (separated with comma) you want to add to a dropbox file
   * @param dbxFile payload in folloing format:<br>
   *          '{ \ "path": ""/some_lower_path_of_a_dropbox_dile", \ "tags": [ \ "tag1",\ "tag2" \ ] \ } <br>
   * @return saved DbxFile as a json
   * @throws GetMetadataErrorException
   * @throws DbxException
   * @throws SolrServerException
   * @throws IOException
   */
  @POST
  public Response add(DbxFile dbxFile) throws GetMetadataErrorException, DbxException, SolrServerException, IOException {

    if (dbxFile.getTags() == null || dbxFile.getTags().isEmpty())
      return Response.status( Response.Status.NOT_ACCEPTABLE ).entity( "File must contain at least one tag" ).build();

    if (dbxFilesHandler.isFile( dbxFile.getPath() ))
      return Response.ok().entity( fileHandler.save( dbxFile ) ).build();

    return Response.serverError().entity( "Tag could not be added to a folder" ).build();

  }

  /**
   * Use this method to remove tags from a DbxFile stored in solr. Specify in DbxFile json payload unique file path you want to
   * remove the tags from. This json must contain only the tags you want to remove.
   * @param dbxFile payload in folloing format:<br>
   *          '{ \ "path": ""/some_lower_path_of_a_dropbox_dile", \ "tags": [ \ "tagToRemove1",\ "tagToRemove2" \ ] \ } <br>
   * @return updated DbxFile as a json
   * @throws SolrServerException
   * @throws IOException
   */
  @PUT
  @Path("remove-tags")
  public DbxFile removeTags(DbxFile dbxFile) throws SolrServerException, IOException {
    return fileHandler.removeTags( dbxFile );
  }

  /**
   * Search file by specific tags
   * @param tags specify tags you want to search by (must be at least one)
   * @param offset optional parameter used for pagination, enter positive numeric value here, if left empty fallbacks to 0
   * @param range optional parameter used for pagination, enter positive numeric value here, if left empty fallbacks to 10
   * @param operator optional boolean operator, possible values are OR and AND, if left empty fallbacks to OR
   * @return List of found DbxFiles in solr
   * @throws DbxApiException
   * @throws DbxException
   * @throws SolrServerException
   * @throws IOException
   */
  @GET
  @Path("search")
  @Produces({MediaType.APPLICATION_JSON})
  public List search(@QueryParam("tags") List<String> tags, @QueryParam("offset") int offset, @QueryParam("range") int range,
      @QueryParam("operator") String operator) throws DbxApiException, DbxException, SolrServerException, IOException {

    return fileHandler.queryFile( tags, offset, range, operator );

  }

  /**
   * Search file by specific tags
   * @param tags specify tags you want to search by (must be at least one)
   * @param offset optional parameter used for pagination, enter positive numeric value here, if left empty fallbacks to 0
   * @param range optional parameter used for pagination, enter positive numeric value here, if left empty fallbacks to 10
   * @param operator optional boolean operator, possible values are OR and AND, if left empty fallbacks to OR
   * @return List of found dropbox file object represented in a json
   * @throws DbxApiException
   * @throws DbxException
   * @throws SolrServerException
   * @throws IOException
   */
  @GET
  @Path("search-dbx")
  @Produces({MediaType.APPLICATION_JSON})
  public List search_dbx(@QueryParam("tags") List<String> tags, @QueryParam("offset") int offset,
      @QueryParam("range") int range, @QueryParam("operator") String operator)
      throws DbxApiException, DbxException, SolrServerException, IOException {

    List<DbxFile> result = fileHandler.queryFile( tags, offset, range, operator );

    ArrayList<Metadata> files = new ArrayList<Metadata>();
    for (DbxFile tag : result) {
      try {
        files.add( dbxFilesHandler.getMetadata( tag.getPath() ) );
      }
      catch (Exception e) {
        e.printStackTrace();
      }

    }

    return files;
  }

  /**
   * Download dropbox files found for specified tags as a zip file
   * @param tags list of tag you search for
   * @return dropbox files to download as a zip
   * @throws DownloadErrorException
   * @throws SolrServerException
   * @throws IOException
   * @throws DbxException
   */
  @GET
  @Path("download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response download(@QueryParam("tags") List<String> tags)
      throws DownloadErrorException, SolrServerException, IOException, DbxException {

    List<DbxFile> files = fileHandler.get( tags );
    if (files.isEmpty())
      return Response.noContent().entity( "No files found for togs " + tags ).build();

    return Response.ok().entity( dbxFilesHandler.download( files ) )
      .header( "Content-Disposition", "attachment; filename=" + zipName( tags ) + ".zip" ).build();
  }

  /**
   * Helper method to retrieve files from dropbox
   * @param folder, specify * to retrieve all files in a root folder, otherwise, specify valid dropbox folder path
   * @return list of dropbox files
   * @throws SolrServerException
   * @throws IOException
   * @throws ListFolderErrorException
   * @throws DbxException
   */
  @GET
  @Path("/dbx/{folder}")
  public List<Metadata> allFiles(@PathParam("folder") String folder)
      throws SolrServerException, IOException, ListFolderErrorException, DbxException {
    return dbxFilesHandler.getAll( folder.equals( "*" ) ? "" : folder );
  }

  private String zipName(List<String> tags) {
    String zipName = "files_for";
    for (String tag : tags) {
      zipName += "_" + tag;
    }

    return zipName;
  }
}
