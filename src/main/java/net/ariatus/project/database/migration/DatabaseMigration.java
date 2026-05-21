package net.ariatus.project.database.migration;

import java.sql.Connection;

public interface DatabaseMigration {

    String id();

    void migrate(Connection connection) throws Exception;
}