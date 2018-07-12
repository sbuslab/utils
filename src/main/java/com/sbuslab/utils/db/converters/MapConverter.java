package com.sbuslab.utils.db.converters;

import javax.persistence.Converter;
import java.util.Map;

@Converter(autoApply = true)
public class MapConverter extends AbstractJsonConverter<Map<String, Object>> { }
