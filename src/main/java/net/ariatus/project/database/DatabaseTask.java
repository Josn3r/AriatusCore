package net.ariatus.project.database;

import java.sql.Connection;

@FunctionalInterface
public interface DatabaseTask {

    void execute(Connection connection) throws Exception;
}