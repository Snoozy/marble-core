# --- !Ups

CREATE TABLE user (
    user_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(25) NOT NULL UNIQUE,
    name VARCHAR(25) NOT NULL,
    photo INT DEFAULT NULL,
    photo_name VARCHAR(40) DEFAULT NULL,
    PRIMARY KEY (user_id),
    FULLTEXT(name),
    FULLTEXT(username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE user_info (
    user_id INT UNSIGNED NOT NULL,
    password VARCHAR(105) NOT NULL,
    email VARCHAR(255) UNIQUE,
    time BIGINT NOT NULL,
    reputation INT NOT NULL,
    bio VARCHAR(200),
    PRIMARY KEY(user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE admin (
    admin_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    PRIMARY KEY (admin_id),
    CONSTRAINT `user__admin_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE social_user (
    social_user_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    social_id VARCHAR(100),
    PRIMARY KEY (social_user_id),
    CONSTRAINT `user__social_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE board (
    board_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    time BIGINT NOT NULL,
    description VARCHAR(500) CHARACTER SET utf8mb4,
    creator_id INT,
    followers INT,
    photo INT,
    privacy TINYINT,
    CONSTRAINT `user__board_fk` FOREIGN KEY (`creator_id`) REFERENCES user(`user_id`),
    PRIMARY KEY (board_id),
    FULLTEXT(name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE post (
    post_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    title VARCHAR(100) CHARACTER SET utf8mb4,
    data VARCHAR(10000) CHARACTER SET utf8mb4,
    board_id INT NOT NULL,
    repost_id INT,
    votes INT NOT NULL,
    comment_count INT NOT NULL,
    media VARCHAR(200) DEFAULT '',
    time BIGINT NOT NULL,
    INDEX `post__user_id_idx` (`user_id` DESC),
    INDEX `post__board_id_idx` (`board_id` DESC),
    INDEX `post__time_idx` (`time` DESC),
    CONSTRAINT `board__post_fk`FOREIGN KEY (`board_id`) REFERENCES board(`board_id`),
    CONSTRAINT `user__post_fk`FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),    
    PRIMARY KEY (post_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE comment (
    comment_id INT NOT NULL AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    data VARCHAR(10000) CHARACTER SET utf8mb4 NOT NULL,
    time BIGINT NOT NULL,
    path VARCHAR(766) CHARACTER SET latin1,
    votes INT,
    CONSTRAINT `user__comment_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    CONSTRAINT `post__comment_fk` FOREIGN KEY (`post_id`) REFERENCES post(`post_id`),
    INDEX `comment__path_idx` (`path` DESC),
    PRIMARY KEY (comment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE post_vote (
    post_vote_id INT NOT NULL AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    time BIGINT NOT NULL,
    value TINYINT NOT NULL,
    PRIMARY KEY (post_vote_id),
    CONSTRAINT `post__post_vote_fk` FOREIGN KEY (`post_id`) REFERENCES post(`post_id`),
    CONSTRAINT `user__post_vote_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    INDEX `post_vote__post_id_user_id_idx` (`post_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE comment_vote (
    comment_vote_id INT NOT NULL AUTO_INCREMENT,
    comment_id INT NOT NULL,
    user_id INT NOT NULL,
    time BIGINT NOT NULL,
    value TINYINT NOT NULL,
    PRIMARY KEY (comment_vote_id),
    CONSTRAINT `comment__comment_vote_fk` FOREIGN KEY (`comment_id`) REFERENCES comment(`comment_id`),
    CONSTRAINT `user__comment_vote_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    INDEX `comment_vote__comment_id_user_id_idx` (`comment_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE user_to_board (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    board_id INT NOT NULL,
    time BIGINT NOT NULL,
    CONSTRAINT `user__user_to_board_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    CONSTRAINT `board__user_to_board_fk` FOREIGN KEY (`board_id`) REFERENCES board(`board_id`),
    INDEX `user_to_board__user_id_board_id_idx` (`user_id`, `board_id`),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE media (
    media_id INT NOT NULL AUTO_INCREMENT,
    media_type TINYINT NOT NULL,
    media_name VARCHAR(40) NOT NULL,
    PRIMARY KEY (media_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

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
    preview VARCHAR(300) CHARACTER SET utf8mb4,
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
    content VARCHAR(5000) CHARACTER SET utf8mb4,
    user_id INT NOT NULL,
    time BIGINT NOT NULL,
    PRIMARY KEY (`message_id`),
    CONSTRAINT `conversation__message_fk` FOREIGN KEY (`conversation_id`) REFERENCES conversation(`conversation_id`),
    CONSTRAINT `user__message_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    INDEX `message_time_idx` (`time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE comment_flag (
    comment_flag_id INT NOT NULL AUTO_INCREMENT,
    comment_id INT NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT `comment_flag__user_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    CONSTRAINT `comment_flag__comment_fk` FOREIGN KEY (`comment_id`) REFERENCES comment(`comment_id`),
    PRIMARY KEY (`comment_flag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE post_flag (
    post_flag_id INT NOT NULL AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT `post_flag__user_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    CONSTRAINT `post_flag__comment_fk` FOREIGN KEY (`post_id`) REFERENCES post(`post_id`),
    PRIMARY KEY (`post_flag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE user_block (
    block_id INT NOT NULL AUTO_INCREMENT,
    blocker_id INT NOT NULL,
    blockee_id INT NOT NULL,
    CONSTRAINT `user_blocker__user_fk` FOREIGN KEY (`blocker_id`) REFERENCES user(`user_id`),
    CONSTRAINT `user_blockee__user_fk` FOREIGN KEY (`blockee_id`) REFERENCES user(`user_id`),
    PRIMARY KEY (`block_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE password_reset (
    reset_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    token VARCHAR(30) NOT NULL,
    time BIGINT NOT NULL,
    PRIMARY KEY (`reset_id`),
    CONSTRAINT `password_reset_user__user_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE apple_device_token (
    device_token_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    token VARCHAR(30) NOT NULL,
    PRIMARY KEY (`device_token_id`),
    CONSTRAINT `apple_device_token__user_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
