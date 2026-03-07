FROM tomcat:10.1-jdk17

RUN rm -rf /usr/local/tomcat/webapps/*

# On installe wget et curl
RUN apt-get update && apt-get install -y wget curl

# Nouvelle méthode pour forcer le téléchargement du gros fichier
RUN curl -L "https://drive.google.com/uc?export=download&id=18Q4yk-5TgfxeZa9u6d5qPsvOPE935Eup" > /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
