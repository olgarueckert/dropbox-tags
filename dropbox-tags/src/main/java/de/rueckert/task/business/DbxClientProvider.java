package de.rueckert.task.business;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.Metadata;

@Stateless
public class DbxClientProvider {
  private DbxClientV2 client;

  private static final String ACCESS_TOKEN = System.getenv( "DBX_ACCESS_TOKEN" );

  @PostConstruct
  public void init() {
    if (client == null) {
      DbxRequestConfig config = new DbxRequestConfig( "dropbox-tags" );
      client = new DbxClientV2( config, ACCESS_TOKEN );
    }

  }

  public DbxDownloader<FileMetadata> getDownloader(de.rueckert.task.entity.DbxFile f)
      throws DownloadErrorException, DbxException {
    return client.files().download( f.getPath() );
  }

  public boolean isFile(String path) throws GetMetadataErrorException, DbxException {
    return client.files().getMetadata( path ) instanceof FileMetadata;
  }

  public Metadata getMetadata(String path) throws GetMetadataErrorException, DbxException {
    return client.files().getMetadata( path );
  }

  public List<Metadata> getFiles(String path) throws ListFolderErrorException, DbxException {
    List<Metadata> entries = client.files().listFolder( path ).getEntries();
    ArrayList<Metadata> files = new ArrayList<Metadata>();
    for (Metadata metadata : entries) {
      if (metadata instanceof FileMetadata)
        files.add( metadata );
    }
    return files;
  }
}
