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

CREATE INDEX files_sha1 ON files (sha1);
CREATE INDEX files_size ON files (size);

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
/*************************************************/

EXPLAIN EXTENDED
SELECT 
	f.dir_id,
	f.filename, 
	f.id, 
	f.size, 
	f.scandate, 
	f.sha1, 
	f.lastmodified 
FROM files f 
WHERE EXISTS ( /* same sha1 but different size */ 
	SELECT ff.id 
	FROM files ff
	WHERE ff.sha1 = f.sha1 
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

/*************************************************/

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

SELECT COUNT(f.id) FROM files f
INNER JOIN directories d 
	ON d.id = f.dir_id
WHERE d.path LIKE '/media/d170ed69-ed03-4bf0-8bbd-32c4b3b8cc49%'



-------------------------------------------------

-- This checks the SHA1 collisions
SELECT
  sha1
FROM files
GROUP BY sha1
HAVING COUNT(*)>1
AND MIN(size)<>MAX(size)

-- This checks for directory duplicates
SELECT
  MIN(path) AS path
FROM directories
GROUP BY path
HAVING COUNT(*)>1

-- This checks for file duplicates
SELECT
  MIN(f.id) AS id
FROM files AS f
INNER JOIN files AS ff 
   ON f.dir_id=ff.dir_id
   AND f.filename=ff.filename
GROUP BY f.id
HAVING COUNT(*)>1

----------------------------------------
SELECT * FROM files WHERE id IN (1, 749811)

INSERT INTO files (
	dir_id,
	scanDir_id,
	filename,
	sha1,
	scandate,
	lastmodified,
	size
) VALUES (
	2,
	4616,
	'MobileFrontend-master-12384ac.tar.gz',
	0x71d22b11fb9e80d96411c5ff9be48f45c079a5a7,
	'2012-08-15 23:49:01.0',
	'2012-07-28 10:55:15.0',
	-123
)

INSERT INTO files (
	dir_id,
	scanDir_id,
	filename,
	sha1,
	scandate,
	lastmodified,
	size
) VALUES (
	5,
	4616,
	'blackberry.css',
	0xdea8d623f346d846c2c6f395849fca753ce0112f,
	'2012-08-15 23:49:01.0',
	'2012-06-27 04:15:43.0',
	-322
)
 


UPDATE files SET size = -68967
WHERE id = 749811

DELETE FROM files WHERE dir_id = 2 and filename = 'MobileFrontend-master-12384ac.tar.gz_othersize';

SELECT * FROM files WHERE filename = 'MobileFrontend-master-12384ac.tar.gz';
SELECT * FROM files WHERE filename = 'blackberry.css';
SELECT * FROM files WHERE id = 15
DELETE FROM files WHERE id IN (749815, 749816);

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
WHERE f.sha1 = 0x71d22b11fb9e80d96411c5ff9be48f45c079a5a7


EXPLAIN SELECT sha1
FROM files
GROUP BY sha1
HAVING COUNT(*)>1
AND MIN(size)<>MAX(size);

SELECT
  f.id, d.path, f.filename
FROM files AS f
INNER JOIN files AS ff 
   ON f.id <> ff.id
   AND f.dir_id=ff.dir_id
   AND f.filename=ff.filename
INNER JOIN directories d 
   ON d.id = f.dir_id

SELECT
  f.id, f.dir_id, f.filename
FROM files AS f
INNER JOIN files AS ff 
   ON f.id <> ff.id
   AND f.dir_id=ff.dir_id
   AND f.filename=ff.filename
GROUP BY f.id;

HAVING COUNT(*)>1

ALTER TABLE directories ADD CONSTRAINT uc_dir_path UNIQUE (path)
ALTER TABLE directories DROP INDEX uc_dir_path


ALTER TABLE files ADD CONSTRAINT uc_files UNIQUE(dir_id, filename)
ALTER TABLE files DROP INDEX uc_files

CREATE INDEX ix_files_dir_id ON files (dir_id);
CREATE INDEX dir_id ON files (dir_id);
CREATE INDEX filename ON files (filename);

DROP INDEX uc_files ON files;
DROP INDEX files_size ON files;
DROP INDEX ix_files_dir_id ON files;

DROP INDEX filename_dir ON files;

/* constraint that will fail if the same file has been mistakenly inserted twice */
CREATE UNIQUE INDEX filename_dir ON files (filename, dir_id)

/* index that will gain us a minute when selecting the duplicates with same filename & dir_id */
CREATE INDEX filename_dirid_id ON files (filename, dir_id, id);

CREATE INDEX sha1 ON files (sha1);
CREATE INDEX size ON files (size);
CREATE INDEX sha1_size ON files (sha1, size);

DROP INDEX sha1 ON files;
DROP INDEX size ON files;
DROP INDEX sha1_size ON files;

SHOW INDEX FROM files;
ANALYZE TABLE files;