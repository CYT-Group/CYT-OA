package com.cyt.center.module.portal.service.support;

import com.cyt.center.core.exception.BizException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalDocumentStorageTest {
    @Test void rejectsPathsOutsidePrivateNamespaces() {
        var storage=new PortalDocumentStorage();
        for(String path:new String[]{"../secret","resumes/../../secret.pdf","C:/secret.pdf","images/nested/file.html","brand-docs/../../key"})assertThrows(BizException.class,()->storage.resolve(path));
        assertTrue(storage.resolve("resumes/abc123.pdf").startsWith(storage.root()));
    }
    @Test void publicImagesCannotReadPrivateResumeOrHtml() {
        var images=new PortalImageStorage(new PortalDocumentStorage());
        assertThrows(BizException.class,()->images.mime("abc.html"));
        assertThrows(BizException.class,()->images.mime("../resumes/test.pdf"));
        assertThrows(BizException.class,()->images.upload("news","test.png","<html>unsafe</html>".getBytes()));
    }
}
