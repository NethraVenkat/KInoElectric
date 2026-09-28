package com.backend.javaleap.repository.projection;

import java.math.BigDecimal;

/** Energy exported to the grid by one floor sector in a period (drives energy sales orders). */
public interface ZoneExportView {
    Long getZoneId();
    String getZoneCode();
    String getZoneName();
    BigDecimal getExportedJ();
}
