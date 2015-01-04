INSERT IGNORE INTO Feed (userId, actionId)
  (SELECT fromUserId, :actionId FROM relationship WHERE toUserId = :userId)
  UNION ALL
  (SELECT userId, :actionId FROM followingphotoquest WHERE photoquestId = :photoquestId)