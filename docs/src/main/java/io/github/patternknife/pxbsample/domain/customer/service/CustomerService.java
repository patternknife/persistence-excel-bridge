package io.github.patternknife.pxbsample.domain.customer.service;


import io.github.patternknife.pxbsample.domain.customer.dao.CustomerRepository;
import io.github.patternknife.pxbsample.domain.customer.dao.CustomerRepositorySupport;
import io.github.patternknife.pxbsample.domain.customer.dto.CustomerReqDTO;
import io.github.patternknife.pxbsample.domain.customer.dto.CustomerResDTO;
import io.github.patternknife.pxbsample.domain.customer.entity.Customer;
import io.github.patternknife.pxbsample.config.security.dao.CustomOauthAccessTokenRepository;
import io.github.patternknife.pxbsample.config.security.dao.CustomOauthRefreshTokenRepository;
import io.github.patternknife.pxbsample.config.security.entity.CustomOauthAccessToken;
import io.github.patternknife.pxbsample.config.security.principal.AccessTokenUserInfo;
import io.github.patternknife.pxbsample.util.CustomUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;



@Service
@RequiredArgsConstructor
public class CustomerService  {

    private final CustomerRepository customerRepository;

    private final CustomerRepositorySupport customerRepositorySupport;


    private final CustomOauthAccessTokenRepository customOauthAccessTokenRepository;
    private final CustomOauthRefreshTokenRepository customOauthRefreshTokenRepository;


    @Value("${spring.jpa.properties.hibernate.dialect}")
    String dbDialect;

    @Value("${app.oauth2.appUser.clientId}")
    private final String appUserClientId;

    /*
    *   Suspended by Admin
    * */
    @Transactional(value = "commonTransactionManager", rollbackFor=Exception.class)
    public void deleteCustomer(Long id, Long adminId){
        Customer customer = customerRepositorySupport.findById(id);

        List<CustomOauthAccessToken> customOauthAccessTokens = customOauthAccessTokenRepository.findByClientIdAndUserName(appUserClientId, customer.getIdName());

        for (CustomOauthAccessToken customOauthAccessToken : customOauthAccessTokens) {
            customOauthRefreshTokenRepository.deleteById(customOauthAccessToken.getRefreshToken());
        }

        customOauthAccessTokenRepository.deleteByUserName(customer.getIdName());

        customerRepositorySupport.deleteOne(id, adminId);
    }

    /*
    *   The member has withdrawn
    * */
    @Transactional(value = "commonTransactionManager", rollbackFor=Exception.class)
    public void deleteCustomer(AccessTokenUserInfo accessTokenUserInfo){

        Customer customer = customerRepositorySupport.findById(accessTokenUserInfo.getAdditionalAccessTokenUserInfo().getId());

        List<CustomOauthAccessToken> customOauthAccessTokens = customOauthAccessTokenRepository.findByClientIdAndUserName(appUserClientId, customer.getIdName());

        for (CustomOauthAccessToken customOauthAccessToken : customOauthAccessTokens) {
            customOauthRefreshTokenRepository.deleteById(customOauthAccessToken.getRefreshToken());
        }

        customOauthAccessTokenRepository.deleteByUserName(customer.getIdName());

        // The criteria for a member withdrawal is the presence of values in the deleted_at and deleted_ci columns. Additionally, such users are considered withdrawn members and cannot be restored by the administrator.
        customer.setDeletedAt(LocalDateTime.now());

        // The criteria for account deactivation are when there are values in deleted_at and delete_admin_id, and it can be restored by an administrator. Since this is the member withdrawal logic, always set delete_admin_id to null.
        customer.setDeleteAdmin(null);

        customer.setDeletedIdName(customer.getIdName());
        customer.setIdName(null);

    }

    @Transactional(value = "commonTransactionManager", rollbackFor=Exception.class)
    public void restoreCustomer(Long id){
        customerRepositorySupport.restoreOne(id);
    }


    @Transactional(value = "commonTransactionManager", rollbackFor=Exception.class)
    public CustomerResDTO.Id update(Long id, CustomerReqDTO.Update dto) {
        return customerRepositorySupport.updateOne(id, dto);
    }


    public boolean checkIdNameDuplicate(String idName) {
        return customerRepository.existsByIdName(idName);
    }

    public boolean checkHpDuplicate(String hp) {
        return customerRepository.existsByHp(CustomUtils.removeSpecialCharacters(hp));
    }



}