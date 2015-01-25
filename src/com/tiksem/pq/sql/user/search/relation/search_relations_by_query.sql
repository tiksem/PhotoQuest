SELECT user.* FROM relationship
  JOIN
  (
    SELECT user.id as userId
    FROM user
    WHERE
      nameData LIKE :query
    LIMIT 0, 200
  ) as sel ON sel.userId = user.ID
ORDER BY :orderBy LIMIT :offset, :limit