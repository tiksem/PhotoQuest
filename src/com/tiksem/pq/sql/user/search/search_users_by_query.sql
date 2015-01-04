SELECT * FROM user
  JOIN
  (
    SELECT user.id as userId
    FROM user
    WHERE
      namedData LIKE :query
    LIMIT 0, 200
  ) as sel ON sel.userId = user.ID
ORDER BY :orderBy DESC LIMIT :offset, :limit