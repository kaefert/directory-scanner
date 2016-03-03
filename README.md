Recursively scan directories & store sha1 in db + reporting
 -> flexible (and complex) tool that goes farther than unison & fslint

If you have some experience with managing disks with overlapping contents, like mirrors or backups, you probably use something like rsync or unison to compare these. If you ever wondered if you have duplicate files, you probably used something like fslint to find them and clean them up.

My problems with those tools was: 1. Not very flexible. two types of questions can be answered: type 1: are my mirrors in sync? type 2: do I have duplicate files? 1. Every time you want a question answered, you have to rescan all your files, unison has tempfiles that make rescanning quicker, but still not as nice as having all info in a sql database.

Therefore I programmed this. It's a little Java-Program that recursively walks a filetree and stores the results in a database. NEW: I've added reporting tools that give answers to quite a big range of questions. You can also recheck a previously scanned directory and deleted directories & files will be removed from the database, and changed will be rescanned and updated in the database.

Of course you can also use this database to query all kinds of other information out of it.

WARNING: This project will only run with a java 1.7+ runtime, since I'm using the "new" java.nio.file.FileSystems framework.

The newer versions have a GUI, but the CLI can also still be used, but the CLI does not have options for reporting. To see the CLI options just add an argument, if it doesn't match the available options, help is printed.

For ease of use I also embedded the jar files for an h2 database into the jar that you find in the download section (starting from v0.0.2) and automatically fallback to this if you don't have an adapted properties file with useable database config options.

If you want to use another database beside h2 and mysql you will need to add the jdbc driver to the classpath when running the application. (I've chosen h2 as fallback option since this review stated that its the most performant embeddable java sql database http://jars.de/java/embedded-java-datenbanken)
