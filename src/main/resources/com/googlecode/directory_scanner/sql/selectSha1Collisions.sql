SELECT sha1 
FROM files 
GROUP BY sha1 
HAVING COUNT(*)>1 
AND MIN(size)<>MAX(size)