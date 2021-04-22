/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.informix;

import java.util.Collections;
import java.util.Map;
import org.geotools.data.Parameter;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.SQLDialect;

/**
 * DataStoreFactory for Informix database.
 *
 * @author George Dewar, Land Information New Zealand
 * @author Ines Falcao, Land Information New Zealand
 */
public class InformixDataStoreFactory extends JDBCDataStoreFactory {
    /** parameter for database type */
    public static final Param DBTYPE =
            new Param(
                    "dbtype",
                    String.class,
                    "Type",
                    true,
                    "informix-sqli",
                    Collections.singletonMap(Parameter.LEVEL, "program"));
    /** Default port number for Informix */
    public static final Param PORT = new Param("port", Integer.class, "Port", true, 9088);

    protected SQLDialect createSQLDialect(JDBCDataStore dataStore) {
        return new InformixDialectBasic(dataStore);
    }

    public String getDisplayName() {
        return "Informix";
    }

    protected String getDriverClassName() {
        return "com.informix.jdbc.IfxDriver";
    }

    protected String getDatabaseID() {
        return (String) DBTYPE.sample;
    }

    public String getDescription() {
        return "Informix Database";
    }

    @Override
    protected String getValidationQuery() {
        return "SELECT DBINFO('version','full') FROM systables WHERE tabid = 1";
    }

    @Override
    protected void setupParameters(Map parameters) {
        super.setupParameters(parameters);
        parameters.put(DBTYPE.key, DBTYPE);
        parameters.put(PORT.key, PORT);

        parameters.remove(SCHEMA.key);
    }
}
