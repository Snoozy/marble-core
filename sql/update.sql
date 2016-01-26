ALTER TABLE comment ADD status TINYINT NOT NULL DEFAULT 0;

CREATE TABLE referral (
    referral_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    CONSTRAINT `user__referral_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
)

alter table post add index `post__time_board_idx` (`board_id`, `time`);

alter table post add index `post__votes_idx` (`votes`);

CREATE TABLE notification_listener (
    id INT NOT NULL AUTO_INCREMENT,
    entity_id INT NOT NULL,
    entity_type TINYINT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (id),
    INDEX `notifications_listener__user_id_idx` (`user_id`),
    INDEX `notifications_listener__entity_id_entity_type_idx` (`entity_id`, `entity_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE notification (
    notification_id INT NOT NULL AUTO_INCREMENT,
    entity_id INT NOT NULL,
    entity_type TINYINT NOT NULL,
    action_type TINYINT NOT NULL,    
    title_user INT NOT NULL,
    user_id INT NOT NULL,
    count INT DEFAULT 0,
    `read` TINYINT DEFAULT 0,
    time BIGINT NOT NULL,
    PRIMARY KEY (`notification_id`),
    CONSTRAINT `user__notification_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    INDEX `notification_read_idx` (`read`),
    INDEX `notificiation_time_idx` (`time`),
    INDEX `notification__entity_id_entity_type_action_type_idx` (`entity_id`, `entity_type`, `action_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE conversation (
    conversation_id INT NOT NULL AUTO_INCREMENT,
    user1_id INT NOT NULL,
    user2_id INT NOT NULL,
    created_time BIGINT NOT NULL,
    updated_time BIGINT NOT NULL,
    `read` TINYINT DEFAULT 0,
    last_user TINYINT NOT NULL,
    preview VARCHAR(300),
    PRIMARY KEY (`conversation_id`),
    CONSTRAINT `user__conversation_user1_id_fk` FOREIGN KEY (`user1_id`) REFERENCES user(`user_id`),
    CONSTRAINT `user__conversation_user2_id_fk` FOREIGN KEY (`user2_id`) REFERENCES user(`user_id`),
    INDEX `conversation_user1_id_idx` (`user1_id`),
    INDEX `conversation_user2_id_idx` (`user2_id`),
    INDEX `conversation_updated_time_idx` (`updated_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE message (
    message_id INT NOT NULL AUTO_INCREMENT,
    conversation_id INT NOT NULL,
    content VARCHAR(5000),
    user_id INT NOT NULL,
    time BIGINT NOT NULL,
    PRIMARY KEY (`message_id`),
    CONSTRAINT `conversation__message_fk` FOREIGN KEY (`conversation_id`) REFERENCES conversation(`conversation_id`),
    CONSTRAINT `user__message_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    INDEX `message_time_idx` (`time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

ALTER TABLE user_info ADD inbox_count INT DEFAULT 0 AFTER reputation;

ALTER TABLE conversation ADD last_user TINYINT NOT NULL AFTER `read`;
