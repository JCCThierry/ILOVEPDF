
# Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
# Click nbfs://nbhost/SystemFileSystem/Templates/Other/Dockerfile to edit this template

FROM tomcat:10.1-jdk17

# Suppression des applications par défaut de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Installation de wget pour le téléchargement
RUN apt-get update && apt-get install -y wget

# Téléchargement de votre fichier .war depuis votre lien Google Drive
# Note : L'ID est celui que vous venez de me donner
RUN wget --no-check-certificate 'https://docs.google.com/uc?export=download&id=18Q4yk-5TgfxeZa9u6d5qPsvOPE935Eup' -O /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
