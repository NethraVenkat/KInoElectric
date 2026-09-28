package com.backend.javaleap.entity.enums;

/**
 * Actors of the use case.
 * <ul>
 *   <li>ADMIN - Transit Energy Director: conversion rates, energy storage, reviews accounts</li>
 *   <li>ACCOUNTANT - Invoicing User: bills power grids, processes tile repair POs, payments</li>
 *   <li>SYSTEM - telemetry feed: tracks steps/kWh per sector, routes energy, updates ledgers</li>
 * </ul>
 */
public enum Role {
    ADMIN, ACCOUNTANT, SYSTEM
}
