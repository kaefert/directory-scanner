SELECT d.path 
FROM directories d 
WHERE d.finished > ? 
AND d.path LIKE ? 
ORDER BY d.path