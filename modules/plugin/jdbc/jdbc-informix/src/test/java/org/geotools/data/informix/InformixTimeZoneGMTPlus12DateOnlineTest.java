package org.geotools.data.informix;

import java.util.TimeZone;
import org.geotools.jdbc.JDBCDateTestSetup;
import org.geotools.jdbc.JDBCTimeZoneDateOnlineTest;

public class InformixTimeZoneGMTPlus12DateOnlineTest extends JDBCTimeZoneDateOnlineTest {

    @Override
    protected JDBCDateTestSetup createTestSetup() {
        super.setTimeZone(TimeZone.getTimeZone("Etc/GMT+12"));
        return new InformixDateTestSetup();
    }
}
