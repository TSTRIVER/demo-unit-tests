package com.atlas.aggregate.cms.core.service;

import static com.atlas.aggregate.cms.api.proto.AccountStatus.ACCOUNT_INITIATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.atlas.aggregate.cms.api.CMSStatusCodes;
import com.atlas.aggregate.cms.api.proto.LoanAccountBalanceDTO;
import com.atlas.aggregate.cms.api.proto.LoanAccountRequestDTO;
import com.atlas.aggregate.cms.api.proto.LoanAccountResponseDTO;
import com.atlas.aggregate.cms.core.constants.CommonConstants;
import com.atlas.aggregate.cms.core.facade.common.AccountLedgerFacade;
import com.atlas.aggregate.cms.core.facade.loanaccount.LoanAccountFacade;
import com.atlas.aggregate.cms.core.helper.api.LedgerAPIHelper;
import com.atlas.aggregate.cms.core.processor.loanaccount.LoanAccountProcessor;
import com.atlas.aggregate.cms.core.service.common.CustomerProfileService;
import com.atlas.aggregate.cms.core.service.common.ProductService;
import com.atlas.aggregate.cms.core.service.loanaccount.impl.LoanAccountServiceImpl;
import com.atlas.aggregate.cms.proto.entity.AccountLedgerEntity;
import com.atlas.aggregate.cms.proto.entity.AccountLedgerType;
import com.atlas.aggregate.cms.proto.entity.CustomerProfileEntity;
import com.atlas.aggregate.cms.proto.entity.LoanAccountEntity;
import com.atlas.aggregate.cms.proto.entity.ParamName;
import com.atlas.aggregate.cms.proto.record.LedgerRecord;
import com.atlas.aggregate.common.proto.ParamDTO;
import com.atlas.aggregate.common.proto.ProductDTO;
import com.atlas.aggregate.common.proto.ProductType;
import com.atlas.aggregate.common.proto.enums.AGOrigin;
import com.atlas.utility.exception.BadRequestException;

import com.common.utility.proto.enums.Currency;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LoanAccountServiceImplTests {

    private AutoCloseable closeable;

    @Mock
    private ProductService productService;

    @Mock
    private LoanAccountFacade loanAccountFacade;

    @Mock
    private AccountLedgerFacade accountLedgerFacade;

    @Mock
    private CustomerProfileService customerProfileService;

    @Mock
    private LoanAccountProcessor loanAccountProcessor;

    @Mock
    private LedgerAPIHelper ledgerAPIHelper;

    @InjectMocks
    private LoanAccountServiceImpl loanAccountService;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    private final String customerProfileId = "TEST_CUSTOMER_PROFILE_ID";
    private final String loanAccountId = "TEST_LOAN_ACCOUNT_ID";
    private final String accountId = "TEST_ACCOUNT_ID";
    private final AGOrigin origin = AGOrigin.CUSTOMER;
    private final String ledgerId = "TEST_LEDGER_ID";

    @Test
    public void testCreateLoanAccount_NoIdempotencySuccess() {

        final String productId = "TEST_PRODUCT_ID";
        final String idempotencyKey = "TEST_IDEMPOTENT_KEY";
        final String endCustomerProfileId = "TEST_END_CUSTOMER_PROFILE_ID";
        LoanAccountRequestDTO loanAccountRequestDTO = LoanAccountRequestDTO.newBuilder()
            .setProductId(productId)
            .setEndCustomerProfileId(endCustomerProfileId)
            .setAccountParam(
                LoanAccountRequestDTO.AccountParam.newBuilder().setApprovedAmount(99999).setTenure(24).setInterestRate(12.5).build())
            .build();
        CustomerProfileEntity customerProfileEntity = CustomerProfileEntity.newBuilder().build();
        ProductDTO productDTO = ProductDTO.newBuilder().setProductType(ProductType.LOAN_ACCOUNT).build();
        LoanAccountEntity loanAccountEntity = LoanAccountEntity.newBuilder()
            .setLoanAccountId(loanAccountId)
            .build();

        ParamDTO.Value.StringList stringList = ParamDTO.Value.StringList.newBuilder().setValue("IND").build();
        ParamDTO.Value.StringList stringList2 = ParamDTO.Value.StringList.newBuilder().setValue("INR").build();
        ParamDTO.Value value2 = ParamDTO.Value.newBuilder().setStringListValue(stringList).build();
        ParamDTO.Value value3 = ParamDTO.Value.newBuilder().setStringListValue(stringList2).build();

        ParamDTO.Value.RangeIntegers rangeIntegers = ParamDTO.Value.RangeIntegers.newBuilder().setMaxValue(99999).setMinValue(1).build();
        ParamDTO.Value paramDTOValue = ParamDTO.Value.newBuilder().setRangeIntegersValue(rangeIntegers).build();

        ParamDTO.Value.RangeDoubles rangeDoubles = ParamDTO.Value.RangeDoubles.newBuilder().setMaxValue(99999).setMinValue(1.5).build();
        ParamDTO.Value paramDTOValueDouble = ParamDTO.Value.newBuilder().setRangeDoublesValue(rangeDoubles).build();

        when(loanAccountFacade.getAccountByIdempotencyKey(customerProfileId, idempotencyKey)).thenReturn(Optional.empty());
        when(customerProfileService.validateOnboardAndGetCustomerProfile(customerProfileId)).thenReturn(customerProfileEntity);
        when(productService.validateProductActivated(customerProfileId, loanAccountRequestDTO.getProductId())).thenReturn(productDTO);
        when(productService.getProductParam(customerProfileId, productId, ParamName.LOAN_AMOUNT_RANGE)).thenReturn(paramDTOValue);
        when(productService.getProductParam(customerProfileId, productId, ParamName.LOAN_TENURE_RANGE)).thenReturn(paramDTOValue);
        when(productService.getProductParam(customerProfileId, productId, ParamName.INTEREST_RATE_RANGE)).thenReturn(paramDTOValueDouble);
        when(productService.getProductParam(customerProfileId, productId, ParamName.COUNTRY)).thenReturn(value2);
        when(productService.getProductParam(customerProfileId, productId, ParamName.CURRENCY)).thenReturn(value3);
        when(productService.getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.COUNTRY)).thenReturn(value2);
        when(productService.getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.CURRENCY)).thenReturn(value3);

        when(productService.getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.LOAN_AMOUNT_RANGE)).thenReturn(
            paramDTOValue);
        when(productService.getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.LOAN_TENURE_RANGE)).thenReturn(
            paramDTOValue);
        when(productService.getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.INTEREST_RATE_RANGE)).thenReturn(
            paramDTOValueDouble);
        when(loanAccountFacade.createLoanAccount(any(LoanAccountEntity.class), eq(CommonConstants.LOAN_ACCOUNT_PREFIX))).thenReturn(
            loanAccountEntity);
        when(loanAccountProcessor.process(loanAccountId)).thenReturn(true);

        //ACT
        LoanAccountResponseDTO response = loanAccountService.createLoanAccount(customerProfileId, loanAccountRequestDTO, idempotencyKey, origin);

        //ASSERT
        assertNotNull(response);
        assertEquals(loanAccountId, response.getAccountId());
        assertEquals(ACCOUNT_INITIATED.toString(), response.getAccountStatus().toString());

        verify(loanAccountFacade, times(1)).getAccountByIdempotencyKey(customerProfileId, idempotencyKey);
        verify(customerProfileService, times(1)).validateOnboardAndGetCustomerProfile(customerProfileId);
        verify(productService, times(1)).validateProductActivated(customerProfileId, loanAccountRequestDTO.getProductId());
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.LOAN_AMOUNT_RANGE);
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.LOAN_TENURE_RANGE);
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.INTEREST_RATE_RANGE);
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.COUNTRY);
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.CURRENCY);
        verify(loanAccountFacade, times(1)).createLoanAccount(any(LoanAccountEntity.class), eq(CommonConstants.LOAN_ACCOUNT_PREFIX));
        verify(loanAccountProcessor, times(1)).process(loanAccountId);
    }

    @Test
    public void testCreateLoanAccount_NoIdempotencyInterestRateOutOfRange() {

        final String productId = "TEST_PRODUCT_ID";
        final String idempotencyKey = "TEST_IDEMPOTENT_KEY";
        final String endCustomerProfileId = "TEST_END_CUSTOMER_PROFILE_ID";
        LoanAccountRequestDTO loanAccountRequestDTO = LoanAccountRequestDTO.newBuilder()
            .setProductId(productId)
            .setEndCustomerProfileId(endCustomerProfileId)
            .setAccountParam(
                LoanAccountRequestDTO.AccountParam.newBuilder().setApprovedAmount(99999).setTenure(24).setInterestRate(0).build())
            .build();
        CustomerProfileEntity customerProfileEntity = CustomerProfileEntity.newBuilder().build();
        ProductDTO productDTO = ProductDTO.newBuilder().setProductType(ProductType.LOAN_ACCOUNT).build();

        ParamDTO.Value.RangeIntegers rangeIntegers = ParamDTO.Value.RangeIntegers.newBuilder().setMaxValue(99999).setMinValue(1).build();
        ParamDTO.Value paramDTOValue = ParamDTO.Value.newBuilder().setRangeIntegersValue(rangeIntegers).build();

        ParamDTO.Value.RangeDoubles rangeDoubles = ParamDTO.Value.RangeDoubles.newBuilder().setMaxValue(99999).setMinValue(1.5).build();
        ParamDTO.Value paramDTOValueDouble = ParamDTO.Value.newBuilder().setRangeDoublesValue(rangeDoubles).build();

        when(loanAccountFacade.getAccountByIdempotencyKey(customerProfileId, idempotencyKey)).thenReturn(Optional.empty());
        when(customerProfileService.validateOnboardAndGetCustomerProfile(customerProfileId)).thenReturn(customerProfileEntity);
        when(productService.validateProductActivated(customerProfileId, loanAccountRequestDTO.getProductId())).thenReturn(productDTO);
        when(productService.getProductParam(customerProfileId, productId, ParamName.LOAN_AMOUNT_RANGE)).thenReturn(paramDTOValue);
        when(productService.getProductParam(customerProfileId, productId, ParamName.LOAN_TENURE_RANGE)).thenReturn(paramDTOValue);
        when(productService.getProductParam(customerProfileId, productId, ParamName.INTEREST_RATE_RANGE)).thenReturn(paramDTOValueDouble);

        //ACT
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> loanAccountService.createLoanAccount(customerProfileId, loanAccountRequestDTO, idempotencyKey, origin));

        //ASSERT
        assertNotNull(exception);
        assertEquals("Loan Interest Rate is out of the allowed range for the product", exception.getMessage());

        verify(loanAccountFacade, times(1)).getAccountByIdempotencyKey(customerProfileId, idempotencyKey);
        verify(customerProfileService, times(1)).validateOnboardAndGetCustomerProfile(customerProfileId);
        verify(productService, times(1)).validateProductActivated(customerProfileId, loanAccountRequestDTO.getProductId());
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.LOAN_AMOUNT_RANGE);
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.LOAN_TENURE_RANGE);
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.INTEREST_RATE_RANGE);
    }

    @Test
    public void testCreateLoanAccount_NoIdempotencyLoanTenureOutOfRange() {

        final String productId = "TEST_PRODUCT_ID";
        final String idempotencyKey = "TEST_IDEMPOTENT_KEY";
        final String endCustomerProfileId = "TEST_END_CUSTOMER_PROFILE_ID";
        LoanAccountRequestDTO loanAccountRequestDTO = LoanAccountRequestDTO.newBuilder()
            .setProductId(productId)
            .setEndCustomerProfileId(endCustomerProfileId)
            .setAccountParam(
                LoanAccountRequestDTO.AccountParam.newBuilder().setApprovedAmount(99999).setTenure(0).setInterestRate(0).build())
            .build();
        CustomerProfileEntity customerProfileEntity = CustomerProfileEntity.newBuilder().build();
        ProductDTO productDTO = ProductDTO.newBuilder().setProductType(ProductType.LOAN_ACCOUNT).build();
        ParamDTO.Value.RangeIntegers rangeIntegers = ParamDTO.Value.RangeIntegers.newBuilder().setMaxValue(99999).setMinValue(1).build();
        ParamDTO.Value paramDTOValue = ParamDTO.Value.newBuilder().setRangeIntegersValue(rangeIntegers).build();

        when(loanAccountFacade.getAccountByIdempotencyKey(customerProfileId, idempotencyKey)).thenReturn(Optional.empty());
        when(customerProfileService.validateOnboardAndGetCustomerProfile(customerProfileId)).thenReturn(customerProfileEntity);
        when(productService.validateProductActivated(customerProfileId, loanAccountRequestDTO.getProductId())).thenReturn(productDTO);
        when(productService.getProductParam(customerProfileId, productId, ParamName.LOAN_AMOUNT_RANGE)).thenReturn(paramDTOValue);
        when(productService.getProductParam(customerProfileId, productId, ParamName.LOAN_TENURE_RANGE)).thenReturn(paramDTOValue);

        //ACT
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> loanAccountService.createLoanAccount(customerProfileId, loanAccountRequestDTO, idempotencyKey, origin));

        //ASSERT
        assertNotNull(exception);
        assertEquals("Loan Tenure is out of the allowed range for the product", exception.getMessage());

        verify(loanAccountFacade, times(1)).getAccountByIdempotencyKey(customerProfileId, idempotencyKey);
        verify(customerProfileService, times(1)).validateOnboardAndGetCustomerProfile(customerProfileId);
        verify(productService, times(1)).validateProductActivated(customerProfileId, loanAccountRequestDTO.getProductId());
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.LOAN_AMOUNT_RANGE);
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.LOAN_TENURE_RANGE);
    }

    @Test
    public void testCreateLoanAccount_NoIdempotencyLoanAmountOutOfRange() {

        final String productId = "TEST_PRODUCT_ID";
        final String idempotencyKey = "TEST_IDEMPOTENT_KEY";
        final String endCustomerProfileId = "TEST_END_CUSTOMER_PROFILE_ID";
        LoanAccountRequestDTO loanAccountRequestDTO = LoanAccountRequestDTO.newBuilder()
            .setProductId(productId)
            .setEndCustomerProfileId(endCustomerProfileId)
            .setAccountParam(
                LoanAccountRequestDTO.AccountParam.newBuilder().setApprovedAmount(0).setTenure(0).setInterestRate(0).build())
            .build();
        CustomerProfileEntity customerProfileEntity = CustomerProfileEntity.newBuilder().build();
        ProductDTO productDTO = ProductDTO.newBuilder().setProductType(ProductType.LOAN_ACCOUNT).build();

        ParamDTO.Value.RangeIntegers rangeIntegers = ParamDTO.Value.RangeIntegers.newBuilder().setMaxValue(99999).setMinValue(1).build();
        ParamDTO.Value paramDTOValue = ParamDTO.Value.newBuilder().setRangeIntegersValue(rangeIntegers).build();

        when(loanAccountFacade.getAccountByIdempotencyKey(customerProfileId, idempotencyKey)).thenReturn(Optional.empty());
        when(customerProfileService.validateOnboardAndGetCustomerProfile(customerProfileId)).thenReturn(customerProfileEntity);
        when(productService.validateProductActivated(customerProfileId, loanAccountRequestDTO.getProductId())).thenReturn(productDTO);
        when(productService.getProductParam(customerProfileId, productId, ParamName.LOAN_AMOUNT_RANGE)).thenReturn(paramDTOValue);

        //ACT
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> loanAccountService.createLoanAccount(customerProfileId, loanAccountRequestDTO, idempotencyKey, origin));

        //ASSERT
        assertNotNull(exception);
        assertEquals(CMSStatusCodes.PRODUCT_APPROVED_LIMIT_OUT_OF_RANGE, exception.getErrorStatusCode());

        verify(loanAccountFacade, times(1)).getAccountByIdempotencyKey(customerProfileId, idempotencyKey);
        verify(customerProfileService, times(1)).validateOnboardAndGetCustomerProfile(customerProfileId);
        verify(productService, times(1)).validateProductActivated(customerProfileId, loanAccountRequestDTO.getProductId());
        verify(productService, times(1)).getProductParam(customerProfileId, loanAccountRequestDTO.getProductId(), ParamName.LOAN_AMOUNT_RANGE);
    }

    @Test
    public void testCreateLoanAccount_InvalidProduct() {

        final String productId = "TEST_PRODUCT_ID";
        final String idempotencyKey = "TEST_IDEMPOTENT_KEY";
        final String endCustomerProfileId = "TEST_END_CUSTOMER_PROFILE_ID";
        LoanAccountRequestDTO loanAccountRequestDTO = LoanAccountRequestDTO.newBuilder()
            .setProductId(productId)
            .setEndCustomerProfileId(endCustomerProfileId)
            .setAccountParam(
                LoanAccountRequestDTO.AccountParam.newBuilder().setApprovedAmount(0).setTenure(0).setInterestRate(0).build())
            .build();
        CustomerProfileEntity customerProfileEntity = CustomerProfileEntity.newBuilder().build();
        ProductDTO productDTO = ProductDTO.newBuilder().setProductType(ProductType.CARD_ACCOUNT).build();

        when(loanAccountFacade.getAccountByIdempotencyKey(customerProfileId, idempotencyKey)).thenReturn(Optional.empty());
        when(customerProfileService.validateOnboardAndGetCustomerProfile(customerProfileId)).thenReturn(customerProfileEntity);
        when(productService.validateProductActivated(customerProfileId, loanAccountRequestDTO.getProductId())).thenReturn(productDTO);

        //ACT
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> loanAccountService.createLoanAccount(customerProfileId, loanAccountRequestDTO, idempotencyKey, origin));

        //ASSERT
        assertNotNull(exception);
        assertEquals(CMSStatusCodes.INVALID_PRODUCT_TYPE, exception.getErrorStatusCode());

        verify(loanAccountFacade, times(1)).getAccountByIdempotencyKey(customerProfileId, idempotencyKey);
        verify(customerProfileService, times(1)).validateOnboardAndGetCustomerProfile(customerProfileId);
        verify(productService, times(1)).validateProductActivated(customerProfileId, loanAccountRequestDTO.getProductId());
    }

    @Test
    public void testCreateLoanAccount_IdempotencySuccess() {

        final String productId = "TEST_PRODUCT_ID";
        final String idempotencyKey = "TEST_IDEMPOTENT_KEY";
        final String endCustomerProfileId = "TEST_END_CUSTOMER_PROFILE_ID";
        LoanAccountRequestDTO loanAccountRequestDTO = LoanAccountRequestDTO.newBuilder()
            .setProductId(productId)
            .setEndCustomerProfileId(endCustomerProfileId)
            .setAccountParam(
                LoanAccountRequestDTO.AccountParam.newBuilder().setApprovedAmount(0).setTenure(0).setInterestRate(0).build())
            .build();
        LoanAccountEntity loanAccountEntity = LoanAccountEntity.newBuilder().build();

        when(loanAccountFacade.getAccountByIdempotencyKey(customerProfileId, idempotencyKey)).thenReturn(Optional.of(loanAccountEntity));

        //ACT
        LoanAccountResponseDTO response = loanAccountService.createLoanAccount(customerProfileId, loanAccountRequestDTO, idempotencyKey, origin);

        //ASSERT
        assertNotNull(response);
        verify(loanAccountFacade, times(1)).getAccountByIdempotencyKey(customerProfileId, idempotencyKey);
    }

    @Test
    public void testGetAccountBalance_Success() {

        LoanAccountEntity loanAccountEntity = LoanAccountEntity.newBuilder()
            .setCustomerProfileId(customerProfileId)
            .setLoanAccountId(loanAccountId)
            .setCurrency(Currency.PKR)
            .build();
        AccountLedgerEntity accountLedgerEntity1 = AccountLedgerEntity.newBuilder()
            .setAccountId(accountId)
            .setLedgerType(AccountLedgerType.LA_AVAILABLE.toString())
            .setLedgerId(ledgerId)
            .build();
        AccountLedgerEntity accountLedgerEntity2 = AccountLedgerEntity.newBuilder()
            .setAccountId(accountId)
            .setLedgerType(AccountLedgerType.LA_RECOVERED.toString())
            .setLedgerId("TEST_LEDGER_ID_2")
            .build();
        AccountLedgerEntity accountLedgerEntity3 = AccountLedgerEntity.newBuilder()
            .setAccountId(accountId)
            .setLedgerType(AccountLedgerType.CA_AVAILABLE.toString())
            .setLedgerId("TEST_LEDGER_ID_3")
            .build();
        ImmutableList<AccountLedgerEntity> accountLedgerEntities = ImmutableList.of(accountLedgerEntity1, accountLedgerEntity2, accountLedgerEntity3);
        List<AccountLedgerEntity> productLedgers = new ArrayList<>(accountLedgerEntities);
        LedgerRecord ledgerRecord1 = LedgerRecord.newBuilder()
            .setLedgerId(ledgerId)
            .setAvailableBalance(5000)
            .build();
        Map<String, String> mp = new HashMap<>();
        mp.put("ledgerType", "LA_RECOVERED");
        LedgerRecord ledgerRecord2 = LedgerRecord.newBuilder()
            .setLedgerId("TEST_LEDGER_ID_2")
            .setAvailableBalance(6000)
            .putAllMetadata(mp)
            .build();
        LedgerRecord ledgerRecord3 = LedgerRecord.newBuilder()
            .setLedgerId("TEST_LEDGER_ID_3")
            .setAvailableBalance(7000)
            .build();
        ImmutableList<LedgerRecord> ledgerRecords = ImmutableList.of(ledgerRecord1, ledgerRecord2, ledgerRecord3);

        when(loanAccountFacade.getLoanAccountByAccountId(accountId)).thenReturn(Optional.of(loanAccountEntity));
        when(accountLedgerFacade.listAccountLedgersByAccountId(accountId)).thenReturn(accountLedgerEntities);
        when(ledgerAPIHelper.getLedgers(customerProfileId, productLedgers.stream().map(AccountLedgerEntity::getLedgerId).toList())).thenReturn(
            ledgerRecords);

        //ACT
        LoanAccountBalanceDTO response = loanAccountService.getAccountBalance(customerProfileId, accountId, origin);

        //ASSERT
        assertNotNull(response);
        assertEquals(loanAccountId, response.getLoanAccountId());
        assertEquals(5000, response.getAvailableBalance());
        assertEquals(6000, response.getRecoveredBalance());
        assertEquals(7000, response.getLentBalance());

        verify(loanAccountFacade, times(1)).getLoanAccountByAccountId(accountId);
        verify(accountLedgerFacade, times(1)).listAccountLedgersByAccountId(accountId);
        verify(ledgerAPIHelper, times(1)).getLedgers(customerProfileId, productLedgers.stream().map(AccountLedgerEntity::getLedgerId).toList());

    }

    @Test
    public void testGetAccountBalance_LoanAccountDetailsNotFound() {

        when(loanAccountFacade.getLoanAccountByAccountId(accountId)).thenReturn(Optional.empty());

        //ACT
        BadRequestException exception =
            assertThrows(BadRequestException.class, () -> loanAccountService.getAccountBalance(customerProfileId, accountId, origin));

        //ASSERT
        assertNotNull(exception);
        assertEquals(CMSStatusCodes.LOAN_ACCOUNT_NOT_FOUND, exception.getErrorStatusCode());

        verify(loanAccountFacade, times(1)).getLoanAccountByAccountId(accountId);

    }

    @Test
    public void testGetAccountBalance_CustomerProfileIdMismatch() {

        LoanAccountEntity loanAccountEntity = LoanAccountEntity.newBuilder()
            .setCustomerProfileId("fakeCustomerProfileId")
            .setLoanAccountId(loanAccountId)
            .setCurrency(Currency.PKR)
            .build();

        when(loanAccountFacade.getLoanAccountByAccountId(accountId)).thenReturn(Optional.of(loanAccountEntity));

        //ACT
        BadRequestException exception =
            assertThrows(BadRequestException.class, () -> loanAccountService.getAccountBalance(customerProfileId, accountId, origin));

        //ASSERT
        assertNotNull(exception);
        assertEquals(CMSStatusCodes.LOAN_ACCOUNT_NOT_FOUND, exception.getErrorStatusCode());

        verify(loanAccountFacade, times(1)).getLoanAccountByAccountId(accountId);

    }

    @Test
    public void testGetAccountBalance_AvailableLedgerNotFoundInAccountLedgers() {

        LoanAccountEntity loanAccountEntity = LoanAccountEntity.newBuilder()
            .setCustomerProfileId(customerProfileId)
            .setLoanAccountId(loanAccountId)
            .setCurrency(Currency.PKR)
            .build();
        AccountLedgerEntity accountLedgerEntity = AccountLedgerEntity.newBuilder()
            .setAccountId(accountId)
            .setLedgerType(AccountLedgerType.CA_AVAILABLE.toString())
            .setLedgerId(ledgerId)
            .build();
        ImmutableList<AccountLedgerEntity> accountLedgerEntities = ImmutableList.of(accountLedgerEntity);

        when(loanAccountFacade.getLoanAccountByAccountId(accountId)).thenReturn(Optional.of(loanAccountEntity));
        when(accountLedgerFacade.listAccountLedgersByAccountId(accountId)).thenReturn(accountLedgerEntities);

        //ACT
        Exception exception =
            assertThrows(Exception.class, () -> loanAccountService.getAccountBalance(customerProfileId, accountId, origin));

        //ASSERT
        assertNotNull(exception);
        verify(loanAccountFacade, times(1)).getLoanAccountByAccountId(accountId);
        verify(accountLedgerFacade, times(1)).listAccountLedgersByAccountId(accountId);
    }

    @Test
    public void testGetAccountBalance_AvailableLedgerNotFoundInLedgersList() {

        LoanAccountEntity loanAccountEntity = LoanAccountEntity.newBuilder()
            .setCustomerProfileId(customerProfileId)
            .setLoanAccountId(loanAccountId)
            .setCurrency(Currency.PKR)
            .build();
        AccountLedgerEntity accountLedgerEntity = AccountLedgerEntity.newBuilder()
            .setAccountId(accountId)
            .setLedgerType(AccountLedgerType.LA_AVAILABLE.toString())
            .setLedgerId(ledgerId)
            .build();
        ImmutableList<AccountLedgerEntity> accountLedgerEntities = ImmutableList.of(accountLedgerEntity);
        List<AccountLedgerEntity> productLedgers = new ArrayList<>(accountLedgerEntities);
        LedgerRecord ledgerRecord = LedgerRecord.newBuilder()
            .setLedgerId("TEST_LEDGER_ID_2")
            .build();
        ImmutableList<LedgerRecord> ledgerRecords = ImmutableList.of(ledgerRecord);

        when(loanAccountFacade.getLoanAccountByAccountId(accountId)).thenReturn(Optional.of(loanAccountEntity));
        when(accountLedgerFacade.listAccountLedgersByAccountId(accountId)).thenReturn(accountLedgerEntities);
        when(ledgerAPIHelper.getLedgers(customerProfileId, productLedgers.stream().map(AccountLedgerEntity::getLedgerId).toList())).thenReturn(
            ledgerRecords);
        //ACT
        Exception exception =
            assertThrows(Exception.class, () -> loanAccountService.getAccountBalance(customerProfileId, accountId, origin));

        //ASSERT
        assertNotNull(exception);
        verify(loanAccountFacade, times(1)).getLoanAccountByAccountId(accountId);
        verify(accountLedgerFacade, times(1)).listAccountLedgersByAccountId(accountId);
        verify(ledgerAPIHelper, times(1)).getLedgers(customerProfileId, productLedgers.stream().map(AccountLedgerEntity::getLedgerId).toList());
    }

    @Test
    public void testGetAccountBalance_RecoverLedgerNotFoundInLedgersList() {

        LoanAccountEntity loanAccountEntity = LoanAccountEntity.newBuilder()
            .setCustomerProfileId(customerProfileId)
            .setLoanAccountId(loanAccountId)
            .setCurrency(Currency.PKR)
            .build();
        AccountLedgerEntity accountLedgerEntity = AccountLedgerEntity.newBuilder()
            .setAccountId(accountId)
            .setLedgerType(AccountLedgerType.LA_AVAILABLE.toString())
            .setLedgerId(ledgerId)
            .build();
        ImmutableList<AccountLedgerEntity> accountLedgerEntities = ImmutableList.of(accountLedgerEntity);
        List<AccountLedgerEntity> productLedgers = new ArrayList<>(accountLedgerEntities);
        LedgerRecord ledgerRecord = LedgerRecord.newBuilder()
            .setLedgerId(ledgerId)
            .setAvailableBalance(5000)
            .build();
        ImmutableList<LedgerRecord> ledgerRecords = ImmutableList.of(ledgerRecord);

        when(loanAccountFacade.getLoanAccountByAccountId(accountId)).thenReturn(Optional.of(loanAccountEntity));
        when(accountLedgerFacade.listAccountLedgersByAccountId(accountId)).thenReturn(accountLedgerEntities);
        when(ledgerAPIHelper.getLedgers(customerProfileId, productLedgers.stream().map(AccountLedgerEntity::getLedgerId).toList())).thenReturn(
            ledgerRecords);
        //ACT
        Exception exception =
            assertThrows(Exception.class, () -> loanAccountService.getAccountBalance(customerProfileId, accountId, origin));

        //ASSERT
        assertNotNull(exception);
        verify(loanAccountFacade, times(1)).getLoanAccountByAccountId(accountId);
        verify(accountLedgerFacade, times(1)).listAccountLedgersByAccountId(accountId);
        verify(ledgerAPIHelper, times(1)).getLedgers(customerProfileId, productLedgers.stream().map(AccountLedgerEntity::getLedgerId).toList());
    }

}
