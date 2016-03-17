SELECT path 
FROM directories 
GROUP BY path 
HAVING COUNT(*)>1
