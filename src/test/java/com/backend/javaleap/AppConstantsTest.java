package com.backend.javaleap;

import com.backend.javaleap.model.AppConstants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConstantsTest {

	@Test
	void convertsJoulesToKwhAndWh() {
		assertEquals(1.0, AppConstants.joulesToKwh(3_600_000), 1e-9);
		assertEquals(1.0, AppConstants.joulesToWh(3_600), 1e-9);
	}
}
