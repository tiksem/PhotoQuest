SELECT
COUNT(IF(MATCH (keywords) AGAINST (:query IN NATURAL LANGUAGE MODE), 1, NULL))
AS count
FROM photoquestsearch, photoquest
WHERE photoquest.userId = :userId And photoquest.id = photoquestsearch.photoquestId
LIMIT 0, 100