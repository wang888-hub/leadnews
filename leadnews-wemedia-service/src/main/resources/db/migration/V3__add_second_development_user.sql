INSERT INTO wm_user(ap_user_id, name, password_hash, nickname, phone)
VALUES (NULL, 'wemedia_other', '$2a$10$72y79aQx6pCuCdwwCbyE4OT8ENZpEajFAS6LrwNkHmwUGKA77P9uu', '其他自媒体', '13900000001')
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname);
