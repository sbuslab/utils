package com.sbuslab.utils.json;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

import com.sbuslab.model.FacadeIgnore;


public class FacadeAnnotationIntrospector extends NopAnnotationIntrospector {

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        FacadeIgnore ann = m.getAnnotation(FacadeIgnore.class);
        return ann != null && ann.value();
    }
}
