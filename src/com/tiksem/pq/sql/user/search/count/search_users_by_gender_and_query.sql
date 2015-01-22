SELECT count(*)
FROM user
WHERE
  nameData LIKE :query
  AND gender = :gender
LIMIT 0, 200