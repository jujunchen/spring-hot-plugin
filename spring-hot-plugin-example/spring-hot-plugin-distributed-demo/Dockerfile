# 指定 java 环境镜像
FROM openjdk:8
# 维护者信息
LABEL service-name=spring-hot-plugin-distributed-demo
# 复制文件到容器
COPY target/*.jar /application.jar

RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' > /etc/timezone

ENTRYPOINT ["java", "-jar", "-Xms256m", "-Xmx256m", "/application.jar"]
