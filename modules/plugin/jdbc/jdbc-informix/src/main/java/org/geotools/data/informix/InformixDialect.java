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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Level;
import org.geotools.geometry.jts.Geometries;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.SQLDialect;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Delegate for {@link InformixDialectBasic} and {@link InformixDialectPrepared} which implements
 * the common part of the api.
 *
 * @author Justin Deoliveira, OpenGEO
 */
public class InformixDialect extends SQLDialect {
    /** mysql spatial types */
    protected Integer POINT = Integer.valueOf(2001);

    protected Integer LINESTRING = Integer.valueOf(2002);
    protected Integer POLYGON = Integer.valueOf(2003);
    protected Integer MULTIPOINT = Integer.valueOf(2004);
    protected Integer MULTILINESTRING = Integer.valueOf(2005);
    protected Integer MULTIPOLYGON = Integer.valueOf(2006);
    protected Integer GEOMETRY = Integer.valueOf(2007);

    public InformixDialect(JDBCDataStore dataStore) {
        super(dataStore);
    }

    @Override
    public boolean includeTable(String schemaName, String tableName, Connection cx)
            throws SQLException {
        if ("geometry_columns".equalsIgnoreCase(tableName)) {
            return false;
        }
        return super.includeTable(schemaName, tableName, cx);
    }

    public String getNameEscape() {
        return "";
    }

    public String getGeometryTypeName(Integer type) {
        if (POINT.equals(type)) {
            return "ST_POINT";
        }

        if (MULTIPOINT.equals(type)) {
            return "ST_MULTIPOINT";
        }

        if (LINESTRING.equals(type)) {
            return "ST_LINESTRING";
        }

        if (MULTILINESTRING.equals(type)) {
            return "ST_MULTILINESTRING";
        }

        if (POLYGON.equals(type)) {
            return "ST_POLYGON";
        }

        if (MULTIPOLYGON.equals(type)) {
            return "ST_MULTIPOLYGON";
        }

        if (GEOMETRY.equals(type)) {
            return "ST_GEOMETRY";
        }

        return super.getGeometryTypeName(type);
    }

    public Integer getGeometrySRID(
            String schemaName, String tableName, String columnName, Connection cx)
            throws SQLException {

        // first check the geometry_columns table
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT ");
        encodeColumnName(null, "srid", sql);
        sql.append(" FROM ");
        encodeTableName("geometry_columns", sql);
        sql.append(" WHERE ");

        encodeColumnName(null, "f_table_schema", sql);

        if (schemaName != null) {
            sql.append(" = '").append(schemaName).append("'");
        } else {
            sql.append(" IS NULL");
        }
        sql.append(" AND ");

        encodeColumnName(null, "f_table_name", sql);
        sql.append(" = '").append(tableName).append("' AND ");

        encodeColumnName(null, "f_geometry_column", sql);
        sql.append(" = '").append(columnName).append("'");

        dataStore.getLogger().fine(sql.toString());

        Statement st = cx.createStatement();
        try {
            ResultSet rs = st.executeQuery(sql.toString());
            try {
                if (rs.next()) {
                    return Integer.valueOf(rs.getInt(1));
                }
            } finally {
                dataStore.closeSafe(rs);
            }
        } catch (SQLException e) {
            // geometry_columns does not exist
        } finally {
            dataStore.closeSafe(st);
        }

        // execute SELECT srid(<columnName>) FROM <tableName> LIMIT 1;
        sql = new StringBuffer();
        sql.append("SELECT st_srid(");
        encodeColumnName(null, columnName, sql);
        sql.append(") ");
        sql.append("FROM ");

        if (schemaName != null) {
            encodeTableName(schemaName, sql);
            sql.append(".");
        }

        encodeSchemaName(tableName, sql);
        sql.append(" WHERE ");
        encodeColumnName(null, columnName, sql);
        sql.append(" is not null LIMIT 1");

        dataStore.getLogger().fine(sql.toString());

        st = cx.createStatement();
        try {
            ResultSet rs = st.executeQuery(sql.toString());

            try {
                if (rs.next()) {
                    return Integer.valueOf(rs.getInt(1));
                } else {
                    // could not find out
                    return null;
                }
            } finally {
                dataStore.closeSafe(rs);
            }
        } finally {
            dataStore.closeSafe(st);
        }
    }

    @Override
    public void encodeGeometryColumn(
            GeometryDescriptor gatt, String prefix, int srid, Hints hints, StringBuffer sql) {
        sql.append("st_asbinary(");
        encodeColumnName(prefix, gatt.getLocalName(), sql);
        sql.append(")");
    }

    public void encodeGeometryEnvelope(String tableName, String geometryColumn, StringBuffer sql) {
        sql.append("st_asbinary(");
        sql.append("st_envelope(");
        encodeColumnName(null, geometryColumn, sql);
        sql.append("))");
    }

    public Envelope decodeGeometryEnvelope(ResultSet rs, int column, Connection cx)
            throws SQLException, IOException {
        // String wkb = rs.getString( column );
        byte[] wkb = rs.getBytes(column);

        try {
            // TODO: srid
            Polygon polygon = (Polygon) new WKBReader().read(wkb);

            return polygon.getEnvelopeInternal();
        } catch (ParseException e) {
            String msg = "Error decoding wkb for envelope";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    public Geometry decodeGeometryValue(
            GeometryDescriptor descriptor,
            ResultSet rs,
            String name,
            GeometryFactory factory,
            Connection cx,
            Hints hints)
            throws IOException, SQLException {
        byte[] bytes = rs.getBytes(name);
        if (bytes == null) {
            return null;
        }
        try {
            return new WKBReader(factory).read(bytes);
        } catch (ParseException e) {
            String msg = "Error decoding wkb";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    public void registerClassToSqlMappings(Map<Class<?>, Integer> mappings) {
        super.registerClassToSqlMappings(mappings);

        mappings.put(Point.class, POINT);
        mappings.put(LineString.class, LINESTRING);
        mappings.put(Polygon.class, POLYGON);
        mappings.put(MultiPoint.class, MULTIPOINT);
        mappings.put(MultiLineString.class, MULTILINESTRING);
        mappings.put(MultiPolygon.class, MULTIPOLYGON);
        mappings.put(Geometry.class, GEOMETRY);
    }

    //	public void registerSqlTypeToClassMappings(Map<Integer, Class<?>> mappings) {
    //		super.registerSqlTypeToClassMappings(mappings);
    //
    //		mappings.put(POINT, Point.class);
    //		mappings.put(LINESTRING, LineString.class);
    //		mappings.put(POLYGON, Polygon.class);
    //		mappings.put(MULTIPOINT, MultiPoint.class);
    //		mappings.put(MULTILINESTRING, MultiLineString.class);
    //		mappings.put(MULTIPOLYGON, MultiPolygon.class);
    //		mappings.put(GEOMETRY, Geometry.class);
    //	}

    public void registerSqlTypeNameToClassMappings(Map<String, Class<?>> mappings) {
        super.registerSqlTypeNameToClassMappings(mappings);

        mappings.put("st_point", Point.class);
        mappings.put("st_linestring", LineString.class);
        mappings.put("st_polygon", Polygon.class);
        mappings.put("st_multipoint", MultiPoint.class);
        mappings.put("st_multilinestring", MultiLineString.class);
        mappings.put("st_multipolygon", MultiPolygon.class);
        mappings.put("st_geometry", Geometry.class);
        mappings.put("st_geometrycollection", GeometryCollection.class);
    }

    @Override
    public void registerSqlTypeToSqlTypeNameOverrides(Map<Integer, String> overrides) {
        // overrides.put(Types.BOOLEAN, "BOOL");
    }

    public void encodePostCreateTable(String tableName, StringBuffer sql) {}

    @Override
    public void encodePostColumnCreateTable(AttributeDescriptor att, StringBuffer sql) {
        // make geometry columns non null in order to be able to index them
        if (att instanceof GeometryDescriptor && !att.isNillable()) {
            if (!sql.toString().trim().endsWith(" NOT NULL")) {
                sql.append(" NOT NULL");
            }
        }
    }

    @Override
    public void postCreateTable(String schemaName, SimpleFeatureType featureType, Connection cx)
            throws SQLException, IOException {

        // create spatial index for all geometry columns
        for (AttributeDescriptor ad : featureType.getAttributeDescriptors()) {
            if (!(ad instanceof GeometryDescriptor)) {
                continue;
            }
            GeometryDescriptor gd = (GeometryDescriptor) ad;

            if (!ad.isNillable()) {
                // can only index non null columns
                StringBuffer sql = new StringBuffer("ALTER TABLE ");
                encodeTableName(featureType.getTypeName(), sql);
                sql.append(" ADD SPATIAL INDEX (");
                encodeColumnName(null, gd.getLocalName(), sql);
                sql.append(")");

                LOGGER.fine(sql.toString());
                Statement st = cx.createStatement();
                try {
                    st.execute(sql.toString());
                } finally {
                    dataStore.closeSafe(st);
                }
            }

        }
    }

    public void encodePrimaryKey(String column, StringBuffer sql) {
        encodeColumnName(null, column, sql);
        sql.append(" SERIAL PRIMARY KEY");
    }

    @Override
    public boolean lookupGeneratedValuesPostInsert() {
        return true;
    }

    @Override
    public Object getLastAutoGeneratedValue(
            String schemaName, String tableName, String columnName, Connection cx)
            throws SQLException {
        Statement st = cx.createStatement();
        try {
            String sql = "SELECT DBINFO( 'sqlca.sqlerrd1' ) FROM systables LIMIT 1";
            dataStore.getLogger().fine(sql);

            ResultSet rs = st.executeQuery(sql);
            try {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            } finally {
                dataStore.closeSafe(rs);
            }
        } finally {
            dataStore.closeSafe(st);
        }

        return null;
    }

    @Override
    public boolean isLimitOffsetSupported() {
        return true;
    }

    @Override
    public void applyLimitOffset(StringBuffer sql, int limit, int offset) {
        // Note: The syntax of LIMIT and SKIP in Informix is stricter inside a subquery than in other contexts. SKIP
        // must precede FIRST (no LIMIT) and these must be immediately after SELECT.
        if (!sql.toString().startsWith("SELECT")) {
            throw new IllegalArgumentException("Cannot apply limit to a query that does not begin with SELECT");
        }

        String limitSql = null;
        if (limit >= 0 && limit < Integer.MAX_VALUE) {
            if (offset > 0) limitSql = " SKIP " + offset + " FIRST " + limit;
            else limitSql = " FIRST " + limit;
        } else if (offset > 0) {
            limitSql = " SKIP " + offset;
        }

        sql.insert("SELECT".length(), limitSql);
    }

}
