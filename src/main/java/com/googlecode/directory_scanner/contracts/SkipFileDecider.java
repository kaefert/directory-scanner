package com.googlecode.directory_scanner.contracts;

import com.googlecode.directory_scanner.domain.PathVisit;

public interface SkipFileDecider {

    public boolean decideFileSkip(PathVisit visit);
}
