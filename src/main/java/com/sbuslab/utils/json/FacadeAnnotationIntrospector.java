package com.sbuslab.utils.json;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.sbuslab.model.FacadeIgnore;

public class FacadeAnnotationIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return hasFacadeIgnoreMarker(m) || super.hasIgnoreMarker(m);
    }

    private boolean hasFacadeIgnoreMarker(AnnotatedMember m) {
        FacadeIgnore ann = m.getAnnotation(FacadeIgnore.class);
        return ann != null && ann.value();
    }
}
