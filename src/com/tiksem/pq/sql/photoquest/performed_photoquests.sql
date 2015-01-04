SELECT photoquest.* FROM  photoquest 
WHERE photoquest.ID IN 
      (SELECT photoquestId FROM performedphotoquest WHERE userId = :userId)
ORDER BY :orderBy DESC LIMIT :offset, :limit