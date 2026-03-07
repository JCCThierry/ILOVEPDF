FROM tomcat:10.1-jdk17

# Nettoyage des dossiers par défaut de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Désactiver le port de shutdown (8005) pour éviter l'erreur "Invalid shutdown command" sur Render
RUN sed -i 's/port="8005"/port="-1"/' /usr/local/tomcat/conf/server.xml

# Installation de curl
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Téléchargement du WAR depuis Dropbox
RUN curl -L "https://www.dropbox.com/scl/fi/2mxsiek33hvuueqmwcguw/ILOVEPDF.war?rlkey=0aoyxsiawf1lx2hd90cd7emcr&st=zfdyyrnz&dl=1" -o /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

# Correction du CMD (il ne faut pas de texte après "run")
CMD ["catalina.sh", "run"]
