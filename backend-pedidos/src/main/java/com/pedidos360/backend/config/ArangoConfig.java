package com.pedidos360.backend.config;

import com.arangodb.ArangoDB;
import com.arangodb.springframework.annotation.EnableArangoRepositories;
import com.arangodb.springframework.config.ArangoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableArangoRepositories(basePackages = "com.pedidos360.backend.repository")
public class ArangoConfig implements ArangoConfiguration {

    @Value("${arangodb.host:857c16557e9b.arangodb.cloud}")
    private String host;

    @Value("${arangodb.port:8529}")
    private int port;

    @Value("${arangodb.user:root}")
    private String user;

    @Value("${arangodb.password}")
    private String password;

    @Value("${arangodb.database:_system}")
    private String database;

    @Value("${arangodb.useSsl:true}")
    private boolean useSsl;

    @Override
    public ArangoDB.Builder arango() {
        return new ArangoDB.Builder() //Configura las credenciales y el cliente de conexión a ArangoDB
                .host(host, port)
                .user(user)
                .password(password)
                .useSsl(useSsl);
    }

    @Override
    public String database() {
        return database; //Aqui se define el nombre de la bases de datos que se utilizara
    }
}
