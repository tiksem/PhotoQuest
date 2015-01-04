SELECT photoquest.* FROM  photoquest 
WHERE photoquest.ID IN 
      (SELECT photoquestId FROM followingphotoquest WHERE userId = :userId)
ORDER BY :orderBy DESC LIMIT :offset, :limit