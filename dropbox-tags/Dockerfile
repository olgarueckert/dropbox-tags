FROM websphere-liberty:webProfile8
COPY src/main/liberty/config/server.xml /opt/ibm/wlp/usr/servers/defaultServer/
COPY /target/dropbox-tags.war /opt/ibm/wlp/usr/servers/defaultServer/dropins/
RUN installUtility install apiDiscovery-1.0 --acceptLicense
ENV LICENSE accept