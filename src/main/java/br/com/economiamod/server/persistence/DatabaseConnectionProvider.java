package br.com.economiamod.server.persistence;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnectionProvider extends AutoCloseable {
    Connection getConnection() throws SQLException;

    @Override
    void close();
}

