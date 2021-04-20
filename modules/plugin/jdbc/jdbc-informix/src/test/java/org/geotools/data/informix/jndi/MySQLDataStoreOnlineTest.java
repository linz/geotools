package org.geotools.data.informix.jndi;

import org.geotools.data.informix.MySQLTestSetup;
import org.geotools.jdbc.JDBCDataStoreOnlineTest;
import org.geotools.jdbc.JDBCJNDITestSetup;
import org.geotools.jdbc.JDBCTestSetup;

public class MySQLDataStoreOnlineTest extends JDBCDataStoreOnlineTest {

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new JDBCJNDITestSetup(new MySQLTestSetup());
    }

    @Override
    public void testCreateSchemaWithConstraints() {
        // Informix does not complain if an attempt is made to insert content longer than the maximum length
        // The test code in the superclass could be improved to handle this though
    }
}
