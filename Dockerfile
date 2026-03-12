FROM tomcat:10.1-jdk17

# Nettoyage des dossiers par défaut de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Installation de curl pour le transfert
RUN apt-get update && apt-get install -y curl

# Téléchargement direct depuis ton Dropbox (Lien modifié avec dl=1)
RUN curl -L "https://www.dropbox.com/scl/fi/2mxsiek33hvuueqmwcguw/ILOVEPDF.war?rlkey=0aoyxsiawf1lx2hd90cd7emcr&st=zfdyyrnz&dl=1" -o /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]