INSERT IGNORE INTO Feed (userId, actionId)
  (SELECT fromUserId, :actionId FROM relationship WHERE toUserId = :userId)
  UNION
  (SELECT userId, :actionId FROM followingphotoquest WHERE userId = :userId)