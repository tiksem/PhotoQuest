DELETE FROM `like` WHERE commentId = :commentId;
DELETE FROM reply WHERE type = 2 and id = :commentId;
DELETE FROM `comment` WHERE id = :commentId