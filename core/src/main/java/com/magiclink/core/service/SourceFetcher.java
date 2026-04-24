package com.magiclink.core.service;

import com.magiclink.core.model.Source;
import java.util.List;

public interface SourceFetcher {
    String fetch(Source source) throws Exception;
    boolean supports(Source source);
}
