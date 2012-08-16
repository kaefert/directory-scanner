package com.googlecode.directory_scanner.contracts;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public interface SkipDecider {

    public boolean decideDirectorySkip(Path path, BasicFileAttributes attrs);

    public boolean decideFileSkip(Path path, BasicFileAttributes attrs);
}
