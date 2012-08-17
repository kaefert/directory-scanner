select * FROM directories where finished IS NULL


SELECT * FROM files f
INNER JOIN directories d 
	ON d.id = f.dir_id 
WHERE d.path LIKE '/media/d170ed69-ed03-4bf0-8bbd-32c4b3b8cc49/test%'


SELECT d.path, d.id, f.filename, f.id, f.size, f.scandate, f.sha1 FROM files f INNER JOIN directories d ON d.id = f.dir_id WHERE ( /* at least given number of those duplicates are within the given path */ SELECT count(ff2.id) FROM files ff2 INNER JOIN directories dd2 ON dd2.id = ff2.dir_id WHERE ff2.sha1 = f.sha1 AND dd2.path LIKE '/%' ) >= 2 

SELECT 
	d.path, 
	d.id, 
	f.filename, 
	f.id, 
	f.size, 
	f.scandate, 
	f.sha1 
FROM files f 
INNER JOIN directories d 
	ON d.id = f.dir_id 
WHERE ( /* at least given number of those duplicates are within the given path */ 
	SELECT count(ff2.id) 
	FROM files ff2 
	INNER JOIN directories dd2 
		ON dd2.id = ff2.dir_id 
	WHERE ff2.sha1 = f.sha1 
	AND dd2.path LIKE '/%'
) >= 2
order by f.size DESC, f.sha1

CREATE INDEX files_sha1
ON files (sha1);

CREATE INDEX files_size
ON files (size);

select 1 from dbo.sysindexes where object_name(id)='files' and indid between 2 and 254

SHOW INDEX FROM files

SHOW INDEX FROM files WHERE Table = "files"




SELECT 
	f.size, 
	f.lastmodified, 
	f.scandate 
FROM files f 
INNER JOIN directories d 
	ON d.id = f.dir_id 
WHERE d.path = ? 
  AND f.filename = ? 
ORDER BY f.sha1




EXPLAIN
SELECT 
	d.path, 
	d.id, 
	f.filename, 
	f.id, 
	f.size, 
	f.scandate, 
	f.sha1, 
	f.lastmodified 
FROM files f 
INNER JOIN directories d 
	ON d.id = f.dir_id
INNER JOIN files ff
	ON ff.sha1 = f.sha1 
	AND ff.size <> f.size 
) 
OR EXISTS ( /* files with same name and path but different id */ 
	SELECT ff2.id 
	FROM files ff2 
	INNER JOIN directories dd2 
		ON dd2.id = ff2.dir_id 
	WHERE ff2.id <> f.id 
	AND ff2.filename = f.filename 
	AND dd2.path = d.path 
) 
ORDER BY f.sha1