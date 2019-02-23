FROM centos

ADD jre-8u201-linux-x64.tar.gz /usr/local
ADD fakehummer-1.0.1.tar /usr/local

ENV JAVA_HOME=/usr/local/jre1.8.0_201
ENV APP_HOME=/usr/local/fakehummer-1.0.1
ENV PATH=$JAVA_HOME/bin:$APP_HOME/bin:$PATH

CMD ["fakehummer"]
EXPOSE 8080
