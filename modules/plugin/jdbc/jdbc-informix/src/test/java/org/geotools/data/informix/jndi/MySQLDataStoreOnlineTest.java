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
}
