package com.atlas.aggregate.cms.configuration;

import java.util.List;
import java.util.concurrent.Executor;

import com.atlas.aggregate.card.api.proto.AttachCardResponseDTO;
import com.atlas.aggregate.card.api.proto.CustomerProfileResponseDTO;
import com.atlas.aggregate.card.api.proto.DetachCardResponseDTO;
import com.atlas.aggregate.card.api.proto.FloatAccountResponseDTO;
import com.atlas.aggregate.card.api.proto.RefundAccountResponseDTO;
import com.atlas.aggregate.card.api.proto.VerifyCardAttachableResponseDTO;
import com.atlas.aggregate.cms.api.proto.BalanceResponseDTO;
import com.atlas.aggregate.cms.api.proto.BalanceResponseListDTO;
import com.atlas.aggregate.cms.api.proto.BillDTO;
import com.atlas.aggregate.cms.api.proto.CreditAccountResponseDTO;
import com.atlas.aggregate.cms.api.proto.CustomerPaymentAccountResponseDTO;
import com.atlas.aggregate.cms.api.proto.CustomerProfileDTO;
import com.atlas.aggregate.cms.api.proto.LedgerBalanceResponseDTO;
import com.atlas.aggregate.cms.api.proto.LoanAccountResponseDTO;
import com.atlas.aggregate.cms.api.proto.LoanDisbursementResponseDTO;
import com.atlas.aggregate.cms.api.proto.OnboardEndCustomerProfileResponseDTO;
import com.atlas.aggregate.cms.api.proto.SendResponseDTO;
import com.atlas.aggregate.cms.api.proto.TransactionRequestDTO;
import com.atlas.aggregate.cms.api.proto.TransactionResponseDTO;
import com.atlas.aggregate.cms.api.proto.WithdrawResponseDTO;
import com.atlas.aggregate.cms.core.constants.ResourceConstants;
import com.atlas.aggregate.common.proto.FeeDTO;
import com.atlas.aggregate.common.proto.ProductConfigsDTO;
import com.atlas.aggregate.common.proto.TaxDTO;
import com.atlas.aggregate.common.proto.TrackerSupportedTypesDTO;
import com.atlas.aggregate.common.proto.TransactionCodeConfigDTO;
import com.atlas.aggregate.common.proto.TransactionLimitDTO;
import com.atlas.aggregate.common.proto.TransactionLimitResponseDTO;
import com.atlas.aggregate.compliance.api.ComplianceAPI;
import com.atlas.aggregate.core.api.TrackerAPI;
import com.atlas.aggregate.core.api.TransactionCodeAPI;
import com.atlas.aggregate.ledger.api.LedgerAPI;
import com.atlas.aggregate.ledger.api.LedgerInternalAPI;
import com.atlas.aggregate.notification.api.NotificationAPI;
import com.atlas.aggregate.payment.api.AccountAPI;
import com.atlas.aggregate.payment.api.CustomerProfileAPI;
import com.atlas.aggregate.payment.api.DevAPI;
import com.atlas.aggregate.payment.api.InternalAPI;
import com.atlas.aggregate.payment.api.ProviderAPI;
import com.atlas.aggregate.payment.api.TransactionAPI;
import com.atlas.aggregate.profile.api.ProfileAPI;
import com.atlas.utility.cache.CacheUtility;
import com.atlas.utility.configuration.common.resolver.PaginationArgumentResolver;
import com.atlas.utility.constants.ConfigConstants;
import com.atlas.utility.datasource.TransactionProvider;
import com.atlas.utility.datasource.mysql.MysqlProvider;
import com.atlas.utility.exception.handler.GlobalExceptionHandler;
import com.atlas.utility.misc.TransactionManager;
import com.atlas.utility.mq.MessageQueueProvider;
import com.atlas.utility.protobuf.ProtoMessageConverter;
import com.atlas.utility.storage.StorageProvider;

import com.common.utility.proto.ApiResponse;
import com.common.utility.proto.rule.ConditionConfigsDTO;
import com.google.protobuf.util.JsonFormat;
import org.aspectj.lang.Aspects;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@TestConfiguration
@Profile({ConfigConstants.PROFILE_TEST})
@Import(GlobalExceptionHandler.class)
public class CMSTestConfiguration implements WebMvcConfigurer {
    @Bean
    ProtoMessageConverter getProtoMessageConverter() {
        JsonFormat.TypeRegistry typeRegistry = JsonFormat.TypeRegistry.newBuilder()
            .add(ApiResponse.getDescriptor())
            .add(CustomerProfileDTO.getDescriptor())
            .add(OnboardEndCustomerProfileResponseDTO.getDescriptor())
            .add(CreditAccountResponseDTO.getDescriptor())
            .add(TransactionResponseDTO.getDescriptor())
            .add(BalanceResponseDTO.getDescriptor())
            .add(BalanceResponseListDTO.getDescriptor())
            .add(ProductConfigsDTO.getDescriptor())
            .add(SendResponseDTO.getDescriptor())
            .add(WithdrawResponseDTO.getDescriptor())
            .add(CustomerPaymentAccountResponseDTO.getDescriptor())
            .add(CustomerProfileResponseDTO.getDescriptor())
            .add(FloatAccountResponseDTO.getDescriptor())
            .add(RefundAccountResponseDTO.getDescriptor())
            .add(AttachCardResponseDTO.getDescriptor())
            .add(VerifyCardAttachableResponseDTO.getDescriptor())
            .add(DetachCardResponseDTO.getDescriptor())
            .add(LedgerBalanceResponseDTO.getDescriptor())
            .add(TransactionRequestDTO.getDescriptor())
            .add(BillDTO.getDescriptor())
            .add(FeeDTO.getDescriptor())
            .add(TaxDTO.getDescriptor())
            .add(TransactionCodeConfigDTO.getDescriptor())
            .add(ConditionConfigsDTO.getDescriptor())
            .add(TransactionLimitResponseDTO.getDescriptor())
            .add(TransactionLimitDTO.getDescriptor())
            .add(TrackerSupportedTypesDTO.getDescriptor())
            .add(LoanAccountResponseDTO.getDescriptor())
            .add(LoanDisbursementResponseDTO.getDescriptor())
            .build();

        return new ProtoMessageConverter(typeRegistry);
    }

    @Bean
    @Primary
    ProtobufHttpMessageConverter getProtobufHttpMessageConverter(ProtoMessageConverter protoMessageConverter) {
        return protoMessageConverter.getHttpConverter(true);
    }

    @Bean(ResourceConstants.BEAN_MYSQL_PROVIDER)
    @DependsOn(ResourceConstants.BEAN_TRANSACTION_MANAGER)
    MysqlProvider getMysqlProvider() {
        return Mockito.mock(MysqlProvider.class);
    }

    @Bean(ResourceConstants.BEAN_TRANSACTION_MANAGER)
    TransactionManager getTransactionManager() {
        return Aspects.aspectOf(TransactionManager.class);
    }

    @Bean(ResourceConstants.BEAN_MYSQL_TRANSACTION_PROVIDER)
    TransactionProvider getTransactionProvider() {
        return Mockito.mock(TransactionProvider.class);
    }

    @Bean(ResourceConstants.QUEUE_PROCESS_QUEUE_POLL_INTERVAL)
    String getProcessQueuePollInterval() {
        return "";
    }

    @Bean(ResourceConstants.QUEUE_PROCESS_QUEUE_EXECUTOR)
    Executor getProcessQueueExecutor() {
        return Mockito.mock(Executor.class);
    }

    @Bean(ResourceConstants.QUEUE_PROCESS_QUEUE_PROVIDER)
    MessageQueueProvider getProcessQueueProvider() {
        return Mockito.mock(MessageQueueProvider.class);
    }

    @Bean(ResourceConstants.QUEUE_NOTIFICATION_QUEUE_POLL_INTERVAL)
    String processQueuePollInterval() {
        return "";
    }

    @Bean(ResourceConstants.QUEUE_NOTIFICATION_QUEUE_EXECUTOR)
    Executor getNotificationQueueExecutor() {
        return Mockito.mock(Executor.class);
    }

    @Bean(ResourceConstants.QUEUE_NOTIFICATION_QUEUE_PROVIDER)
    MessageQueueProvider getNotificationQueueProvider() {
        return Mockito.mock(MessageQueueProvider.class);
    }

    @Bean(ResourceConstants.QUEUE_TASK_QUEUE_POLL_INTERVAL)
    String taskQueuePollInterval() {
        return "";
    }

    @Bean(ResourceConstants.QUEUE_TASK_QUEUE_EXECUTOR)
    Executor getTaskQueueExecutor() {
        return Mockito.mock(Executor.class);
    }

    @Bean(ResourceConstants.QUEUE_TASK_QUEUE_PROVIDER)
    MessageQueueProvider getTaskQueueProvider() {
        return Mockito.mock(MessageQueueProvider.class);
    }

    // Other services beans
    @Bean(ResourceConstants.API_PROFILE)
    ProfileAPI getProfileAPI() {
        return Mockito.mock(ProfileAPI.class);
    }

    @Bean(ResourceConstants.API_LEDGER)
    LedgerAPI getLedgerAPI() {
        return Mockito.mock(LedgerAPI.class);
    }

    @Bean(ResourceConstants.API_LEDGER_INTERNAL)
    LedgerInternalAPI getLedgerInternalAPI() {
        return Mockito.mock(LedgerInternalAPI.class);
    }

    @Bean(ResourceConstants.API_NOTIFICATION)
    NotificationAPI getNotificationAPI() {
        return Mockito.mock(NotificationAPI.class);
    }

    @Bean(ResourceConstants.API_COMPLIANCE)
    ComplianceAPI getComplianceAPI() {
        return Mockito.mock(ComplianceAPI.class);
    }

    @Bean(ResourceConstants.API_PAYMENT_INTERNAL)
    InternalAPI getPaymentInternalAPI() {
        return Mockito.mock(InternalAPI.class);
    }

    @Bean(ResourceConstants.API_PAYMENT_PROVIDER)
    ProviderAPI getPaymentProviderAPI() {
        return Mockito.mock(ProviderAPI.class);
    }

    @Bean(ResourceConstants.API_PAYMENT_CUSTOMER_PROFILE)
    CustomerProfileAPI getPaymentCustomerProfileAPI() {
        return Mockito.mock(CustomerProfileAPI.class);
    }

    @Bean(ResourceConstants.API_PAYMENT_ACCOUNT)
    AccountAPI getPaymentAccountAPI() {
        return Mockito.mock(AccountAPI.class);
    }

    @Bean(ResourceConstants.API_PAYMENT_TRANSACTION)
    TransactionAPI getPaymentTransactionAPI() {
        return Mockito.mock(TransactionAPI.class);
    }

    @Bean(ResourceConstants.API_PAYMENT_DEV)
    DevAPI getPaymentDevAPI() {
        return Mockito.mock(DevAPI.class);
    }

    @Bean(ResourceConstants.API_CORE_TRANSACTION_CODE)
    TransactionCodeAPI getCoreTransactionCodeAPI() {
        return Mockito.mock(TransactionCodeAPI.class);
    }

    @Bean(ResourceConstants.API_TRACKER)
    TrackerAPI getTrackerAPI() {
        return Mockito.mock(TrackerAPI.class);
    }

    @Bean(ResourceConstants.BEAN_CACHE)
    CacheUtility getCache() {
        return Mockito.mock(CacheUtility.class);
    }

    @Primary
    @Bean(ResourceConstants.CACHE_MANAGER_REDIS)
    public CacheManager redisCacheManager() {
        return Mockito.mock(RedisCacheManager.class);
    }

    @Bean(ResourceConstants.CACHE_MANAGER_IN_MEMORY)
    CacheManager getInMemoryCacheManager() {
        return Mockito.mock(CacheManager.class);
    }

    @Bean(ResourceConstants.BEAN_STORAGE_PROVIDER)
    StorageProvider getStorageProvider() {
        return Mockito.mock(StorageProvider.class);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new PaginationArgumentResolver());
    }
}
