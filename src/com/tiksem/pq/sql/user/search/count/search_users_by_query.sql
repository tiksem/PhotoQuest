SELECT count(*)
FROM user
WHERE
  nameData LIKE :query
LIMIT 0, 200
