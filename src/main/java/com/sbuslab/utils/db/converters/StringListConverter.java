package com.sbuslab.utils.db.converters;

import javax.persistence.Converter;
import java.util.List;


@Converter(autoApply = true)
public class StringListConverter extends AbstractJsonConverter<List<String>> {}
