/*
 * Copyright 2004-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.converters.marshaller.json;

import grails.converters.JSON;

import java.util.Collection;

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.web.json.JSONWriter;

/**
 * @author Siegfried Puchbauer
 * @since 1.1
 */
@SuppressWarnings("unchecked")
public class CollectionMarshaller implements ObjectMarshaller<JSON> {

    public boolean supports(Object object) {
        return object instanceof Collection;
    }

    public void marshalObject(Object o, JSON converter) throws ConverterException {
        JSONWriter writer = converter.getWriter();
        writer.array();
        for (Object o1 : ((Collection) o)) {
            converter.convertAnother(o1);
        }
        writer.endArray();
    }
}
