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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.jdbc.BasicSQLDialect;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

/**
 * MySQL database dialect based on basic (non-prepared) statements.
 *
 * @author Justin Deoliveira, OpenGEO
 * @author Nikolaos Pringouris <nprigour@gmail.com> added support for MySQL versions 5.6 (and above)
 */
public class InformixDialectBasic extends BasicSQLDialect {

    InformixDialect delegate;

    public InformixDialectBasic(JDBCDataStore dataStore) {
        this(dataStore, false);
    }

    public InformixDialectBasic(JDBCDataStore dataStore, boolean usePreciseSpatialOps) {
        super(dataStore);
        delegate = new InformixDialect(dataStore);
    }

    @Override
    public boolean includeTable(String schemaName, String tableName, Connection cx)
            throws SQLException {
        return delegate.includeTable(schemaName, tableName, cx);
    }

    @Override
    public String getNameEscape() {
        return delegate.getNameEscape();
    }

    @Override
    public String getGeometryTypeName(Integer type) {
        return delegate.getGeometryTypeName(type);
    }

    @Override
    public Integer getGeometrySRID(
            String schemaName, String tableName, String columnName, Connection cx)
            throws SQLException {
        return delegate.getGeometrySRID(schemaName, tableName, columnName, cx);
    }

    @Override
    public void encodeColumnName(String prefix, String raw, StringBuffer sql) {
        delegate.encodeColumnName(prefix, raw, sql);
    }

    public void encodeGeometryColumn(
            GeometryDescriptor gatt, String prefix, int srid, StringBuffer sql) {
        delegate.encodeColumnName(prefix, gatt.getLocalName(), sql);
    }

    @Override
    public void encodeGeometryColumn(
            GeometryDescriptor gatt, String prefix, int srid, Hints hints, StringBuffer sql) {
        delegate.encodeGeometryColumn(gatt, prefix, srid, hints, sql);
    }

    @Override
    public void encodeColumnType(String sqlTypeName, StringBuffer sql) {
        delegate.encodeColumnType(sqlTypeName, sql);
    }

    @Override
    public void registerClassToSqlMappings(Map<Class<?>, Integer> mappings) {
        delegate.registerClassToSqlMappings(mappings);
    }

    @Override
    public void registerSqlTypeToClassMappings(Map<Integer, Class<?>> mappings) {
        delegate.registerSqlTypeToClassMappings(mappings);
    }

    @Override
    public void registerSqlTypeNameToClassMappings(Map<String, Class<?>> mappings) {
        delegate.registerSqlTypeNameToClassMappings(mappings);
    }

    @Override
    public void registerSqlTypeToSqlTypeNameOverrides(Map<Integer, String> overrides) {
        delegate.registerSqlTypeToSqlTypeNameOverrides(overrides);
    }

    @Override
    public void encodePostCreateTable(String tableName, StringBuffer sql) {
        delegate.encodePostCreateTable(tableName, sql);
    }

    @Override
    public void encodePostColumnCreateTable(AttributeDescriptor att, StringBuffer sql) {
        delegate.encodePostColumnCreateTable(att, sql);
    }

    @Override
    public void postCreateTable(String schemaName, SimpleFeatureType featureType, Connection cx)
            throws SQLException, IOException {
        delegate.postCreateTable(schemaName, featureType, cx);
    }

    @Override
    public void encodePrimaryKey(String column, StringBuffer sql) {
        delegate.encodePrimaryKey(column, sql);
    }

    @Override
    public boolean lookupGeneratedValuesPostInsert() {
        return delegate.lookupGeneratedValuesPostInsert();
    }

    @Override
    public Object getNextAutoGeneratedValue(
            String schemaName, String tableName, String columnName, Connection cx)
            throws SQLException {
        return delegate.getNextAutoGeneratedValue(schemaName, tableName, columnName, cx);
    }

    @Override
    public Object getLastAutoGeneratedValue(
            String schemaName, String tableName, String columnName, Connection cx)
            throws SQLException {
        return delegate.getLastAutoGeneratedValue(schemaName, tableName, columnName, cx);
    }

    @Override
    public void encodeGeometryValue(Geometry value, int dimension, int srid, StringBuffer sql)
            throws IOException {
        if (value != null) {
            String fromText = fromTextFunctionName(value);
            sql.append(fromText + "('");
            sql.append(new WKTWriter().write(value));
            sql.append("', ").append(srid).append(")");
        } else {
            sql.append("NULL");
        }
    }

    // https://www.cursor-distribution.de/aktuell.12.10.xC6/documentation/ids_spat_bookmap.pdf
    private String fromTextFunctionName(Geometry value) {
        if (value instanceof Point) {
            return "ST_PointFromText";
        } else if (value instanceof LineString) {
            return "ST_LineFromText";
        } else if (value instanceof Polygon) {
            return "ST_PolyFromText";
        } else if (value instanceof MultiPoint) {
            return "ST_MPointFromText";
        } else if (value instanceof MultiLineString) {
            return "ST_MLineFromText";
        } else if (value instanceof MultiPolygon) {
            return "ST_MPolyFromText";
        }
        return "ST_GeomFromText";
    }

    @Override
    public Geometry decodeGeometryValue(
            GeometryDescriptor descriptor,
            ResultSet rs,
            String column,
            GeometryFactory factory,
            Connection cx,
            Hints hints)
            throws IOException, SQLException {
        byte[] bytes = rs.getBytes(column);
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

    @Override
    public void encodeGeometryEnvelope(String tableName, String geometryColumn, StringBuffer sql) {
        sql.append("ST_AsBinary(");
        sql.append("ST_Envelope(");

        encodeColumnName(null, geometryColumn, sql);
        sql.append("))");
    }

    @Override
    public Envelope decodeGeometryEnvelope(ResultSet rs, int column, Connection cx)
            throws SQLException, IOException {
        byte[] wkb = rs.getBytes(column);

        try {
            /**
             * As of MySQL 5.7.6, if the argument is a point or a vertical or horizontal line
             * segment, ST_Envelope() returns the point or the line segment as its MBR rather than
             * returning an invalid polygon therefore we must override behavior and check for a
             * geometry and not a polygon
             */
            // TODO: srid
            Geometry geom = (Geometry) new WKBReader().read(wkb);

            return geom.getEnvelopeInternal();
        } catch (ParseException e) {
            String msg = "Error decoding wkb for envelope";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    @Override
    public boolean isLimitOffsetSupported() {
        return delegate.isLimitOffsetSupported();
    }

    @Override
    public void applyLimitOffset(StringBuffer sql, int limit, int offset) {
        delegate.applyLimitOffset(sql, limit, offset);
    }

    @Override
    public FilterToSQL createFilterToSQL() {
        return new InformixFilterToSQL();
    }
}
