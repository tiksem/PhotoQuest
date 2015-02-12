SELECT user.* FROM user
  JOIN
  (
    SELECT user.id as userId
    FROM user
    WHERE
      nameData LIKE :query
      ::where
    LIMIT 0, :innerLimit
  ) as sel ON sel.userId = user.ID
ORDER BY ::orderBy LIMIT :offset, :limit