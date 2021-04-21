package org.geotools.data.informix;

import java.util.TimeZone;
import org.geotools.jdbc.JDBCDateTestSetup;
import org.geotools.jdbc.JDBCTimeZoneDateOnlineTest;

public class InformixTimeZoneGMTMinus14DateOnlineTest extends JDBCTimeZoneDateOnlineTest {

    @Override
    protected JDBCDateTestSetup createTestSetup() {
        super.setTimeZone(TimeZone.getTimeZone("Etc/GMT-14"));
        return new InformixDateTestSetup();
    }
}
