#FROM regi.acloud.run/library/cocktail-openjdk:centos7-8u212.B000008
FROM openjdk:8u212-jre-alpine3.9

#RUN localedef -c -i ko_KR -f UTF-8 ko_KR.UTF-8
#ENV LC_ALL=ko_KR.UTF-8
#ENV LANG=ko_KR.UTF-8
ENV LC_ALL=en_US.utf8
ENV LANG=en_US.utf8

ENV API_VERSION="4.3.2"

LABEL maintainer="Acornsoft"
LABEL "acloud.run"="Acornsoft"
LABEL "product"="Cocktail"
LABEL "service-mode"="prod"
LABEL version=${API_VERSION}

# make sure root login is disabled
RUN sed -i -e 's/^root::/root:!:/' /etc/shadow
RUN apk update && apk upgrade && rm -Rf /var/cache/apk/*
RUN apk add --no-cache msttcorefonts-installer fontconfig
RUN update-ms-fonts

RUN mkdir -p /home/acloud/api
COPY repo/acloud-api/target/acloud-api-0.0.1-SNAPSHOT.jar /home/acloud/api/acloud-api.jar
COPY repo/aws-eks/aws-iam-authenticator /home/acloud/aws-iam-authenticator

ENV PATH "/home/acloud/:$PATH"

RUN chmod 755 /home/acloud/aws-iam-authenticator
#RUN yum -y upgrade && \
#    yum clean all

# Run as non-root
RUN addgroup -g 1001 -S cocktail && adduser -u 1001 -S cocktail -G cocktail
RUN chown -R cocktail:cocktail /home/acloud
USER cocktail

EXPOSE 8080

CMD java $JAVA_OPTIONS -jar /home/acloud/api/acloud-api.jar --API_VERSION=$API_VERSION