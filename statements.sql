INSERT INTO directories_done VALUES ('/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/Daten_BIG/.unison/disabled')

SELECT count(path) FROM files;                                                                            /* 893.528 */
SELECT count(path) FROM files WHERE path LIKE '/media/d3f3bcfc-e3d4-4f2f-8948-bf3df7112f09/Daten/%';      /* 232.786 */
SELECT count(path) FROM files WHERE path NOT LIKE '/media/d3f3bcfc-e3d4-4f2f-8948-bf3df7112f09/Daten/%';  /* 660.742 */


SELECT path FROM directories_done



/************************************************************/
/** create indexes so that complex select statements work ***/
/************************************************************/
CREATE INDEX sha1 ON files(sha1); 
CREATE INDEX path ON files(path);



/************************************************************/
/** Files that exist in directory 1 but not in directory 2 **/
/************************************************************/
SELECT n.*
FROM files n
WHERE n.path LIKE '/media/d3f3bcfc-e3d4-4f2f-8948-bf3df7112f09/Daten/%'
AND NOT EXISTS (
	SELECT o.path
	FROM files o
	WHERE o.path LIKE '/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/%'
	AND o.sha1 = n.sha1
);

/************************************************************/
/** look for differences **/
/************************************************************/
SELECT n.*, b.*
FROM files n

LEFT OUTER JOIN files b
ON b.path LIKE '/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/%'
 AND CONCAT('%', SUBSTRING(b.path, 60, CHAR_LENGTH(b.path)-60), '%')
LIKE CONCAT('%', SUBSTRING(n.path, 50, CHAR_LENGTH(n.path)-50), '%')

WHERE n.path LIKE '/media/d3f3bcfc-e3d4-4f2f-8948-bf3df7112f09/Daten/%'
AND NOT EXISTS (
	SELECT o.path
	FROM files o
	WHERE o.path LIKE '/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/%'
	AND o.sha1 = n.sha1
);



/************************************************************/
/** join only on filename without path **/
/************************************************************/
SELECT n.*, b.*
FROM files n

LEFT OUTER JOIN files b
ON b.path LIKE '/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/%'
AND SUBSTRING_INDEX(b.path, "/", -1) = SUBSTRING_INDEX(n.path, "/", -1)

WHERE n.path LIKE '/media/d3f3bcfc-e3d4-4f2f-8948-bf3df7112f09/Daten/%'
AND NOT EXISTS (
	SELECT o.path
	FROM files o
	WHERE o.path LIKE '/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/%'
	AND o.sha1 = n.sha1
);



/************************************************************/
/** look for differences **/
/************************************************************/
SELECT n.*
FROM files n

WHERE n.path LIKE '/home/kaefert/tmp/fotos dezember 2005/%'
AND NOT EXISTS (
	SELECT o.path
	FROM files o
	WHERE o.path LIKE '/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/%'
	AND o.sha1 = n.sha1
);




SELECT SUBSTRING_INDEX('/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/Daten_BIG/Entertainment/Music/lossy (mp3)/4_Unsorted/chantal kreviazuk - leaving on a jet plane.wma', '/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/Daten_BIG/', -1);

/************************************************************/
/**************** Files that exist only once ****************/
/************************************************************/
select a.* from files a
WHERE NOT EXISTS (
  SELECT b.path FROM files b
  WHERE b.sha1 = a.sha1
  AND b.path <> a.path
)









--<ScriptOptions statementTerminator=";"/>

ALTER TABLE `files`.`files` DROP PRIMARY KEY;

DROP TABLE `files`.`files`;

CREATE TABLE `files`.`files` (
	`path` VARCHAR(1000) NOT NULL,
	`sha1` VARCHAR(40),
	`scandate` TIMESTAMP DEFAULT 'CURRENT_TIMESTAMP' NOT NULL,
	`size` BIGINT NOT NULL,
	PRIMARY KEY (`path`)
) ENGINE=MyISAM;





