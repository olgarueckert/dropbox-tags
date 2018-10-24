package de.rueckert.task.business;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import org.apache.solr.client.solrj.SolrServerException;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.Metadata;
import de.rueckert.task.entity.DbxFile;
import de.rueckert.task.utils.DbxTagsAudit;
import de.rueckert.task.utils.DbxTagsException;

@Stateless
@Interceptors(DbxTagsAudit.class)
public class DbxFileHandler {

  private static final String ZIP_MAX_SIZE = System.getenv( "ZIP_MAX_SIZE" );

  @Inject
  DbxClientProvider client;

  public byte[] download(List<de.rueckert.task.entity.DbxFile> files)
      throws SolrServerException, IOException, DownloadErrorException, DbxException {

    Path path = getTempPath();

    for (DbxFile f : files) {

      DbxDownloader<FileMetadata> downloader = client.getDownloader( f );
      try {
        String name = path + File.separator + new File( f.getPath() ).getName();

        String absolutePath = new File( name ).getAbsolutePath();
        FileOutputStream out = new FileOutputStream( absolutePath );
        downloader.download( out );
        out.close();
      }
      catch (DbxException ex) {
        ex.printStackTrace();
      }

    }

    return zipFiles( path );
  }

  public Path getTempPath() throws IOException {
    return Files.createTempDirectory( "temp-dbx" );
  }

  private byte[] zipFiles(Path path) throws IOException {
    File directory = path.toFile();
    String[] files = path.toFile().list();

    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ZipOutputStream zos = new ZipOutputStream( baos );
      byte bytes[] = new byte[2048];

      for (String fileName : files) {
        FileInputStream fis = new FileInputStream( directory.getPath() + File.separator + fileName );
        BufferedInputStream bis = new BufferedInputStream( fis );

        zos.putNextEntry( new ZipEntry( fileName ) );

        int bytesRead;
        while ((bytesRead = bis.read( bytes )) != -1) {
          zos.write( bytes, 0, bytesRead );
        }
        zos.closeEntry();
        bis.close();
        fis.close();
      }
      zos.flush();
      baos.flush();
      zos.close();
      baos.close();

      final int size = baos.size() / (1024 * 1024);

      if (size >= Integer.valueOf( ZIP_MAX_SIZE ))
        throw new DbxTagsException( "Size of the downloaded files should be below " + ZIP_MAX_SIZE + " mb", 412 );

      return baos.toByteArray();
    }

    finally {
      clearDirectory( path );
    }

  }

  public boolean isFile(String path) throws GetMetadataErrorException, DbxException {
    return client.isFile( path );
  }

  public Metadata getMetadata(String path) throws GetMetadataErrorException, DbxException {
    return client.getMetadata( path );
  }

  public List<Metadata> getAll(String path) throws ListFolderErrorException, DbxException {
    return client.getFiles( path );
  }

  public static void clearDirectory(Path path) throws IOException {
    Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, @SuppressWarnings("unused") BasicFileAttributes attrs) {
        file.toFile().deleteOnExit();
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult preVisitDirectory(Path dir, @SuppressWarnings("unused") BasicFileAttributes attrs) {
        dir.toFile().deleteOnExit();
        return FileVisitResult.CONTINUE;
      }
    } );
  }
}
