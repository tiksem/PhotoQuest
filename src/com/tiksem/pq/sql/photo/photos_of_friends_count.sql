SELECT count(*) FROM photo WHERE userId in
(SELECT toUserId FROM relationship WHERE fromUserId = :userId AND type = 0)
AND photoquestId = :photoquestId