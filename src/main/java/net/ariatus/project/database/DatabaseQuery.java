package net.ariatus.project.database;

import java.sql.Connection;

@FunctionalInterface
public interface DatabaseQuery<T> {

    T execute(Connection connection) throws Exception;
}