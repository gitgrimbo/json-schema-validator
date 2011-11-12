/*
 * Copyright (c) 2011, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eel.kitchen.jsonschema.mechanics;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.eel.kitchen.jsonschema.JsonValidator;
import org.eel.kitchen.jsonschema.ValidationReport;
import org.eel.kitchen.jsonschema.uri.HTTPURIHandler;
import org.eel.kitchen.jsonschema.uri.URIHandler;
import org.eel.kitchen.util.JsonLoader;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;

import static org.testng.Assert.*;

public final class URIHandlerTest
{
    private static final JsonNode schema
        = JsonNodeFactory.instance.objectNode();

    private static final JsonValidator validator = new JsonValidator(schema);

    private static final URIHandler handler = new HTTPURIHandler();

    @Test(
        expectedExceptions = IllegalArgumentException.class,
        expectedExceptionsMessageRegExp = "^scheme http already registered$"
    )
    public void testRegisteringExistingSchemeFails()
    {
        validator.registerURIHandler("http", handler);
    }

    @Test(
        expectedExceptions = IllegalArgumentException.class,
        expectedExceptionsMessageRegExp = "^scheme is null"
    )
    public void testNullSchemeFails()
    {
        validator.registerURIHandler(null, handler);
    }

    @Test(
        expectedExceptions = IllegalArgumentException.class,
        expectedExceptionsMessageRegExp = "^handler is null"
    )
    public void testNullHandlerFails()
    {
        validator.registerURIHandler("myscheme", null);
    }

    @Test(
        expectedExceptions = IllegalArgumentException.class,
        expectedExceptionsMessageRegExp = "^invalid scheme \\+23$"
    )
    public void testInvalidSchemeFails()
    {
        validator.registerURIHandler("+23", handler);
    }

    @Test
    public void testUnregisteringAndRegistering()
    {
        validator.unregisterURIHandler("http");
        validator.registerURIHandler("http", handler);
    }

    @Test
    public void aspirinTime()
        throws IOException
    {
        final JsonNode testNode
            = JsonLoader.fromResource("/ref/torture.json").get("aspirin");

        final URIHandler handler = new URIHandler()
        {
            @Override
            public JsonNode getDocument(final URI uri)
                throws IOException
            {
                return testNode;
            }
        };

        final JsonValidator validator
            = new JsonValidator(testNode);

        validator.registerURIHandler("mystuff", handler);

        final ValidationReport report = validator.validate("#/link1",
            JsonNodeFactory.instance.nullNode());

        assertFalse(report.isSuccess());
        assertTrue(report.isError());

        assertEquals(report.getMessages().size(), 1);
        assertEquals(report.getMessages().get(0), "#: FATAL: schema "
            + "{\"$ref\":\"mystuff:a#/link2\"} loops on itself");
    }
}