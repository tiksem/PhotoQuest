SELECT * FROM photo WHERE userId in
(SELECT toUserId FROM relationship WHERE fromUserId = :userId AND type = 0)
AND photoquestId = :photoquestId
AND :orderBy :operator :orderByValue
AND id :operator :photoId
LIMIT 0, 1