package com.backend.javaleap.model;

/** How tile technologies are laid out when a new sector is created. */
public enum TilePattern {
    /** Every 3rd tile on a diagonal is electromagnetic, the rest piezo. */
    MIXED,
    ALL_PIEZO,
    ALL_ELECTROMAGNETIC
}
