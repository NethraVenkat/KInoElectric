package com.backend.javaleap.service;

import com.backend.javaleap.entity.AnalyticAccount;
import com.backend.javaleap.entity.Contact;
import com.backend.javaleap.entity.Invoice;
import com.backend.javaleap.entity.PurchaseOrder;
import com.backend.javaleap.entity.PurchaseOrderLine;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.enums.InvoiceStatus;
import com.backend.javaleap.entity.enums.OrderStatus;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.CreateInvoiceRequest;
import com.backend.javaleap.model.request.PurchaseOrderRequest;
import com.backend.javaleap.model.response.InvoiceResponse;
import com.backend.javaleap.model.response.PurchaseOrderResponse;
import com.backend.javaleap.repository.InvoiceRepository;
import com.backend.javaleap.repository.PurchaseOrderRepository;
import com.backend.javaleap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Purchase flow: draft PO -> confirm (PO number) -> vendor bill (draft invoice). */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ContactService contactService;
    private final OrderLineResolver lineResolver;
    private final DocumentSequenceService sequenceService;
    private final InvoiceService invoiceService;
    private final TransactionMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> search(OrderStatus status, Long vendorId, String q, Pageable pageable) {
        return PageResponse.from(orderRepository.search(status, vendorId, q == null || q.isBlank() ? null : q.trim(),
                pageable), p -> mapper.toResponse(p, null, false));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse get(Long id) {
        return response(find(id));
    }

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest req) {
        PurchaseOrder po = PurchaseOrder.builder().status(OrderStatus.DRAFT).createdBy(currentUser()).build();
        apply(po, req);
        PurchaseOrder saved = orderRepository.save(po);
        log.info("Draft PO {} for {} ({} INR)", saved.getId(), saved.getVendor().getName(), saved.getTotalAmount());
        return response(saved);
    }

    @Transactional
    public PurchaseOrderResponse update(Long id, PurchaseOrderRequest req) {
        PurchaseOrder po = find(id);
        requireStatus(po, OrderStatus.DRAFT);
        apply(po, req);
        return response(po);
    }

    @Transactional
    public PurchaseOrderResponse confirm(Long id) {
        PurchaseOrder po = find(id);
        requireStatus(po, OrderStatus.DRAFT);
        if (po.getLines().isEmpty()) {
            throw new BadRequestException("A purchase order needs at least one line");
        }
        po.setPoNumber(sequenceService.next(DocumentSequenceService.PURCHASE_ORDER, po.getOrderDate()));
        po.setStatus(OrderStatus.CONFIRMED);
        po.setConfirmedAt(LocalDateTime.now());
        po.setConfirmedBy(currentUser());
        log.info("PO {} confirmed", po.getPoNumber());
        return response(po);
    }

    @Transactional
    public PurchaseOrderResponse cancel(Long id) {
        PurchaseOrder po = find(id);
        if (po.getStatus() == OrderStatus.INVOICED) {
            throw new BadRequestException("PO is billed - cancel the vendor bill first");
        }
        if (po.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("PO is already cancelled");
        }
        po.setStatus(OrderStatus.CANCELLED);
        return response(po);
    }

    /** Creates the draft vendor bill for a confirmed PO. */
    @Transactional
    public InvoiceResponse createBill(Long id, CreateInvoiceRequest req) {
        PurchaseOrder po = find(id);
        requireStatus(po, OrderStatus.CONFIRMED);
        Invoice bill = invoiceService.createFromPurchaseOrder(po, req);
        po.setStatus(OrderStatus.INVOICED);
        return invoiceService.get(bill.getId());
    }

    private void apply(PurchaseOrder po, PurchaseOrderRequest req) {
        Contact vendor = contactService.requireActive(req.vendorId());
        if (!vendor.isVendor()) {
            throw new BadRequestException("Contact " + vendor.getCode() + " is not a vendor");
        }
        if (req.expectedDate() != null && req.expectedDate().isBefore(req.orderDate())) {
            throw new BadRequestException("expectedDate cannot be before orderDate");
        }
        AnalyticAccount analytic = req.analyticAccountCode() == null || req.analyticAccountCode().isBlank()
                ? null : lineResolver.analyticAccount(req.analyticAccountCode());
        List<PurchaseOrderLine> lines = lineResolver.resolve(req.lines(), true, analytic).stream()
                .map(r -> PurchaseOrderLine.builder().product(r.product()).description(r.description())
                        .tile(r.tile()).analyticAccount(r.analytic()).quantity(r.quantity()).unitPrice(r.unitPrice())
                        .gstRate(r.gstRate()).subtotal(r.subtotal()).taxAmount(r.tax()).total(r.total()).build())
                .toList();
        po.setVendor(vendor);
        po.setOrderDate(req.orderDate());
        po.setExpectedDate(req.expectedDate());
        po.setAnalyticAccount(analytic);
        po.setReference(req.reference() == null ? null : req.reference().trim());
        po.setNotes(req.notes());
        po.replaceLines(lines);
    }

    private PurchaseOrderResponse response(PurchaseOrder po) {
        Invoice bill = po.getId() == null ? null
                : invoiceRepository.findFirstByPurchaseOrderIdAndStatusNotOrderByIdDesc(po.getId(), InvoiceStatus.CANCELLED)
                .orElse(null);
        return mapper.toResponse(po, bill, true);
    }

    private static void requireStatus(PurchaseOrder po, OrderStatus expected) {
        if (po.getStatus() != expected) {
            throw new BadRequestException("Purchase order is " + po.getStatus() + " - expected " + expected);
        }
    }

    private PurchaseOrder find(Long id) {
        return orderRepository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Purchase order", id));
    }

    private User currentUser() {
        return SecurityUtils.currentUserId().flatMap(userRepository::findById).orElse(null);
    }
}
