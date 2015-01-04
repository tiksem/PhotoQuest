SELECT * FROM user
  JOIN
  (
    SELECT user.id as userId
    FROM user
    WHERE
      namedData LIKE :query AND location = :location
      AND gender = :gender
    LIMIT 0, 200
  ) as sel ON sel.userId = user.ID
ORDER BY :orderBy DESC LIMIT :offset, :limit