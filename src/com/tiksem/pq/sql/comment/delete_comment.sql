DELETE FROM reply WHERE type = 3 AND id IN (SELECT id FROM `like` WHERE commentId = :commentId);
DELETE FROM `like` WHERE commentId = :commentId;
DELETE FROM reply WHERE type = 2 and id = :commentId;
DELETE FROM `comment` WHERE id = :commentId;
UPDATE `user` SET unreadRepliesCount = (SELECT COUNT(*) FROM Reply WHERE `read` = FALSE AND userId = `user`.id)