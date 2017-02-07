FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/projects.jar /projects/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/projects/app.jar"]
