package com.backend.javaleap.service;

import com.backend.javaleap.entity.Contact;
import com.backend.javaleap.entity.Invoice;
import com.backend.javaleap.entity.Product;
import com.backend.javaleap.entity.SalesOrder;
import com.backend.javaleap.entity.SalesOrderLine;
import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.enums.InvoiceStatus;
import com.backend.javaleap.entity.enums.OrderStatus;
import com.backend.javaleap.entity.enums.UnitOfMeasure;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.EnergyUnits;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.CreateInvoiceRequest;
import com.backend.javaleap.model.request.EnergySalesOrderRequest;
import com.backend.javaleap.model.request.SalesOrderRequest;
import com.backend.javaleap.model.response.InvoiceResponse;
import com.backend.javaleap.model.response.SalesOrderResponse;
import com.backend.javaleap.repository.AnalyticAccountRepository;
import com.backend.javaleap.repository.EnergyGenerationDailyRepository;
import com.backend.javaleap.repository.InvoiceRepository;
import com.backend.javaleap.repository.ProductRepository;
import com.backend.javaleap.repository.SalesOrderRepository;
import com.backend.javaleap.repository.UserRepository;
import com.backend.javaleap.repository.projection.ZoneExportView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Sales flow: energy export (or manual) sales order -> confirm (SO number) -> customer invoice.
 * The energy order converts the telemetry log (kWh exported per floor sector) into order lines.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private static final String DEFAULT_ENERGY_PRODUCT = "PRD-KWH";

    private final SalesOrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final EnergyGenerationDailyRepository generationRepository;
    private final AnalyticAccountRepository analyticAccountRepository;
    private final UserRepository userRepository;
    private final ContactService contactService;
    private final StationLookup stationLookup;
    private final OrderLineResolver lineResolver;
    private final TaxCalculator taxCalculator;
    private final DocumentSequenceService sequenceService;
    private final InvoiceService invoiceService;
    private final TransactionMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<SalesOrderResponse> search(OrderStatus status, Long customerId, String q, Pageable pageable) {
        return PageResponse.from(orderRepository.search(status, customerId, q == null || q.isBlank() ? null : q.trim(),
                pageable), s -> mapper.toResponse(s, null, false));
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse get(Long id) {
        return response(find(id));
    }

    // ------------------------------------------------------------------ manual orders

    @Transactional
    public SalesOrderResponse create(SalesOrderRequest req) {
        SalesOrder so = SalesOrder.builder().status(OrderStatus.DRAFT).createdBy(currentUser()).build();
        apply(so, req);
        return response(orderRepository.save(so));
    }

    @Transactional
    public SalesOrderResponse update(Long id, SalesOrderRequest req) {
        SalesOrder so = find(id);
        requireStatus(so, OrderStatus.DRAFT);
        if (so.isEnergyExport()) {
            throw new BadRequestException("Energy export orders are generated from telemetry - cancel and regenerate");
        }
        apply(so, req);
        return response(so);
    }

    private void apply(SalesOrder so, SalesOrderRequest req) {
        Contact customer = customer(req.customerId());
        List<SalesOrderLine> lines = lineResolver.resolve(req.lines(), false, null).stream()
                .map(r -> SalesOrderLine.builder().product(r.product()).description(r.description())
                        .analyticAccount(r.analytic()).quantity(r.quantity()).unitPrice(r.unitPrice())
                        .gstRate(r.gstRate()).subtotal(r.subtotal()).taxAmount(r.tax()).total(r.total()).build())
                .toList();
        so.setCustomer(customer);
        so.setOrderDate(req.orderDate());
        so.setReference(req.reference() == null ? null : req.reference().trim());
        so.setNotes(req.notes());
        so.replaceLines(lines);
    }

    // ------------------------------------------------------------------ energy export from telemetry

    /**
     * Builds the monthly energy sales order: for each floor sector, kWh exported to the grid in the
     * period (from energy_generation_daily) x the kWh product price, tagged with the sector's analytic account.
     */
    @Transactional
    public SalesOrderResponse createEnergyOrder(EnergySalesOrderRequest req) {
        Contact customer = customer(req.customerId());
        Station station = stationLookup.resolve(req.stationCode());
        LocalDate start = req.periodStart();
        LocalDate end = req.periodEnd();
        if (start.isAfter(end)) {
            throw new BadRequestException("periodStart must be on or before periodEnd");
        }
        if (end.isAfter(LocalDate.now())) {
            throw new BadRequestException("periodEnd cannot be in the future - energy is billed after it is exported");
        }
        if (ChronoUnit.DAYS.between(start, end) > 366) {
            throw new BadRequestException("Billing period cannot exceed one year");
        }
        if (orderRepository.existsEnergyOrderOverlapping(station.getId(), start, end)) {
            throw new ConflictException("Energy for " + station.getCode() + " in " + start + " to " + end
                    + " is already on another sales order");
        }
        String productCode = req.productCode() == null || req.productCode().isBlank()
                ? DEFAULT_ENERGY_PRODUCT : req.productCode().trim();
        Product product = productRepository.findByCode(productCode)
                .orElseThrow(() -> new BadRequestException("Unknown product " + productCode));
        if (!Boolean.TRUE.equals(product.getCanBeSold()) || product.getUom() != UnitOfMeasure.KWH) {
            throw new BadRequestException("Product " + productCode + " must be a sellable kWh product");
        }

        List<SalesOrderLine> lines = new ArrayList<>();
        for (ZoneExportView z : generationRepository.sumExportedByZone(station.getId(), start, end)) {
            BigDecimal kwh = EnergyUnits.toKwh(z.getExportedJ()).setScale(3, RoundingMode.HALF_UP);
            if (kwh.signum() <= 0) {
                continue;
            }
            TaxCalculator.LineAmounts a = taxCalculator.line(kwh, product.getSalePrice(), product.getGstRate());
            lines.add(SalesOrderLine.builder()
                    .product(product)
                    .description("Kinetic energy exported - " + z.getZoneName() + " (" + start + " to " + end + ")")
                    .analyticAccount(analyticAccountRepository.findByZoneId(z.getZoneId()).orElse(null))
                    .quantity(kwh).unitPrice(product.getSalePrice()).gstRate(product.getGstRate())
                    .subtotal(a.subtotal()).taxAmount(a.tax()).total(a.total())
                    .build());
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("No energy was exported by " + station.getCode() + " in this period");
        }

        SalesOrder so = SalesOrder.builder()
                .customer(customer)
                .orderDate(LocalDate.now())
                .station(station)
                .periodStart(start)
                .periodEnd(end)
                .status(OrderStatus.DRAFT)
                .reference("ENERGY-" + station.getCode() + "-" + start)
                .notes("Grid export of harvested kinetic energy, " + station.getName())
                .createdBy(currentUser())
                .build();
        so.replaceLines(lines);
        SalesOrder saved = orderRepository.save(so);
        log.info("Energy sales order {} for {}: {} lines, {} INR", saved.getId(), station.getCode(), lines.size(),
                saved.getTotalAmount());
        return response(saved);
    }

    // ------------------------------------------------------------------ lifecycle

    @Transactional
    public SalesOrderResponse confirm(Long id) {
        SalesOrder so = find(id);
        requireStatus(so, OrderStatus.DRAFT);
        if (so.getLines().isEmpty()) {
            throw new BadRequestException("A sales order needs at least one line");
        }
        so.setSoNumber(sequenceService.next(DocumentSequenceService.SALES_ORDER, so.getOrderDate()));
        so.setStatus(OrderStatus.CONFIRMED);
        so.setConfirmedAt(LocalDateTime.now());
        so.setConfirmedBy(currentUser());
        log.info("SO {} confirmed", so.getSoNumber());
        return response(so);
    }

    @Transactional
    public SalesOrderResponse cancel(Long id) {
        SalesOrder so = find(id);
        if (so.getStatus() == OrderStatus.INVOICED) {
            throw new BadRequestException("SO is invoiced - cancel the customer invoice first");
        }
        if (so.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("SO is already cancelled");
        }
        so.setStatus(OrderStatus.CANCELLED);
        return response(so);
    }

    /** Creates the draft customer invoice for a confirmed SO. */
    @Transactional
    public InvoiceResponse createInvoice(Long id, CreateInvoiceRequest req) {
        SalesOrder so = find(id);
        requireStatus(so, OrderStatus.CONFIRMED);
        Invoice invoice = invoiceService.createFromSalesOrder(so, req);
        so.setStatus(OrderStatus.INVOICED);
        return invoiceService.get(invoice.getId());
    }

    // ------------------------------------------------------------------ helpers

    private Contact customer(Long id) {
        Contact c = contactService.requireActive(id);
        if (!c.isCustomer()) {
            throw new BadRequestException("Contact " + c.getCode() + " is not a customer");
        }
        return c;
    }

    private SalesOrderResponse response(SalesOrder so) {
        Invoice inv = so.getId() == null ? null
                : invoiceRepository.findFirstBySalesOrderIdAndStatusNotOrderByIdDesc(so.getId(), InvoiceStatus.CANCELLED)
                .orElse(null);
        return mapper.toResponse(so, inv, true);
    }

    private static void requireStatus(SalesOrder so, OrderStatus expected) {
        if (so.getStatus() != expected) {
            throw new BadRequestException("Sales order is " + so.getStatus() + " - expected " + expected);
        }
    }

    private SalesOrder find(Long id) {
        return orderRepository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Sales order", id));
    }

    private User currentUser() {
        return SecurityUtils.currentUserId().flatMap(userRepository::findById).orElse(null);
    }
}
