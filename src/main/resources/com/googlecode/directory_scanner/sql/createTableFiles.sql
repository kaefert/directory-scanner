CREATE TABLE IF NOT EXISTS files ( 
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
