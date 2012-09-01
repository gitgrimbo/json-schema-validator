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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        Set<String> expectedMissing = new HashSet<String>(Arrays.asList("header", "body"));
        assertPropertiesMissing(r, "", expectedMissing);
    }

    @Test
    public void testSchemaWithRelativeRefsLoadedFromFile() throws Exception {
        ValidationReport r = doValidate("/grimbo/child1/child.json", "/grimbo/test-object.json");
        printReport(r);
        assertTrue(r.isSuccess());
    }

    @Test
    public void testSchemaWithRelativeRefsLoadedFromJar() throws Exception {
        // create the jar full of schemas in target
        File target = new File(".", "target");
        File jarFile = new File(target, "resources.jar").getAbsoluteFile().getCanonicalFile();
        createZip(jarFile, new File("src/test/resources"));

        // create a classloader for the jar
        URLClassLoader u = new URLClassLoader(new URL[] { jarFile.toURI().toURL() }, null);

        // get the schema url from the jar
        URL schemaURL = u.getResource("grimbo/child1/child.json");

        ValidationReport r = doValidate(schemaURL, getClass().getResource("/grimbo/test-object.json"));
        printReport(r);
        assertTrue(r.isSuccess());
    }

    @Test
    public void testTestObjectNoBodyItem() throws Exception {
        ValidationReport r = doValidate("/grimbo/child1/child.json", "/grimbo/test-object-no-bodyItem.json");
        printReport(r);
        Set<String> expectedMissing = new HashSet<String>(Arrays.asList("bodyItem"));
        assertPropertiesMissing(r, "/body", expectedMissing);
    }

    @Test
    public void testAbsoluteUriForParent() throws Exception {
        // Test that a schema can refer to an absolute schema.
        // And that any relative refs from the absolute schema, are interpreted
        // relative to that schema.
        ValidationReport r = doValidate("/grimbo/fake/absolute/location/child.json", "/grimbo/test-object.json");
        printReport(r);
        assertTrue(r.isSuccess());
    }

    private void assertPropertiesMissing(ValidationReport report, String path, Set<String> missing) {
        JsonNode errors = report.asJsonNode().get(path);
        assertEquals(errors.size(), missing.size(), "Number of errors same");
        for (int i = errors.size() - 1; i >= 0; i--) {
            JsonNode error = errors.get(i);
            String prop = error.get("missing").get(0).asText();
            assertTrue(missing.remove(prop));
        }
        assertTrue(missing.isEmpty());
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

    /**
     * Adds all the contents of "folder" to "zipName", relative to "folder".
     * 
     * @param zipName
     * @param folder
     * 
     * @throws IOException
     */
    private void createZip(String zipName, String folder) throws IOException {
        createZip(new File(zipName), new File(folder));
    }

    /**
     * Adds all the contents of "folder" to "zipFile", relative to "folder".
     * 
     * @param zipFile
     * @param folder
     * 
     * @throws IOException
     */
    private void createZip(File zipFile, File folder) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(zipFile);
            ZipOutputStream out = new ZipOutputStream(fos);

            addFolderToZip(folder, out);

            out.close();
        } finally {
            close(fos);
        }
    }

    private static void addFolderToZip(File folder, ZipOutputStream zip) throws IOException {
        addFolderToZip(folder, zip, folder);
    }

    /**
     * Adds all the contents of "folder" to "zip", relative to "baseFolder".
     * 
     * @param folder
     * @param zip
     * @param baseFolder
     * 
     * @throws IOException
     */
    private static void addFolderToZip(File folder, ZipOutputStream zip, File baseFolder) throws IOException {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                addFolderToZip(file, zip, baseFolder);
            } else {
                String baseName = baseFolder.getAbsolutePath();
                String name = file.getAbsolutePath().substring(baseName.length());
                if (name.startsWith(File.separator)) {
                    name = name.substring(File.separator.length());
                }
                name = name.replace(File.separator, "/");
                ZipEntry zipEntry = new ZipEntry(name);
                zip.putNextEntry(zipEntry);
                copy(new FileInputStream(file), zip);
                zip.closeEntry();
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024 * 8];
        int read = -1;
        while ((read = in.read(buf)) > -1) {
            out.write(buf, 0, read);
        }
    }

    private void close(OutputStream out) {
        if (null == out) {
            return;
        }
        try {
            out.close();
        } catch (IOException e) {
            // Basically ignore
            e.printStackTrace();
        }
    }
}
