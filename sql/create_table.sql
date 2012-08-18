
/*************************************************************************************/
/**************************** sql_createTableDirectories *****************************/ 
/*************************************************************************************/
CREATE TABLE directories ( 
     id INT NOT NULL AUTO_INCREMENT PRIMARY KEY 
    ,path VARCHAR(1000) NOT NULL 
	,scanDir_id INT NULL 
	,scandate DateTime NOT NULL 
	,finished DateTime NULL 
	,FOREIGN KEY (scanDir_id) REFERENCES directories(id) ON DELETE CASCADE 
) ENGINE=InnoDB;

/*************************************************************************************/
/******************************* sql_createTableFiles ********************************/ 
/*************************************************************************************/
CREATE TABLE files ( 
	 id INT NOT NULL AUTO_INCREMENT PRIMARY KEY 
	,dir_id INT NOT NULL 
	,scanDir_id INT NULL
	,filename VARCHAR(255) NOT NULL 
	,sha1 BINARY(20) NULL 
	,scandate DateTime NOT NULL 
	,lastmodified DateTime NOT NULL 
	,size BIGINT NOT NULL 
	,FOREIGN KEY (dir_id) REFERENCES directories(id) ON DELETE CASCADE 
	,FOREIGN KEY (scanDir_id) REFERENCES directories(id) ON DELETE CASCADE 
) ENGINE=InnoDB;

/*************************************************************************************/
/****************************** sql_createTableFailures ******************************/
/*************************************************************************************/
CREATE TABLE failures ( 
     id INT NOT NULL AUTO_INCREMENT PRIMARY KEY 
    ,path VARCHAR(1000) NOT NULL 
	,scanDir_id INT NULL 
	,scandate DateTime NOT NULL 
	,size BIGINT NULL 
	,sizeRead BIGINT NULL 
	,error VARCHAR(1000) NOT NULL 
	,FOREIGN KEY (scanDir_id) REFERENCES directories(id) ON DELETE CASCADE 
) ENGINE=InnoDB;





/*************************************************************************************/
/*************************************************************************************/
/******************* OLD STUFF, IGNORE EVERYTHING BELOW THIS BLOCK *******************/
/*************************************************************************************/
/*************************************************************************************/

DROP TABLE files;

CREATE TABLE files (
          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY
         ,sha1 VARCHAR(40)
         ,scandate timestamp NOT NULL
         ,path VARCHAR(1000) NOT NULL
         ,size BIGINT NOT NULL
 );
 
 SELECT * FROM files;
 
 alter table files change column size size bigint not null;
 alter table files change column size size_long long not null;
 
 ALTER TABLE files
 RENAME TO files_old;
 
 CREATE TABLE files (
	  path VARCHAR(1000) NOT NULL PRIMARY KEY
	 ,sha1 VARCHAR(40)
	 ,scandate timestamp NOT NULL
	 ,size DOUBLE NOT NULL
 );
 
 CREATE TABLE directories_done (
 	path VARCHAR(1000) NOT NULL PRIMARY KEY
 );
 
INSERT INTO files (path, sha1, scandate, size)
	SELECT o.path, o.sha1, o.scandate, o.size
	FROM files_old o
	WHERE NOT EXISTS(
		SELECT n.path
		FROM files n
		WHERE n.path = o.path
	AND NOT EXISTS(
		SELECT i.path
		FROM files_old i
		WHERE i.path = o.path
		AND i.id > o.id
	);

	DELETE FROM files WHERE path LIKE '/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/Daten_BIG/Fremd%'
	
SELECT count(id) FROM files_old;
SELECT count(path) FROM files;
SELECT count(path) FROM directories_done;

INSERT INTO directories_done VALUES ('/media/9ff210fe-fd7d-48ac-b2f5-9f8fb94fda07/Daten/Daten_BIG/.unison/disabled')

SELECT path FROM directories_done

SELECT * FROM directories_done

