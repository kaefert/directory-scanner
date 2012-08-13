package com.googlecode.directory_scanner.contracts;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public interface DirectorySkipDecider {
    public boolean decideDirectorySkip(Path path, BasicFileAttributes attrs);
}
