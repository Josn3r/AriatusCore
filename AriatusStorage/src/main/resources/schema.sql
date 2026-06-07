CREATE TABLE IF NOT EXISTS ariatus_storage_chests (
                                                      uuid VARCHAR(36) NOT NULL,
    chest_number INT NOT NULL,
    unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    unlocked_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (uuid, chest_number)
    );

CREATE TABLE IF NOT EXISTS ariatus_storage_contents (
                                                        uuid VARCHAR(36) NOT NULL,
    chest_number INT NOT NULL,
    content LONGTEXT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (uuid, chest_number)
    );