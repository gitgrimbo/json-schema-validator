/*
 * Copyright (c) 2012, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grimbo;

import static org.testng.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eel.kitchen.jsonschema.main.JsonSchema;
import org.eel.kitchen.jsonschema.main.JsonSchemaFactory;
import org.eel.kitchen.jsonschema.ref.SchemaContainer;
import org.eel.kitchen.jsonschema.report.ValidationReport;
import org.eel.kitchen.jsonschema.uri.URIDownloader;
import org.eel.kitchen.jsonschema.util.JsonLoader;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class NonAbsoluteSchemaUris {
    JsonSchemaFactory factory;

    @BeforeTest
    public void beforeTest() {
        factory = new JsonSchemaFactory.Builder().registerScheme("file", new URIDownloader() {
            public InputStream fetch(URI source) throws IOException {
                return source.toURL().openStream();
            }
        }).build();
    }

    @Test
    public void testEmptyObject() throws Exception {
        ValidationReport r = doValidate("/grimbo/child1/child.json", "/grimbo/empty-object.json");
        printReport(r);
        JsonNode errors = r.asJsonNode().get("");
        assertEquals(errors.size(), 2);
        Set<String> expected = new HashSet<String>(Arrays.asList("header", "body"));
        for (int i = errors.size() - 1; i >= 0; i--) {
            JsonNode error = errors.get(i);
            String missing = error.get("missing").get(0).asText();
            assertTrue(expected.remove(missing));
        }
        assertTrue(expected.isEmpty());
    }

    @Test
    public void testTestObject() throws Exception {
        ValidationReport r = doValidate("/grimbo/child1/child.json", "/grimbo/test-object.json");
        printReport(r);
        assertTrue(r.isSuccess());
    }

    @Test
    public void testTestObjectNoBodyItem() throws Exception {
        ValidationReport r = doValidate("/grimbo/child1/child.json", "/grimbo/test-object-no-bodyItem.json");
        printReport(r);
        assertTrue(r.isSuccess());
    }

    private ValidationReport doValidate(String schemaResource, String dataResource) throws Exception {
        // will get "file://" URIs for the schema and the data file.
        return doValidate(getClass().getResource(schemaResource), getClass().getResource(dataResource));
    }

    private ValidationReport doValidate(URL schemaResource, URL dataResource) throws Exception {
        JsonNode data = JsonLoader.fromURL(dataResource);

        final SchemaContainer container = factory.getSchema(schemaResource.toURI());
        final JsonSchema schema = factory.createSchema(container);

        return schema.validate(data);
    }

    private void printReport(ValidationReport r) throws Exception {
        System.out.println("\n----------\n");
        new ObjectMapper().writer(new DefaultPrettyPrinter()).writeValue(System.out, r.asJsonNode());
        System.out.println("\n----------\n");
    }
}
