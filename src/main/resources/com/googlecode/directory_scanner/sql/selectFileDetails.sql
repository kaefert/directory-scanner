SELECT 
	f.id, 
	f.size, 
	f.lastmodified, 
	f.scandate 
FROM files f 
INNER JOIN directories d 
	ON d.id = f.dir_id 
WHERE d.path = ? 
  AND f.filename = ? 
ORDER BY f.sha1
