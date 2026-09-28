package com.backend.javaleap.service;

import com.backend.javaleap.entity.AnalyticAccount;
import com.backend.javaleap.entity.Product;
import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.model.request.OrderLineRequest;
import com.backend.javaleap.repository.AnalyticAccountRepository;
import com.backend.javaleap.repository.ProductRepository;
import com.backend.javaleap.repository.TileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Validates order lines against the product master and computes their amounts. */
@Component
@RequiredArgsConstructor
public class OrderLineResolver {

    private final ProductRepository productRepository;
    private final AnalyticAccountRepository analyticAccountRepository;
    private final TileRepository tileRepository;
    private final TaxCalculator taxCalculator;

    public record ResolvedLine(Product product, String description, Tile tile, AnalyticAccount analytic,
                               BigDecimal quantity, BigDecimal unitPrice, BigDecimal gstRate,
                               BigDecimal subtotal, BigDecimal tax, BigDecimal total) { }

    public List<ResolvedLine> resolve(List<OrderLineRequest> lines, boolean purchase, AnalyticAccount defaultAnalytic) {
        List<ResolvedLine> result = new ArrayList<>();
        int n = 1;
        for (OrderLineRequest l : lines) {
            try {
                result.add(resolveOne(l, purchase, defaultAnalytic));
            } catch (BadRequestException ex) {
                throw new BadRequestException("Line " + n + ": " + ex.getMessage());
            }
            n++;
        }
        return result;
    }

    private ResolvedLine resolveOne(OrderLineRequest l, boolean purchase, AnalyticAccount defaultAnalytic) {
        Product product = productRepository.findByCode(l.productCode().trim())
                .orElseThrow(() -> new BadRequestException("Unknown product " + l.productCode()));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("Product " + product.getCode() + " is inactive");
        }
        if (purchase && !Boolean.TRUE.equals(product.getCanBePurchased())) {
            throw new BadRequestException("Product " + product.getCode() + " cannot be purchased");
        }
        if (!purchase && !Boolean.TRUE.equals(product.getCanBeSold())) {
            throw new BadRequestException("Product " + product.getCode() + " cannot be sold");
        }
        Tile tile = null;
        if (l.tileCode() != null && !l.tileCode().isBlank()) {
            if (!purchase) {
                throw new BadRequestException("tileCode is only allowed on purchase orders");
            }
            tile = tileRepository.findByTileCode(l.tileCode().trim())
                    .orElseThrow(() -> new BadRequestException("Unknown tile " + l.tileCode()));
        }
        AnalyticAccount analytic = defaultAnalytic;
        if (l.analyticAccountCode() != null && !l.analyticAccountCode().isBlank()) {
            analytic = analyticAccount(l.analyticAccountCode());
        }
        BigDecimal qty = l.quantity().setScale(3, RoundingMode.HALF_UP);
        BigDecimal price = (l.unitPrice() != null ? l.unitPrice()
                : purchase ? product.getPurchasePrice() : product.getSalePrice()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal rate = (l.gstRate() != null ? l.gstRate() : product.getGstRate()).setScale(2, RoundingMode.HALF_UP);
        TaxCalculator.LineAmounts a = taxCalculator.line(qty, price, rate);
        String description = l.description() == null || l.description().isBlank()
                ? product.getName() + (tile == null ? "" : " - tile " + tile.getTileCode())
                : l.description().trim();
        return new ResolvedLine(product, description, tile, analytic, qty, price, rate, a.subtotal(), a.tax(), a.total());
    }

    public AnalyticAccount analyticAccount(String code) {
        return analyticAccountRepository.findByCode(code.trim())
                .filter(a -> Boolean.TRUE.equals(a.getActive()))
                .orElseThrow(() -> new BadRequestException("Unknown or inactive analytic account " + code));
    }
}
