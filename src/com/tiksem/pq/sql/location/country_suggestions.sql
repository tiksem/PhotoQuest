(SELECT * FROM Country WHERE enName like :query)
UNION
(SELECT * FROM Country WHERE ruName like :query)
ORDER BY id desc LIMIT :limit