(SELECT enName as value, id FROM Country WHERE enName like :query ORDER BY enName)
UNION
(SELECT ruName as value, id FROM Country WHERE ruName like :query ORDER BY ruName)
LIMIT :limit