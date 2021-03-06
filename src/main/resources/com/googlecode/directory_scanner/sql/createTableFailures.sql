CREATE TABLE IF NOT EXISTS failures ( 
     id INT NOT NULL AUTO_INCREMENT PRIMARY KEY 
    ,path VARCHAR(1000) NOT NULL 
	,scanDir_id INT NULL 
	,scandate DateTime NOT NULL 
	,size BIGINT NULL 
	,sizeRead BIGINT NULL 
	,error VARCHAR(1000) NOT NULL 
	,FOREIGN KEY (scanDir_id) REFERENCES directories(id) ON DELETE CASCADE 
) ENGINE=InnoDB;