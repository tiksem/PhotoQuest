SELECT count(*)
FROM user
WHERE
  nameData LIKE :query
  AND location = :location
  AND gender = :gender
LIMIT 0, 200