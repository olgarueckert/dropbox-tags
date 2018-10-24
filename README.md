# Dropbox Tags REST API

This Rest API extends Dropbox API functionality by introducing tags.


## Run Application on Docker

To run the application you need running Solr instance as well as Dropbox API app with generated access token (see below (*) Register a Dropbox API app).

You can configure these two services in `docker-compose.yaml` which is included into the source code.
Following environment variables need to be modified:

	 DBX_ACCESS_TOKEN - generated access token for dropbox
	 SOLR_URL  - solr ulr, for example : http://host.docker.internal:8983/solr/dropbox-tags, where dropbox-tags is the name for the collection to store the tags
	 ZIP_MAX_SIZE  - maximum size for the zip that can be downloaded in MB

### Steps to run docker-compose.yaml

1. checkout `dropbox-tags`, navigate to the `dropbox-tags` maven project and build it using `mvn clean install` command
2. now you can execute `docker-compose up` 
3. Application should be available at `http://localhost:3000/dropbox-tags`


### Steps to create docker image using maven build
1. checkout `https://github.com/olgarueckert/dropbox-tags.git`, navigate to the `dropbox-tags/dropbox-tags` maven project and build it using `mvn clean install` command
2. execute `mvn clean install -P build-docker-image` to build the image
3. now you can execute `docker run -e "DBX_ACCESS_TOKEN=[]" -e "SOLR_URL=[]" -e "ZIP_MAX_SIZE=[]"  --name dropbox-tags -p 3000:9080 -it openliberty-dropbox-tags:1.0-SNAPSHOT` to start docker container (replace `[]` with your env variables values)

### Accessing the application
Application is running under the context root `dropbox-tags`. To get the overview or to test the available API please go to `localhost:3000/api/explorer`. Some javadocs regarding REST API can be found here `http://localhost:3000/dropbox-tags/doc/` 

(Please note -  download functionality is currently not working in build-in liberty swagger ui, so to test zip download you will have to call url direclty in browser or use `curl` command, for example: `curl -X GET --header 'Accept: application/octet-stream' 'http://localhost:3000/dropbox-tags/api/files/download?tags=private' --output private.zip`)
  

##### (*) Register a Dropbox API app 

To use the Dropbox API, you'll need to register a new app in the [App Console](https://www.dropbox.com/developers/apps). Select Dropbox API app and choose your app's permission. You'll need to use the app key created with this app to access API v2.In order to make calls to the API, you'll need an instance of the Dropbox object. To instantiate, pass in the access token for the account you want to link. (Tip: You can [generate an access token](https://blogs.dropbox.com/developers/2014/05/generate-an-access-token-for-your-own-account/) for your own account through the [App Console](https://www.dropbox.com/developers/apps)).
Source: https://github.com/dropbox/dropbox-sdk-java.



