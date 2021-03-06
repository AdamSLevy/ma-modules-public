/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;

/**
 * 
 * 
 * @author Terry Packer
 */
public class SerotoninJsonValueDeserializer extends StdDeserializer<JsonValue>{

	private static final long serialVersionUID = 1L;

	protected SerotoninJsonValueDeserializer() {
		super(JsonValue.class);
	}

	@Override
	public JsonValue deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {

		//Ideally we would not read this as a a tree before converting it back to JSON
		JsonTypeReader typeReader = new JsonTypeReader(jp.readValueAsTree().toString());
		try {
			return typeReader.read();
		} catch (JsonException e) {
			throw new IOException(e);
		}
	}

}
