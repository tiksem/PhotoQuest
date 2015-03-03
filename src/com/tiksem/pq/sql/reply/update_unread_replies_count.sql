UPDATE `user` SET unreadRepliesCount = (SELECT COUNT(*) FROM Reply WHERE `read` = FALSE AND userId = :userId)
WHERE id = :userId