package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.Product;
import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.AccountType;
import com.backend.javaleap.entity.enums.ProductType;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.ProductRequest;
import com.backend.javaleap.model.response.ProductResponse;
import com.backend.javaleap.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Product master. Sellable products need an INCOME account; purchasable products need an
 * EXPENSE account (repairs) or an ASSET account (capitalised tiles).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AccountService accountService;
    private final AccountingMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductType type, Boolean sold, Boolean purchased, Boolean active,
                                                String q, Pageable pageable) {
        return PageResponse.from(productRepository.search(type, sold, purchased, active,
                q == null || q.isBlank() ? null : q.trim(), pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (productRepository.existsByCode(req.code())) {
            throw new ConflictException("Product code " + req.code() + " already exists");
        }
        Product p = new Product();
        p.setCode(req.code());
        p.setActive(true);
        apply(p, req);
        Product saved = productRepository.save(p);
        log.info("Product {} '{}' created", saved.getCode(), saved.getName());
        return mapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product p = find(id);
        if (!p.getCode().equals(req.code())) {
            if (productRepository.existsByCode(req.code())) {
                throw new ConflictException("Product code " + req.code() + " already exists");
            }
            p.setCode(req.code());
        }
        apply(p, req);
        return mapper.toResponse(p);
    }

    @Transactional
    public ProductResponse setActive(Long id, boolean active) {
        Product p = find(id);
        p.setActive(active);
        return mapper.toResponse(p);
    }

    private void apply(Product p, ProductRequest req) {
        if (!Boolean.TRUE.equals(req.canBeSold()) && !Boolean.TRUE.equals(req.canBePurchased())) {
            throw new BadRequestException("A product must be sellable, purchasable or both");
        }
        p.setName(req.name().trim());
        p.setProductType(req.productType());
        p.setUom(req.uom());
        p.setHsnSac(req.hsnSac() == null || req.hsnSac().isBlank() ? null : req.hsnSac().trim());
        p.setSalePrice(scale4(req.salePrice()));
        p.setPurchasePrice(scale4(req.purchasePrice()));
        p.setGstRate(req.gstRate() == null ? BigDecimal.ZERO : req.gstRate().setScale(2, RoundingMode.HALF_UP));
        p.setCanBeSold(Boolean.TRUE.equals(req.canBeSold()));
        p.setCanBePurchased(Boolean.TRUE.equals(req.canBePurchased()));
        p.setDescription(req.description());

        if (Boolean.TRUE.equals(req.canBeSold())) {
            Account income = req.incomeAccountCode() == null || req.incomeAccountCode().isBlank()
                    ? accountService.defaultFor(AccountSubType.OPERATING_REVENUE)
                    : accountService.requirePostable(req.incomeAccountCode());
            if (income.getAccountType() != AccountType.INCOME) {
                throw new BadRequestException("Income account must be an INCOME account");
            }
            p.setIncomeAccount(income);
        } else {
            p.setIncomeAccount(null);
        }
        if (Boolean.TRUE.equals(req.canBePurchased())) {
            Account expense = req.expenseAccountCode() == null || req.expenseAccountCode().isBlank()
                    ? accountService.defaultFor(req.productType() == ProductType.GOODS
                    ? AccountSubType.FIXED_ASSET : AccountSubType.OPERATING_EXPENSE)
                    : accountService.requirePostable(req.expenseAccountCode());
            if (expense.getAccountType() != AccountType.EXPENSE && expense.getAccountType() != AccountType.ASSET) {
                throw new BadRequestException("Expense account must be an EXPENSE or ASSET account");
            }
            p.setExpenseAccount(expense);
        } else {
            p.setExpenseAccount(null);
        }
    }

    private static BigDecimal scale4(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(4) : v.setScale(4, RoundingMode.HALF_UP);
    }

    private Product find(Long id) {
        return productRepository.findWithAccountsById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
