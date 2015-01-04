SELECT count(*)
FROM user
WHERE
  namedData LIKE :query
  AND gender = :gender
LIMIT 0, 200